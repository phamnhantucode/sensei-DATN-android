package com.phamnhantucode.aicareercoach.data.neon

import android.util.Log
import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.BuildConfig
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object NeonUserService {
    private const val TAG = "NeonUserService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ... (Existing methods)
    
    suspend fun syncUser(clerkUserId: String, email: String): Result<NeonUser> = withContext(Dispatchers.IO) {
        val authHeader = resolveAuthorizationHeader()
        if (authHeader == null) {
            return@withContext Result.failure(Exception("Could not resolve specific auth token for Neon."))
        }

        try {
            // 1. Check if user exists
            val existingUser = fetchNeonUser(clerkUserId, authHeader)
            if (existingUser != null) {
                // User exists, return it
                return@withContext Result.success(existingUser)
            }

            // 2. If not, create user
            val newUser = createNeonUser(clerkUserId, email, authHeader)
            if (newUser != null) {
                // Initialize default credit for new user
                createDefaultUserCredit(newUser.id, authHeader, BuildConfig.NEON_API_URL.trimEnd('/'))
                return@withContext Result.success(newUser)
            } else {
                return@withContext Result.failure(Exception("Failed to create user."))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Sync user failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateUserProfile(clerkUserId: String, update: UserProfileUpdate): Result<NeonUser> = withContext(Dispatchers.IO) {
        val authHeader = resolveAuthorizationHeader()
            ?: return@withContext Result.failure(Exception("Auth unavailable"))

        try {
            val user = fetchNeonUser(clerkUserId, authHeader)
                ?: return@withContext Result.failure(Exception("User not found"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val payload = JSONObject().apply {
                update.industry?.let { put("industry", it) }
                update.experienceYears?.let { put("experience", it) }
                if (update.skills.isNotEmpty()) {
                    put("skills", JSONArray(update.skills))
                }
                update.bio?.let { put("bio", it) }
                put("updatedAt", java.time.Instant.now().toString())
            }

            val request = Request.Builder()
                .url("$apiUrl/User?id=eq.${user.id}")
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Update failed: ${response.code}")
                val body = response.body?.string() ?: ""
                val jsonArray = JSONArray(body)
                if (jsonArray.length() > 0) {
                    val updatedJson = jsonArray.getJSONObject(0)
                     Result.success(parseNeonUser(updatedJson, clerkUserId))
                } else {
                    Result.failure(Exception("Update returned no data"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveAuthorizationHeader(authToken: String? = null): String? {
        if (authToken != null) {
            return if (authToken.startsWith("Bearer ", ignoreCase = true) || 
                       authToken.startsWith("Basic ", ignoreCase = true)) {
                authToken
            } else {
                "Bearer $authToken"
            }
        }
        val fetched = NeonAuth.fetchNeonAuthToken()
        return if (!fetched.isNullOrBlank()) "Bearer $fetched" else null
    }

    private fun fetchNeonUser(clerkUserId: String, authorizationHeader: String): NeonUser? {
        val encodedClerkId = URLEncoder.encode(clerkUserId, "UTF-8")
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val url = "$apiUrl/User?select=*&clerkUserId=eq.$encodedClerkId&limit=1"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", authorizationHeader)
            .get()
            .build()
            
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank()) {
                 val json = JSONArray(body)
                 if (json.length() > 0) parseNeonUser(json.getJSONObject(0), clerkUserId) else null
            } else null
        }
    }

    private fun createNeonUser(clerkUserId: String, email: String, authorizationHeader: String): NeonUser? {
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val userId = java.util.UUID.randomUUID().toString()
        val now = java.time.Instant.now().toString()

        val payload = JSONObject().apply {
            put("id", userId)
            put("clerkUserId", clerkUserId)
            put("email", email)
            put("createdAt", now)
            put("updatedAt", now)
        }

        val request = Request.Builder()
            .url("$apiUrl/User")
            .addHeader("Authorization", authorizationHeader)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
            
         return client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank()) {
                 val json = JSONArray(body)
                 if (json.length() > 0) parseNeonUser(json.getJSONObject(0), clerkUserId) else null
            } else null
        }
    }
    
    private fun parseNeonUser(json: JSONObject, clerkUserId: String): NeonUser {
        val skillsJson = json.optJSONArray("skills")
        val skills = if (skillsJson != null) {
            (0 until skillsJson.length()).map { skillsJson.optString(it) }
        } else emptyList()
        
        return NeonUser(
            id = json.optString("id"),
            clerkUserId = clerkUserId, // json.optString("clerkUserId")
            industry = json.optString("industry").takeIf { it != "null" && it.isNotBlank() },
            experienceYears = json.optInt("experience").takeUnless { json.isNull("experience") },
            skills = skills,
            bio = json.optString("bio").takeIf { it != "null" && it.isNotBlank() },
            isPaid = json.optBoolean("isPaid", false)
        )
    }

    private fun generateRandomId(): String {
        return java.util.UUID.randomUUID().toString()
    }

    private fun createDefaultUserCredit(
        userId: String,
        authorizationHeader: String,
        apiUrl: String
    ) {
        val randomId = generateRandomId()
        val now = java.time.Instant.now().toString()
        val payload = JSONObject().apply {
            put("id", randomId)
            put("userId", userId)
            put("balance", 10)
            put("createdAt", now)
            put("updatedAt", now)
        }

        Log.d(TAG, "[NeonUserService] Creating default user credit: $payload")

        val request = Request.Builder()
            .url("$apiUrl/UserCredit")
            .addHeader("Authorization", authorizationHeader)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    Log.e(TAG, "[NeonUserService] Failed to create user credit: $body")
                } else {
                    Log.d(TAG, "[NeonUserService] Created default user credit.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[NeonUserService] Exception creating user credit", e)
        }
    }
    
    class InsufficientCreditException(message: String = "Insufficient credits") : IOException(message)

    /**
     * Deducts credits from the user's balance.
     */
    suspend fun deductCredit(
        userId: String,
        amount: Int,
        featureName: String,
        authToken: String? = null
    ) = withContext(Dispatchers.IO) {
        val authorizationHeader = resolveAuthorizationHeader(authToken)
            ?: throw IOException("No Neon auth credentials available.")

        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')

        val balance = fetchCreditBalance(userId, authorizationHeader, apiUrl)

        if (balance < amount) {
            throw InsufficientCreditException("Insufficient credits. Balance: $balance, Required: $amount")
        }

        val newBalance = balance - amount
        val updatePayload = JSONObject().apply {
            put("balance", newBalance)
            put("updatedAt", java.time.Instant.now().toString())
        }
        
        val patchRequest = Request.Builder()
            .url("$apiUrl/UserCredit?userId=eq.$userId")
            .addHeader("Authorization", authorizationHeader)
            .addHeader("Content-Type", "application/json")
            .patch(updatePayload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(patchRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to update credit balance (${response.code})")
            }
        }

        val transactionId = generateRandomId()
        val transactionPayload = JSONObject().apply {
            put("id", transactionId)
            put("userId", userId)
            put("amount", -amount)
            put("type", "USAGE")
            put("description", "Used for $featureName")
            put("createdAt", java.time.Instant.now().toString())
        }

        val transactionRequest = Request.Builder()
            .url("$apiUrl/CreditTransaction")
            .addHeader("Authorization", authorizationHeader)
            .addHeader("Content-Type", "application/json")
            .post(transactionPayload.toString().toRequestBody(jsonMediaType))
            .build()
            
        try {
            client.newCall(transactionRequest).execute().use {
                if (!it.isSuccessful) Log.w(TAG, "Failed to log credit transaction: ${it.code}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log credit transaction", e)
        }
        
        Log.d(TAG, "Deducted $amount credits from user $userId for $featureName. New balance: $newBalance")
    }

    suspend fun fetchNeonUserId(clerkUserId: String, authorizationHeader: String): String {
        val encodedClerkId = URLEncoder.encode(clerkUserId, "UTF-8")
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val userRequestUrl = "$apiUrl/User?select=id&clerkUserId=eq.$encodedClerkId&limit=1"

        val request = Request.Builder()
            .url(userRequestUrl)
            .addHeader("Authorization", authorizationHeader)
            .get()
            .build()
            
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                    ?: throw IOException("Neon user fetch returned an empty body.")
                if (!response.isSuccessful) {
                    throw IOException("Neon user fetch failed (${response.code}): $bodyString")
                }

                val results = JSONArray(bodyString)
                if (results.length() == 0) {
                    throw IllegalStateException("No Neon user record found. Complete onboarding first.")
                }

                results.getJSONObject(0).optString("id")
            }
        }
    }

    private fun fetchCreditBalance(userId: String, authorizationHeader: String, apiUrl: String): Int {
        val requestUrl = "$apiUrl/UserCredit?userId=eq.$userId&select=balance&limit=1"
        
         val request = Request.Builder()
            .url(requestUrl)
            .addHeader("Authorization", authorizationHeader)
            .get()
            .build()
            
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch credit balance (${response.code}): $body")
            }
            
            if (!body.isNullOrBlank()) {
                val json = JSONArray(body)
                if (json.length() > 0) {
                    return json.getJSONObject(0).optInt("balance")
                }
            }
            return 0
        }
    }

    data class UserProfileUpdate(
        val industry: String?,
        val experienceYears: Int?,
        val skills: List<String> = emptyList(),
        val bio: String?,
    )

    data class NeonUser(
        val id: String,
        val clerkUserId: String,
        val industry: String?,
        val experienceYears: Int?,
        val skills: List<String>,
        val bio: String?,
        val isPaid: Boolean = false,
    )

}
