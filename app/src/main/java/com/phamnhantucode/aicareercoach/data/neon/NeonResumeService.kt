package com.phamnhantucode.aicareercoach.data.neon

import android.util.Base64
import android.util.Log
import com.phamnhantucode.aicareercoach.BuildConfig
import com.phamnhantucode.aicareercoach.data.local.ResumeConverters
import com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume
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
import kotlin.text.Charsets.UTF_8

/**
 * Client for Neon REST SQL API to manage resume data
 */
object NeonResumeService {

    private const val TAG = "NeonResumeService"
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val converter = ResumeConverters()

    /**
     * Creates or updates a resume in Neon database
     */
    suspend fun saveResume(
        resume: Resume,
        userId: String,
        authToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authorizationHeader = resolveAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("No Neon auth credentials available"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val now = java.time.Instant.now().toString()

            // Convert Resume to JSON using the converter
            val resumeJson = converter.fromResume(resume)

            val payload = JSONObject().apply {
                put("id", resume.id)
                put("userId", userId)
                put("content", resumeJson) // Store as JSON text
                put("atsScore", JSONObject.NULL) // Can be calculated later
                put("feedback", JSONObject.NULL)
                put("createdAt", now)
                put("updatedAt", now)
            }

            Log.d(TAG, "Saving resume ${resume.id} for user $userId")

            val request = Request.Builder()
                .url("$apiUrl/Resume?on_conflict=id")
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
                    if (response.code == 409) {
                        // Resume exists, try to update
                        Log.i(TAG, "Resume exists, attempting to update...")
                        return@withContext updateResume(resume, authToken)
                    }
                    return@withContext Result.failure(
                        IOException("Failed to save resume (${response.code}): $errorMessage")
                    )
                }
                Log.d(TAG, "Successfully saved resume ${resume.id}")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving resume", e)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing resume in Neon database
     */
    suspend fun updateResume(
        resume: Resume,
        authToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authorizationHeader = resolveAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("No Neon auth credentials available"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val now = java.time.Instant.now().toString()

            // Convert Resume to JSON using the converter
            val resumeJson = converter.fromResume(resume)

            val payload = JSONObject().apply {
                put("content", resumeJson)
                put("updatedAt", now)
            }

            val encodedId = URLEncoder.encode(resume.id, UTF_8.name())
            val request = Request.Builder()
                .url("$apiUrl/Resume?id=eq.$encodedId")
                .addHeader("Authorization", authorizationHeader)
                .addHeader("Content-Type", "application/json")
                .patch(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    val errorMessage = bodyString ?: "Empty response body"
                    return@withContext Result.failure(
                        IOException("Failed to update resume (${response.code}): $errorMessage")
                    )
                }
                Log.d(TAG, "Successfully updated resume ${resume.id}")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating resume", e)
            Result.failure(e)
        }
    }

    /**
     * Gets a specific resume by ID from Neon database
     */
    suspend fun getResume(
        resumeId: String,
        authToken: String? = null
    ): Result<Resume?> = withContext(Dispatchers.IO) {
        try {
            val authorizationHeader = resolveAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("No Neon auth credentials available"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val encodedId = URLEncoder.encode(resumeId, UTF_8.name())

            val request = Request.Builder()
                .url("$apiUrl/Resume?id=eq.$encodedId&limit=1")
                .addHeader("Authorization", authorizationHeader)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Failed to get resume (${response.code}): $bodyString")
                    )
                }

                val results = JSONArray(bodyString ?: "[]")
                if (results.length() == 0) {
                    return@withContext Result.success(null)
                }

                val json = results.getJSONObject(0)
                val contentJson = json.getString("content")
                val resume = converter.toResume(contentJson)

                Log.d(TAG, "Successfully retrieved resume $resumeId")
                Result.success(resume)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting resume", e)
            Result.failure(e)
        }
    }

    /**
     * Gets all resumes for a user from Neon database
     */
    suspend fun getAllResumesForUser(
        userId: String,
        authToken: String? = null
    ): Result<List<Resume>> = withContext(Dispatchers.IO) {
        try {
            val authorizationHeader = resolveAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("No Neon auth credentials available"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val encodedUserId = URLEncoder.encode(userId, UTF_8.name())

            val request = Request.Builder()
                .url("$apiUrl/Resume?userId=eq.$encodedUserId&order=updatedAt.desc")
                .addHeader("Authorization", authorizationHeader)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Failed to get resumes (${response.code}): $bodyString")
                    )
                }

                val results = JSONArray(bodyString ?: "[]")
                val resumes = mutableListOf<Resume>()

                for (i in 0 until results.length()) {
                    try {
                        val json = results.getJSONObject(i)
                        val contentJson = json.getString("content")
                        val resume = converter.toResume(contentJson)
                        resumes.add(resume)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse resume at index $i", e)
                    }
                }

                Log.d(TAG, "Successfully retrieved ${resumes.size} resumes for user $userId")
                Result.success(resumes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting resumes for user", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a resume from Neon database
     */
    suspend fun deleteResume(
        resumeId: String,
        authToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authorizationHeader = resolveAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("No Neon auth credentials available"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val encodedId = URLEncoder.encode(resumeId, UTF_8.name())

            val request = Request.Builder()
                .url("$apiUrl/Resume?id=eq.$encodedId")
                .addHeader("Authorization", authorizationHeader)
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val bodyString = response.body?.string()
                    return@withContext Result.failure(
                        IOException("Failed to delete resume (${response.code}): $bodyString")
                    )
                }
                Log.d(TAG, "Successfully deleted resume $resumeId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting resume", e)
            Result.failure(e)
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
}
