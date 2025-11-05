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
import java.util.concurrent.TimeUnit
import kotlin.text.Charsets.UTF_8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository for managing cover letter generation via Gemini AI and storage in Neon database.
 */
class CoverLetterRepository(
    private val client: OkHttpClient = OkHttpClient(),
) {

    suspend fun fetchUserCoverLetters(): List<CoverLetterRecord> = withContext(Dispatchers.IO) {
        val user = Clerk.user
            ?: throw IllegalStateException("User session unavailable. Please sign in again.")

        return@withContext executeWithAuthRetry { authHeader ->
            val userId = fetchNeonUserId(user.id, authHeader)
            fetchCoverLettersForUser(userId, authHeader)
        }
    }

    suspend fun generateCoverLetter(
        companyName: String,
        jobTitle: String,
        jobDescription: String,
    ): GeneratedCoverLetter = withContext(Dispatchers.IO) {
        val user = Clerk.user
            ?: throw IllegalStateException("User session unavailable. Please sign in again.")

        val userProfile = executeWithAuthRetry { authHeader ->
            fetchUserProfile(user.id, authHeader)
        }

        val prompt = buildCoverLetterPrompt(
            companyName = companyName,
            jobTitle = jobTitle,
            jobDescription = jobDescription,
            userProfile = userProfile
        )

        return@withContext callGeminiApi(prompt)
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
            val userId = fetchNeonUserId(user.id, authHeader)

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val payload = JSONObject().apply {
                // Generate a random hex ID similar to what the database would generate
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
        val industryContext = if (userProfile.industry.isNotBlank()) {
            "The candidate works in the ${userProfile.industry} industry."
        } else ""

        val skillsContext = if (userProfile.skills.isNotEmpty()) {
            "Key skills: ${userProfile.skills.joinToString(", ")}."
        } else ""

        val experienceContext = if (userProfile.experience != null) {
            "Years of experience: ${userProfile.experience}."
        } else ""

        val bioContext = if (userProfile.bio.isNotBlank()) {
            "About the candidate: ${userProfile.bio}"
        } else ""

        return """
            Write a professional cover letter for a job application with the following details:
            
            Job Title: $jobTitle
            Company: $companyName
            Job Description: $jobDescription
            
            Candidate Information:
            $industryContext
            $skillsContext
            $experienceContext
            $bioContext
            
            Requirements:
            - Write in a professional, confident, and personable tone
            - Highlight relevant skills and experience from the candidate's profile that match the job description
            - Include specific examples of how the candidate's background aligns with the role
            - Show enthusiasm for the company and position
            - Keep it concise (3-4 paragraphs)
            - Start with "Dear Hiring Manager,"
            - End with "Thank you for your consideration," followed by "[Your Name]"
            - Format as a plain text email body (no special formatting)
            
            Return ONLY the cover letter text, no additional commentary or explanations.
        """.trimIndent()
    }

    private suspend fun callGeminiApi(prompt: String): GeneratedCoverLetter {
        val requestUrl = HttpUrl.Builder()
            .scheme("https")
            .host(GEMINI_API_HOST)
            .addPathSegments("v1beta/models/$GEMINI_MODEL_NAME:generateContent")
            .build()

        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put(
                                "parts",
                                JSONArray().apply {
                                    put(JSONObject().apply { put("text", prompt) })
                                }
                            )
                        }
                    )
                }
            )
        }

        val request = Request.Builder()
            .url(requestUrl)
            .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .addHeader("Content-Type", JSON_MEDIA_TYPE)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        val timeoutClient = client.newBuilder()
            .callTimeout(2, TimeUnit.MINUTES)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val rawText = timeoutClient.newCall(request).execute().use { response ->
            val bodyString = response.body?.string()
                ?: throw IOException("Gemini returned an empty response.")
            if (!response.isSuccessful) {
                throw IOException("Gemini request failed (${response.code}): $bodyString")
            }
            extractGeminiText(JSONObject(bodyString))
                ?: throw IOException("Gemini response did not include text content.")
        }

        return GeneratedCoverLetter(content = rawText.trim())
    }

    private fun extractGeminiText(response: JSONObject): String? {
        val candidates = response.optJSONArray("candidates") ?: return null
        for (i in 0 until candidates.length()) {
            val candidate = candidates.optJSONObject(i) ?: continue
            val content = candidate.optJSONObject("content") ?: continue
            val parts = content.optJSONArray("parts") ?: continue
            val collected = buildString {
                for (j in 0 until parts.length()) {
                    val part = parts.optJSONObject(j) ?: continue
                    val text = part.optString("text")
                    if (!text.isNullOrBlank()) append(text)
                }
            }
            if (collected.isNotBlank()) return collected
        }
        return null
    }

    private suspend fun resolveAuthorizationHeader(forceRefresh: Boolean = false): String? {
        val bearer = if (forceRefresh) {
            // Force fetch fresh token, skip cached
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
            // Check if it's a 401 Unauthorized error
            if (e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true) {
                Log.w(TAG, "Got 401 error, refreshing auth token and retrying...")
                // Refresh auth header and retry once
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

        // Always fetch a fresh token to avoid using expired cached tokens
        return when (val result = session.fetchToken()) {
            is ClerkResult.Success -> result.value.jwt.takeUnless { it.isBlank() }
            is ClerkResult.Failure -> {
                Log.w(TAG, "Failed to fetch fresh Clerk token: ${result.error}")
                // As fallback, try cached token (might be expired but worth trying)
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
        private const val GEMINI_API_HOST = "generativelanguage.googleapis.com"
        private const val GEMINI_MODEL_NAME = "gemini-2.5-flash"
        private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
    }
}
