package com.phamnhantucode.aicareercoach.data.coverletter

import android.util.Base64
import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.fetchToken
import com.phamnhantucode.aicareercoach.BuildConfig
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.text.Charsets.UTF_8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// Manages cover letters using Gemini and Neon DB
class CoverLetterRepository(
    private val client: OkHttpClient = OkHttpClient(),
) {

    suspend fun fetchUserCoverLetters(): List<CoverLetterRecord> = withContext(Dispatchers.IO) {
        val user = Clerk.user
            ?: throw IllegalStateException("User session unavailable. Please sign in again.")

        return@withContext executeWithAuthRetry { authHeader ->
            val email = user.emailAddresses.firstOrNull()?.emailAddress ?: throw IllegalStateException("User email not found")
            val result = com.phamnhantucode.aicareercoach.data.neon.NeonUserService.syncUser(user.id, email)
            val neonUser = result.getOrNull() ?: throw IllegalStateException("Failed to sync Neon user")
            fetchCoverLettersForUser(neonUser.id, authHeader)
        }
    }

    suspend fun generateCoverLetter(
        companyName: String,
        jobTitle: String,
        jobDescription: String
    ): GeneratedCoverLetter = withContext(Dispatchers.IO) {
        val user = Clerk.user
            ?: throw IllegalStateException("User session unavailable. Please sign in again.")

        val userProfile = executeWithAuthRetry { authHeader ->
            // Deduct Credit
            val email = user.emailAddresses.firstOrNull()?.emailAddress ?: throw IllegalStateException("User email not found")
            val result = com.phamnhantucode.aicareercoach.data.neon.NeonUserService.syncUser(user.id, email)
            val neonUser = result.getOrNull() ?: throw IllegalStateException("Failed to sync Neon user")
            
            com.phamnhantucode.aicareercoach.data.neon.NeonUserService.deductCredit(neonUser.id, 1, "Cover Letter Generation", authHeader)

            fetchUserProfile(user.id, authHeader)
        }

        val prompt = buildCoverLetterPrompt(
            companyName = companyName,
            jobTitle = jobTitle,
            jobDescription = jobDescription,
            userProfile = userProfile
        )

        try {
            val messages = listOf(
                com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.Message(
                    role = "system",
                    content = "You are an expert career coach and professional writer specialized in creating compelling cover letters."
                ),
                com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.Message(
                    role = "user",
                    content = prompt
                )
            )

            val content = com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.chatCompletion(
                messages = messages,
                model = "google/gemini-2.5-flash-lite" // Can be configured or left to default
            )

            return@withContext GeneratedCoverLetter(content = content.trim())
        } catch (e: Exception) {
            throw IOException("Failed to generate cover letter: ${e.message}", e)
        }
    }

    suspend fun saveCoverLetter(
        companyName: String,
        jobTitle: String,
        jobDescription: String,
        content: String,
        status: String = "draft"
    ): CoverLetterRecord = withContext(Dispatchers.IO) {
        val user = Clerk.user
            ?: throw IllegalStateException("User session unavailable. Please sign in again.")

        return@withContext executeWithAuthRetry { authHeader ->
            val email = user.emailAddresses.firstOrNull()?.emailAddress ?: throw IllegalStateException("User email not found")
            val result = com.phamnhantucode.aicareercoach.data.neon.NeonUserService.syncUser(user.id, email)
            val neonUser = result.getOrNull() ?: throw IllegalStateException("Failed to sync Neon user")
            val userId = neonUser.id

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val payload = JSONObject().apply {
                // Make random ID
                val randomBytes = ByteArray(12)
                java.security.SecureRandom().nextBytes(randomBytes)
                val hexId = randomBytes.joinToString("") { "%02x".format(it) }
                put("id", hexId)
                put("userId", userId)
                put("companyName", companyName)
                put("jobTitle", jobTitle)
                put("jobDescription", jobDescription)
                put("content", content)
                put("status", status)
                put("createdAt", Instant.now().toString())
                put("updatedAt", Instant.now().toString())
            }

            val request = Request.Builder()
                .url("$apiUrl/CoverLetter")
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", JSON_MEDIA_TYPE)
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("Failed to save cover letter (${response.code}): $bodyString")
                }
                if (bodyString.isBlank()) {
                    throw IOException("Neon returned an empty response when creating cover letter.")
                }

                val results = JSONArray(bodyString)
                if (results.length() == 0) {
                    throw IOException("Neon did not return the created cover letter.")
                }
                return@use parseCoverLetter(results.getJSONObject(0))
            }
        }
    }

    suspend fun updateCoverLetter(
        id: String,
        content: String
    ): CoverLetterRecord = withContext(Dispatchers.IO) {
        return@withContext executeWithAuthRetry { authHeader ->
            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val encodedId = URLEncoder.encode(id, UTF_8.name())

            val payload = JSONObject().apply {
                put("content", content)
                put("updatedAt", Instant.now().toString())
            }

            val request = Request.Builder()
                .url("$apiUrl/CoverLetter?id=eq.$encodedId")
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", JSON_MEDIA_TYPE)
                .addHeader("Prefer", "return=representation")
                .patch(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("Failed to update cover letter (${response.code}): $bodyString")
                }
                if (bodyString.isBlank()) {
                    throw IOException("Neon returned an empty response when updating cover letter.")
                }

                val results = JSONArray(bodyString)
                if (results.length() == 0) {
                    throw IOException("Neon did not return the updated cover letter.")
                }
                return@use parseCoverLetter(results.getJSONObject(0))
            }
        }
    }

    suspend fun deleteCoverLetter(id: String) = withContext(Dispatchers.IO) {
        executeWithAuthRetry { authHeader ->
            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val encodedId = URLEncoder.encode(id, UTF_8.name())

            val request = Request.Builder()
                .url("$apiUrl/CoverLetter?id=eq.$encodedId")
                .addHeader("Authorization", authHeader)
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val bodyString = response.body?.string().orEmpty()
                    throw IOException("Failed to delete cover letter (${response.code}): $bodyString")
                }
            }
        }
    }

    private fun buildCoverLetterPrompt(
        companyName: String,
        jobTitle: String,
        jobDescription: String,
        userProfile: UserProfile
    ): String {
        return """
            Write a professional cover letter for a $jobTitle position at $companyName.
    
            About the candidate:
            - Industry: ${userProfile.industry}
            - Years of Experience: ${userProfile.experience ?: "Not specified"}
            - Skills: ${userProfile.skills.joinToString(", ")}
            - Professional Background: ${userProfile.bio}
    
            Job Description:
            $jobDescription
    
            Requirements:
            1. Use a professional, enthusiastic tone
            2. Highlight relevant skills and experience
            3. Show understanding of the company's needs
            4. Keep it concise (max 400 words)
            5. Use proper business letter formatting in markdown
            6. Include specific examples of achievements
            7. Relate candidate's background to job requirements
    
            Format the letter in markdown.
        """.trimIndent()
    }

    private suspend fun resolveAuthorizationHeader(forceRefresh: Boolean = false): String? {
        val bearer = if (forceRefresh) {
            // Get new token
            fetchClerkSessionToken()
        } else {
            fetchClerkSessionToken()
                ?: BuildConfig.NEON_API_KEY.takeUnless { it.isBlank() }
        }

        val basicAuth = BuildConfig.NEON_DB_ROLE.takeUnless { it.isBlank() }?.let { role ->
            val password = BuildConfig.NEON_DB_PASSWORD.takeUnless { it.isBlank() } ?: return@let null
            val credentials = "$role:$password"
            val encoded = Base64.encodeToString(credentials.toByteArray(UTF_8), Base64.NO_WRAP)
            "Basic $encoded"
        }

        return when {
            bearer != null -> "Bearer $bearer"
            basicAuth != null -> basicAuth
            else -> null
        }
    }

    private suspend fun <T> executeWithAuthRetry(block: suspend (authHeader: String) -> T): T {
        var authHeader = resolveAuthorizationHeader()
            ?: throw IllegalStateException("No Neon authentication method configured.")

        return try {
            block(authHeader)
        } catch (e: IOException) {
            
            if (e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true) {
                Log.w(TAG, "Got 401 error, refreshing auth token and retrying...")
                // Refresh and retry
                authHeader = resolveAuthorizationHeader(forceRefresh = true)
                    ?: throw IllegalStateException("Failed to refresh authentication token.")
                block(authHeader)
            } else {
                throw e
            }
        }
    }

    private suspend fun fetchClerkSessionToken(): String? {
        val session = Clerk.session ?: return null

        // Get fresh token
        return when (val result = session.fetchToken()) {
            is ClerkResult.Success -> result.value.jwt.takeUnless { it.isBlank() }
            is ClerkResult.Failure -> {
                Log.w(TAG, "Failed to fetch fresh Clerk token: ${result.error}")
                // Try cached token
                session.lastActiveToken?.jwt?.takeUnless { it.isBlank() }
            }
            else -> null
        }
    }

    private fun fetchNeonUserId(clerkUserId: String, authorizationHeader: String): String {
        val encodedClerkId = URLEncoder.encode(clerkUserId, UTF_8.name())
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val userRequestUrl = "$apiUrl/User?select=id&clerkUserId=eq.$encodedClerkId&limit=1"

        val userRequest = Request.Builder()
            .url(userRequestUrl)
            .addHeader("Authorization", authorizationHeader)
            .get()
            .build()

        return client.newCall(userRequest).execute().use { response ->
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

    private fun fetchUserProfile(clerkUserId: String, authorizationHeader: String): UserProfile {
        val encodedClerkId = URLEncoder.encode(clerkUserId, UTF_8.name())
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val userRequestUrl = "$apiUrl/User?select=industry,skills,bio,experience&clerkUserId=eq.$encodedClerkId&limit=1"

        val userRequest = Request.Builder()
            .url(userRequestUrl)
            .addHeader("Authorization", authorizationHeader)
            .get()
            .build()

        return client.newCall(userRequest).execute().use { response ->
            val bodyString = response.body?.string()
                ?: throw IOException("Neon user profile fetch returned an empty body.")
            if (!response.isSuccessful) {
                throw IOException("Neon user profile fetch failed (${response.code}): $bodyString")
            }

            val results = JSONArray(bodyString)
            if (results.length() == 0) {
                throw IllegalStateException("No Neon user record found.")
            }

            val userJson = results.getJSONObject(0)
            UserProfile(
                industry = userJson.optString("industry", ""),
                skills = parseStringArray(userJson.optJSONArray("skills")),
                bio = userJson.optString("bio", ""),
                experience = userJson.optInt("experience", -1).takeIf { it >= 0 }
            )
        }
    }

    private fun fetchCoverLettersForUser(userId: String, authorizationHeader: String): List<CoverLetterRecord> {
        val encodedUserId = URLEncoder.encode(userId, UTF_8.name())
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val requestUrl = "$apiUrl/CoverLetter?select=*&userId=eq.$encodedUserId&order=createdAt.desc"

        val request = Request.Builder()
            .url(requestUrl)
            .addHeader("Authorization", authorizationHeader)
            .get()
            .build()

        return client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string()
                ?: throw IOException("Neon cover letters fetch returned an empty body.")
            if (!response.isSuccessful) {
                throw IOException("Neon cover letters fetch failed (${response.code}): $bodyString")
            }

            val results = JSONArray(bodyString)
            List(results.length()) { index ->
                parseCoverLetter(results.getJSONObject(index))
            }
        }
    }

    private fun parseCoverLetter(json: JSONObject): CoverLetterRecord {
        return CoverLetterRecord(
            id = json.optString("id", ""),
            userId = json.optString("userId", ""),
            companyName = json.optString("companyName", ""),
            jobTitle = json.optString("jobTitle", ""),
            jobDescription = json.optString("jobDescription", ""),
            content = json.optString("content", ""),
            status = json.optString("status", "draft"),
            createdAt = parseInstant(json.optString("createdAt")),
            updatedAt = parseInstant(json.optString("updatedAt"))
        )
    }

    private fun parseInstant(value: String): Instant {
        if (value.isBlank()) return Instant.EPOCH
        return runCatching { Instant.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).toInstant() }
            .getOrElse { Instant.EPOCH }
    }

    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return List(array.length()) { index -> array.optString(index) }
            .filter { it.isNotBlank() }
    }

    data class CoverLetterRecord(
        val id: String,
        val userId: String,
        val companyName: String,
        val jobTitle: String,
        val jobDescription: String,
        val content: String,
        val status: String,
        val createdAt: Instant,
        val updatedAt: Instant
    )

    data class UserProfile(
        val industry: String,
        val skills: List<String>,
        val bio: String,
        val experience: Int?
    )

    data class GeneratedCoverLetter(
        val content: String
    )

    companion object {
        private const val TAG = "CoverLetterRepository"
        private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
    }
}
