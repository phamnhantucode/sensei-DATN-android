package com.phamnhantucode.aicareercoach.data.coverletter

import android.util.Base64
import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.fetchToken
import com.phamnhantucode.aicareercoach.BuildConfig
import com.phamnhantucode.aicareercoach.data.coverletter.EmailType
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

    suspend fun generateEmail(
        type: EmailType,
        inputs: Map<String, String>
    ): GeneratedCoverLetter = withContext(Dispatchers.IO) {
        val user = Clerk.user
            ?: throw IllegalStateException("User session unavailable. Please sign in again.")

        val userProfile = executeWithAuthRetry { authHeader ->

            val email = user.emailAddresses.firstOrNull()?.emailAddress ?: throw IllegalStateException("User email not found")
            val result = com.phamnhantucode.aicareercoach.data.neon.NeonUserService.syncUser(user.id, email)
            val neonUser = result.getOrNull() ?: throw IllegalStateException("Failed to sync Neon user")
            
            val creditDescription = "Generate ${type.displayName}"
            com.phamnhantucode.aicareercoach.data.neon.NeonUserService.deductCredit(neonUser.id, 1, creditDescription, authHeader)
            
            // We do NOT fetch user profile here to avoid contaminating the prompt with onboarding data.
            // We rely on inputs provided by the user in the form.
        }

        val candidateProfile = """
            CANDIDATE PROFILE:
            - Role/Industry: ${inputs["userIndustry"] ?: "Unspecified"}
            - Experience: ${inputs["userExperience"] ?: "Not specified"} years
            - Core Skills: ${inputs["userSkills"] ?: "Not specified"}
            - Background: ${inputs["userBio"] ?: "Not specified"}
        """.trimIndent()

        val prompt = com.phamnhantucode.aicareercoach.data.ai.PromptFactory.createCoverLetterPrompt(
            context = candidateProfile,
            type = type,
            inputs = inputs
        )

        try {
            val messages = listOf(
                com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.Message(
                    role = "system",
                    content = "You are an expert career coach and professional writer specialized in job search communication."
                ),
                com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.Message(
                    role = "user",
                    content = prompt
                )
            )

            val content = com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.chatCompletion(
                messages = messages,
                model = "google/gemini-2.5-flash-lite"
            )

            return@withContext GeneratedCoverLetter(content = content.trim())
        } catch (e: Exception) {
            throw IOException("Failed to generate email: ${e.message}", e)
        }
    }

    suspend fun saveCoverLetter(
        type: EmailType,
        companyName: String,
        recipient: String,
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

                val randomBytes = ByteArray(12)
                java.security.SecureRandom().nextBytes(randomBytes)
                val hexId = randomBytes.joinToString("") { "%02x".format(it) }
                put("id", hexId)
                put("userId", userId)
                put("type", type.id)
                put("companyName", companyName)
                put("recipient", recipient)
                put("title", jobTitle)
                put("description", jobDescription)
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
                    throw IOException("Failed to save email (${response.code}): $bodyString")
                }
                if (bodyString.isBlank()) {
                    throw IOException("Neon returned an empty response when creating email.")
                }

                val results = JSONArray(bodyString)
                if (results.length() == 0) {
                    throw IOException("Neon did not return the created email.")
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
                    throw IOException("Failed to update email (${response.code}): $bodyString")
                }
                if (bodyString.isBlank()) {
                    throw IOException("Neon returned an empty response when updating email.")
                }

                val results = JSONArray(bodyString)
                if (results.length() == 0) {
                    throw IOException("Neon did not return the updated email.")
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
                    throw IOException("Failed to delete email (${response.code}): $bodyString")
                }
            }
        }
    }



    private suspend fun resolveAuthorizationHeader(forceRefresh: Boolean = false): String? {
        val bearer = if (forceRefresh) {
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
        return when (val result = session.fetchToken()) {
            is ClerkResult.Success -> result.value.jwt.takeUnless { it.isBlank() }
            is ClerkResult.Failure -> {
                Log.w(TAG, "Failed to fetch fresh Clerk token: ${result.error}")
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
            type = EmailType.fromId(json.optString("type", EmailType.APPLICATION.id)),
            companyName = json.optString("companyName", ""),
            recipient = json.optString("recipient", ""),
            jobTitle = json.optString("title", ""),
            jobDescription = json.optString("description", ""),
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
        val type: EmailType,
        val companyName: String,
        val recipient: String,
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
