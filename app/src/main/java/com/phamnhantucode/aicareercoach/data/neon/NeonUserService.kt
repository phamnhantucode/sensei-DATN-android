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

    /**
     * Generates a random ID matching Prisma's default: encode(gen_random_bytes(12), 'hex')
     * This creates a 24-character hex string from 12 random bytes.
     */
    private fun generateRandomId(): String {
        val randomBytes = ByteArray(12)
        java.security.SecureRandom().nextBytes(randomBytes)
        return randomBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Creates or updates a user with the industry field.
     * This should be used during onboarding after ensuring the IndustryInsight exists.
     *
     * @param user The Clerk user to sync
     * @param industry The industry name (required for new users)
     * @param authToken Optional authentication token
     */
    /**
     * Creates or updates a user with the industry field.
     * This should be used during onboarding after ensuring the IndustryInsight exists.
     *
     * @param user The Clerk user to sync
     * @param industry The industry name (required for new users)
     * @param authToken Optional authentication token
     */
    suspend fun upsertUserWithIndustry(
        user: User,
        industry: String,
        authToken: String? = null
    ) = withContext(Dispatchers.IO) {
        val authorizationHeader = resolveAuthorizationHeader(authToken)
            ?: run {
                Log.w(TAG, "[NeonUserService] No Neon auth credentials available; skipping Neon sync.")
                return@withContext
            }

        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val email = resolvePrimaryEmail(user)
            ?: run {
                Log.w(TAG, "[NeonUserService] Clerk user ${user.id} missing email; skipping Neon sync.")
                return@withContext
            }

        // First, check if user already exists
        val existingUser = getUser(user.id, authToken)

        if (existingUser != null) {
            // User exists, just update the industry
            Log.d(TAG, "[NeonUserService] User ${user.id} already exists in Neon, updating industry to $industry")
            val updatePayload = JSONObject().apply {
                put("industry", industry)
            }
            patchUser(apiUrl, authorizationHeader, user.id, updatePayload)
        } else {
            // User doesn't exist, create with industry
            // Generate ID using the same expression as in Prisma schema
            val randomId = generateRandomId()
            val now = java.time.Instant.now().toString()
            val payload = JSONObject().apply {
                put("id", randomId)
                put("clerkUserId", user.id)
                put("email", email)
                put("name", resolveDisplayName(user) ?: JSONObject.NULL)
                put("imageUrl", user.imageUrl ?: JSONObject.NULL)
                put("industry", industry)
                put("createdAt", now)
                put("updatedAt", now)
                put("skills", JSONArray())
            }

            Log.d(TAG, "[NeonUserService] Creating new Neon user with industry: $payload")

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
                        Log.i(TAG, "[NeonUserService] Neon record exists for ${user.id}; attempting to update instead.")
                        val updatePayload = JSONObject().apply {
                            put("industry", industry)
                        }
                        patchUser(apiUrl, authorizationHeader, user.id, updatePayload)
                        return@withContext
                    }
                    throw IOException("Neon query failed (${response.code}): $errorMessage")
                }
                Log.d(TAG, "[NeonUserService] Created Neon user ${user.id} with industry $industry.")
            }
        }
    }

    /**
     * Legacy method that creates/updates user without industry.
     * WARNING: This will fail if the database requires industry to be non-null.
     * Use upsertUserWithIndustry() instead for onboarding flow.
     */
    suspend fun upsertUser(user: User, authToken: String? = null) = withContext(Dispatchers.IO) {
        val authorizationHeader = resolveAuthorizationHeader(authToken)
            ?: run {
                Log.w(TAG, "[NeonUserService] No Neon auth credentials available; skipping Neon sync.")
                return@withContext
            }

        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val email = resolvePrimaryEmail(user)
            ?: run {
                Log.w(TAG, "[NeonUserService] Clerk user ${user.id} missing email; skipping Neon sync.")
                return@withContext
            }

        // Generate ID using the same expression as in Prisma schema
        val randomId = generateRandomId()
        val now = java.time.Instant.now().toString()
        val payload = JSONObject().apply {
            put("id", randomId)
            put("clerkUserId", user.id)
            put("email", email)
            put("name", resolveDisplayName(user) ?: JSONObject.NULL)
            put("imageUrl", user.imageUrl ?: JSONObject.NULL)
            put("industry", JSONObject.NULL)
            put("createdAt", now)
            put("updatedAt", now)
            put("skills", JSONArray())
        }

        Log.d(TAG, "[NeonUserService] Neon sync payload: $payload")

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
                    Log.i(TAG, "[NeonUserService] Neon record exists for ${user.id}; attempting to update instead.")
                    patchUser(apiUrl, authorizationHeader, user.id, payload)
                    return@withContext
                }
                throw IOException("Neon query failed (${response.code}): $errorMessage")
            }
            Log.d(TAG, "[NeonUserService] Synced Clerk user ${user.id} with Neon.")
        }
    }

    suspend fun updateUserProfile(
        clerkUserId: String,
        profile: UserProfileUpdate,
        authToken: String? = null,
    ) = withContext(Dispatchers.IO) {
        val authorizationHeader = resolveAuthorizationHeader(authToken)
            ?: run {
                Log.w(TAG, "[NeonUserService] No Neon auth credentials available; skipping user profile update.")
                return@withContext
            }

        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val payload = JSONObject().apply {
            if (profile.industry != null) {
                put("industry", profile.industry)
            } else {
                put("industry", JSONObject.NULL)
            }
            if (profile.experienceYears != null) {
                put("experience", profile.experienceYears)
            } else {
                put("experience", JSONObject.NULL)
            }
            put("skills", JSONArray(profile.skills))
            if (profile.bio != null) {
                put("bio", profile.bio)
            } else {
                put("bio", JSONObject.NULL)
            }
        }

        Log.d(TAG, "[NeonUserService] Updating Neon user $clerkUserId with onboarding profile: $payload")
        patchUser(apiUrl, authorizationHeader, clerkUserId, payload)
        Log.d(TAG, "[NeonUserService] Updated onboarding profile for Neon user $clerkUserId.")
    }

    suspend fun getUser(
        clerkUserId: String,
        authToken: String? = null,
    ): NeonUser? = withContext(Dispatchers.IO) {
        val authorizationHeader = resolveAuthorizationHeader(authToken) ?: return@withContext null
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val encodedClerkId = URLEncoder.encode(clerkUserId, UTF_8.name())
        val request = Request.Builder()
            .url("$apiUrl/User?clerkUserId=eq.$encodedClerkId&limit=1")
            .addHeader("Authorization", authorizationHeader)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) return@withContext null
            val results = JSONArray(bodyString)
            if (results.length() == 0) return@withContext null
            val json = results.getJSONObject(0)
            val neonId = json.getString("id") // The auto-generated hex ID
            val industry = json.optString("industry").takeIf { it.isNotBlank() }
            val experience = if (json.has("experience") && !json.isNull("experience")) json.optInt("experience") else null
            val skillsArray = json.optJSONArray("skills") ?: JSONArray()
            val skills = List(skillsArray.length()) { i -> skillsArray.optString(i) }.filter { it.isNotBlank() }
            val bio = json.optString("bio").takeIf { it.isNotBlank() }
            return@use NeonUser(
                id = neonId,
                clerkUserId = clerkUserId,
                industry = industry,
                experienceYears = experience,
                skills = skills,
                bio = bio
            )
        }
    }

    private fun resolveAuthorizationHeader(authToken: String?): String? {
        val bearerToken = authToken?.takeUnless { it.isBlank() }
            ?: BuildConfig.NEON_API_KEY.takeUnless { it.isBlank() }
        val basicAuthHeader = BuildConfig.NEON_DB_ROLE.takeUnless { it.isBlank() }?.let { role ->
            val password = BuildConfig.NEON_DB_PASSWORD.takeUnless { it.isBlank() } ?: return@let null
            val credentials = "$role:$password"
            val encodedCredentials =
                Base64.encodeToString(credentials.toByteArray(UTF_8), Base64.NO_WRAP)
            "Basic $encodedCredentials"
        }

        return when {
            bearerToken != null -> "Bearer $bearerToken"
            basicAuthHeader != null -> basicAuthHeader
            else -> null
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

    private fun patchUser(
        apiUrl: String,
        authorizationHeader: String,
        clerkUserId: String,
        payload: JSONObject,
    ) {
        // Always update the updatedAt timestamp when patching
        val now = java.time.Instant.now().toString()
        payload.put("updatedAt", now)

        val encodedClerkId = URLEncoder.encode(clerkUserId, UTF_8.name())
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
                    "Neon update failed (${patchResponse.code}) for $clerkUserId: $errorMessage"
                )
            }
            Log.d(TAG, "[NeonUserService] Patched Neon user $clerkUserId.")
        }
    }

    data class UserProfileUpdate(
        val industry: String?,
        val experienceYears: Int?,
        val skills: List<String> = emptyList(),
        val bio: String?,
    )

    data class NeonUser(
        val id: String, // Neon User.id (the auto-generated hex ID used as foreign key)
        val clerkUserId: String,
        val industry: String?,
        val experienceYears: Int?,
        val skills: List<String>,
        val bio: String?,
    )

}
