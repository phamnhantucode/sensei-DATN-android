package com.phamnhantucode.aicareercoach.data.neon

import android.util.Base64
import android.util.Log
import com.clerk.api.user.User
import com.phamnhantucode.aicareercoach.BuildConfig
import java.io.IOException
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.text.Charsets.UTF_8

/**
 * Minimal client for Neon REST SQL API used to mirror Clerk users
 * into the Postgres schema defined under prisma/schema.prisma.
 */
object NeonUserService {

    private const val TAG = "NeonUserService"
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun upsertUser(user: User, authToken: String? = null) = withContext(Dispatchers.IO) {
        val bearerToken = authToken?.takeUnless { it.isBlank() }
            ?: BuildConfig.NEON_API_KEY.takeUnless { it.isBlank() }
        val basicAuthHeader = BuildConfig.NEON_DB_ROLE.takeUnless { it.isBlank() }?.let { role ->
            val password = BuildConfig.NEON_DB_PASSWORD.takeUnless { it.isBlank() } ?: return@let null
            val credentials = "$role:$password"
            val encodedCredentials =
                Base64.encodeToString(credentials.toByteArray(UTF_8), Base64.NO_WRAP)
            "Basic $encodedCredentials"
        }

        val authorizationHeader = when {
            bearerToken != null -> "Bearer $bearerToken"
            basicAuthHeader != null -> basicAuthHeader
            else -> {
                Log.w(TAG, "No Neon auth credentials available; skipping Neon sync.")
                return@withContext
            }
        }

        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val email = resolvePrimaryEmail(user)
            ?: run {
                Log.w(TAG, "Clerk user ${user.id} missing email; skipping Neon sync.")
                return@withContext
            }

        val payload = JSONObject().apply {
            put("clerkUserId", user.id)
            put("email", email)
            put("name", resolveDisplayName(user) ?: JSONObject.NULL)
            put("imageUrl", user.imageUrl ?: JSONObject.NULL)
            put("industry", JSONObject.NULL)
            put("skills", JSONArray())
        }

        Log.d(TAG, "Neon sync payload: $payload")

        val request = Request.Builder()
            .url("$apiUrl/User?on_conflict=clerkUserId")
            .addHeader("Authorization", authorizationHeader)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .addHeader("Prefer", "return=minimal")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string()
            if (!response.isSuccessful) {
                val errorMessage = bodyString ?: "Empty response body"
                if (response.code == 409 && bodyString?.contains("duplicate key value") == true) {
                    Log.i(TAG, "Neon record exists for ${user.id}; attempting to update instead.")
                    updateExistingUser(apiUrl, authorizationHeader, user, payload)
                    return@withContext
                }
                throw IOException("Neon query failed (${response.code}): $errorMessage")
            }
            Log.d(TAG, "Synced Clerk user ${user.id} with Neon.")
        }
    }

    private fun resolvePrimaryEmail(user: User): String? {
        val primaryId = user.primaryEmailAddressId
        val primary = primaryId?.let { id ->
            user.emailAddresses.firstOrNull { it.id == id }
        }
        val fallback = primary ?: user.emailAddresses.firstOrNull()
        return fallback?.emailAddress?.takeUnless { it.isBlank() }
    }

    private fun resolveDisplayName(user: User): String? {
        val first = user.firstName?.takeUnless { it.isBlank() }
        val last = user.lastName?.takeUnless { it.isBlank() }
        val combined = listOfNotNull(first, last)
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
        return combined
            ?: user.username?.takeUnless { it.isBlank() }
            ?: resolvePrimaryEmail(user)
    }

    private fun updateExistingUser(
        apiUrl: String,
        authorizationHeader: String,
        user: User,
        payload: JSONObject,
    ) {
        val encodedClerkId = URLEncoder.encode(user.id, UTF_8.name())
        val request =
            Request.Builder()
                .url("$apiUrl/User?clerkUserId=eq.$encodedClerkId")
                .addHeader("Authorization", authorizationHeader)
                .addHeader("Content-Type", "application/json")
                .patch(payload.toString().toRequestBody(jsonMediaType))
                .build()

        client.newCall(request).execute().use { patchResponse ->
            val bodyString = patchResponse.body?.string()
            if (!patchResponse.isSuccessful) {
                val errorMessage = bodyString ?: "Empty response body"
                throw IOException(
                    "Neon update failed (${patchResponse.code}) for ${user.id}: $errorMessage"
                )
            }
            Log.d(TAG, "Updated existing Neon user ${user.id}.")
        }
    }

}
