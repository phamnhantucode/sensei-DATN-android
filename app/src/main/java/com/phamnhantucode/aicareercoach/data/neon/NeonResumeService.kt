package com.phamnhantucode.aicareercoach.data.neon

import android.util.Base64
import android.util.Log
import com.phamnhantucode.aicareercoach.BuildConfig
import com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridResume
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
import kotlin.text.Charsets.UTF_8

// Client for Neon Resume data
object NeonResumeService {

    private const val TAG = "NeonResumeService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Creates or updates a resume
    suspend fun saveResume(
        resume: Resume,
        userId: String,
        authToken: String? = null,
        gridResume: GridResume? = null,
        preserveExistingJson: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val operationId = "saveResume-${resume.id.take(8)}"
        Log.d(TAG, "[NeonSync] [$operationId] Starting save for user $userId")
        try {
            val authorizationHeader = resolveAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("No Neon auth credentials available"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')

            // First, check if resume already exists
            val existingResume = getResume(resume.id, authToken)

            if (existingResume.isSuccess && existingResume.getOrNull() != null) {
                // Resume exists, update it
                Log.d(TAG, "[NeonSync] [$operationId] Resume already exists, updating...")
                return@withContext updateResume(resume, authToken, gridResume, preserveExistingJson)
            } else {
                // Resume doesn't exist, create new one
                Log.d(TAG, "[NeonSync] [$operationId] Creating new resume")
                val now = java.time.Instant.now().toString()

                // 1. Save to Resume table
                // For new resumes, don't preserve existing json (there is none)
                val payload = NeonResumeMapper.toNeonResumePayload(resume, userId, gridResume, preserveExistingJson = false).apply {
                    put("atsScore", JSONObject.NULL)
                    put("feedback", JSONObject.NULL)
                    put("createdAt", now)
                    put("updatedAt", now)
                }

                val request = Request.Builder()
                    .url("$apiUrl/Resume")
                    .addHeader("Authorization", authorizationHeader)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string()
                    if (!response.isSuccessful) {
                        // Check for 409 Conflict (Duplicate Key)
                        if (response.code == 409) {
                            Log.w(TAG, "[NeonSync] [$operationId] Conflict detected (409), resume already exists. Retrying as update.")
                            return@withContext updateResume(resume, authToken, gridResume, preserveExistingJson)
                        }

                        val errorMessage = bodyString ?: "Empty response body"
                        Log.e(TAG, "[NeonSync] [$operationId] Failed to save to Resume table: ${response.code} - $errorMessage")
                        return@withContext Result.failure(
                            IOException("Failed to save resume (${response.code}): $errorMessage")
                        )
                    }
                    Log.d(TAG, "[NeonSync] [$operationId] Successfully saved to Resume table")
                }

                // 2. Save to related tables
                saveRelatedTables(resume, authorizationHeader, apiUrl, operationId)
                
                Log.d(TAG, "[NeonSync] [$operationId] Completed successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[NeonSync] [$operationId] Error saving resume", e)
            Result.failure(e)
        }
    }

    // Saves related data (PersonalInfo, etc.)
    private suspend fun saveRelatedTables(
        resume: Resume,
        authorizationHeader: String,
        apiUrl: String,
        operationId: String
    ) {
        // Save PersonalInfo
        savePersonalInfo(resume, authorizationHeader, apiUrl, operationId)
        
        // Save Education entries
        saveEducation(resume, authorizationHeader, apiUrl, operationId)
        
        // Save Experience entries
        saveExperience(resume, authorizationHeader, apiUrl, operationId)
        
        // Save Project entries
        saveProjects(resume, authorizationHeader, apiUrl, operationId)
    }

    private suspend fun savePersonalInfo(
        resume: Resume,
        authorizationHeader: String,
        apiUrl: String,
        operationId: String
    ) {
        try {
            val personalInfo = resume.personalInfo
            val payload = NeonResumeMapper.toNeonPersonalInfoPayload(resume.id, resume.personalInfo)

            val request = Request.Builder()
                .url("$apiUrl/ResumePersonalInfo")
                .addHeader("Authorization", authorizationHeader)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "[NeonSync] [$operationId] Failed to save PersonalInfo: ${response.code} - ${response.body?.string()}")
                } else {
                    Log.d(TAG, "[NeonSync] [$operationId] Saved PersonalInfo")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[NeonSync] [$operationId] Error saving PersonalInfo", e)
        }
    }

    private suspend fun saveEducation(
        resume: Resume,
        authorizationHeader: String,
        apiUrl: String,
        operationId: String
    ) {
        if (resume.education.isEmpty()) return
        var successCount = 0
        resume.education.forEach { edu ->
            try {
                val payload = NeonResumeMapper.toNeonEducationPayload(resume.id, edu)

                val request = Request.Builder()
                    .url("$apiUrl/ResumeEducation")
                    .addHeader("Authorization", authorizationHeader)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "[NeonSync] [$operationId] Failed to save Education ${edu.id}: ${response.code}")
                    } else {
                        successCount++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[NeonSync] [$operationId] Error saving Education ${edu.id}", e)
            }
        }
        Log.d(TAG, "[NeonSync] [$operationId] Saved $successCount/${resume.education.size} Education entries")
    }

    private suspend fun saveExperience(
        resume: Resume,
        authorizationHeader: String,
        apiUrl: String,
        operationId: String
    ) {
        if (resume.workExperiences.isEmpty()) return
        var successCount = 0
        resume.workExperiences.forEach { exp ->
            try {
                val payload = NeonResumeMapper.toNeonExperiencePayload(resume.id, exp)

                val request = Request.Builder()
                    .url("$apiUrl/ResumeExperience")
                    .addHeader("Authorization", authorizationHeader)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "[NeonSync] [$operationId] Failed to save Experience ${exp.id}: ${response.code}")
                    } else {
                        successCount++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[NeonSync] [$operationId] Error saving Experience ${exp.id}", e)
            }
        }
        Log.d(TAG, "[NeonSync] [$operationId] Saved $successCount/${resume.workExperiences.size} Experience entries")
    }

    private suspend fun saveProjects(
        resume: Resume,
        authorizationHeader: String,
        apiUrl: String,
        operationId: String
    ) {
        if (resume.projects.isEmpty()) return
        var successCount = 0
        resume.projects.forEach { project ->
            try {
                val payload = NeonResumeMapper.toNeonProjectPayload(resume.id, project)

                val request = Request.Builder()
                    .url("$apiUrl/ResumeProject")
                    .addHeader("Authorization", authorizationHeader)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                    
                Log.d(TAG, "[NeonSync] [$operationId] Saving project payload: $payload")

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "[NeonSync] [$operationId] Failed to save Project ${project.id}: ${response.code}")
                    } else {
                        successCount++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[NeonSync] [$operationId] Error saving Project ${project.id}", e)
            }
        }
        Log.d(TAG, "[NeonSync] [$operationId] Saved $successCount/${resume.projects.size} Project entries")
    }

    // Updates an existing resume
    suspend fun updateResume(
        resume: Resume,
        authToken: String? = null,
        gridResume: GridResume? = null,
        preserveExistingJson: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val operationId = "updateResume-${resume.id.take(8)}"
        Log.d(TAG, "[NeonSync] [$operationId] Starting update")
        try {
            val authorizationHeader = resolveAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("No Neon auth credentials available"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val now = java.time.Instant.now().toString()

            // 1. Update Resume table
            val payload = NeonResumeMapper.toNeonResumeUpdatePayload(resume, gridResume, preserveExistingJson).apply {
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
                    Log.e(TAG, "[NeonSync] [$operationId] Failed to update Resume table: ${response.code} - $errorMessage")
                    return@withContext Result.failure(
                        IOException("Failed to update resume (${response.code}): $errorMessage")
                    )
                }
                Log.d(TAG, "[NeonSync] [$operationId] Successfully updated Resume table")
            }

            // 2. Delete old related data and re-insert
            deleteRelatedTables(resume.id, authorizationHeader, apiUrl, operationId)
            saveRelatedTables(resume, authorizationHeader, apiUrl, operationId)

            Log.d(TAG, "[NeonSync] [$operationId] Completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[NeonSync] [$operationId] Error updating resume", e)
            Result.failure(e)
        }
    }

    // Deletes related table entries
    private suspend fun deleteRelatedTables(
        resumeId: String,
        authorizationHeader: String,
        apiUrl: String,
        operationId: String
    ) {
        val encodedId = URLEncoder.encode(resumeId, UTF_8.name())
        val tables = listOf("ResumePersonalInfo", "ResumeEducation", "ResumeExperience", "ResumeProject")
        
        for (table in tables) {
            val request = Request.Builder()
                .url("$apiUrl/$table?resumeId=eq.$encodedId")
                .addHeader("Authorization", authorizationHeader)
                .delete()
                .build()
            
            Log.d(TAG, "[NeonSync] [$operationId] Deleting from $table: ${request.url}")

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 404) {
                    val bodyString = response.body?.string()
                    throw IOException("Failed to delete from $table (${response.code}): $bodyString")
                }
                // Log.d(TAG, "[NeonSync] [$operationId] Cleared $table") // Optional: uncomment if needed, but might be noisy
            }
        }
        Log.d(TAG, "[NeonSync] [$operationId] Cleared related tables")
    }

    // Gets a specific resume
    suspend fun getResume(
        resumeId: String,
        authToken: String? = null
    ): Result<Resume?> = withContext(Dispatchers.IO) {
        val operationId = "getResume-${resumeId.take(8)}"
        try {
            val authorizationHeader = resolveAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("No Neon auth credentials available"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val encodedId = URLEncoder.encode(resumeId, UTF_8.name())
            
            // Use PostgREST embedding to fetch related tables in one go
            // Query: select=*,ResumePersonalInfo(*),ResumeEducation(*),ResumeExperience(*),ResumeProject(*)
            // Note: URL encoding is handled by OkHttp but for query params we need to be careful
            val selectQuery = "*,ResumePersonalInfo(*),ResumeEducation(*),ResumeExperience(*),ResumeProject(*)"
            
            val request = Request.Builder()
                .url("$apiUrl/Resume?id=eq.$encodedId&select=$selectQuery&limit=1")
                .addHeader("Authorization", authorizationHeader)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "[NeonSync] [$operationId] Failed to get resume: ${response.code} - $bodyString")
                    return@withContext Result.failure(
                        IOException("Failed to get resume (${response.code}): $bodyString")
                    )
                }

                val results = JSONArray(bodyString ?: "[]")
                if (results.length() == 0) {
                    Log.d(TAG, "[NeonSync] [$operationId] Resume not found")
                    return@withContext Result.success(null)
                }

                val json = results.getJSONObject(0)
                
                // Use mapper to parse Neon row to Resume (including raw embedded data)
                val resume = NeonResumeMapper.fromNeonResumeRow(json)

                Log.d(TAG, "[NeonSync] [$operationId] Successfully retrieved resume")
                Result.success(resume)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[NeonSync] [$operationId] Error getting resume", e)
            Result.failure(e)
        }
    }

    // Gets all resumes for a user
    suspend fun getAllResumesForUser(
        userId: String,
        authToken: String? = null
    ): Result<List<Resume>> = withContext(Dispatchers.IO) {
        val operationId = "getAllResumes-${userId.take(8)}"
        Log.d(TAG, "[NeonSync] [$operationId] Fetching resumes for user")
        try {
            val authorizationHeader = resolveAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("No Neon auth credentials available"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val encodedUserId = URLEncoder.encode(userId, UTF_8.name())

            // Use PostgREST embedding to fetch related tables
            val selectQuery = "*,ResumePersonalInfo(*),ResumeEducation(*),ResumeExperience(*),ResumeProject(*)"

            val request = Request.Builder()
                .url("$apiUrl/Resume?userId=eq.$encodedUserId&select=$selectQuery&order=updatedAt.desc")
                .addHeader("Authorization", authorizationHeader)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "[NeonSync] [$operationId] Failed to get resumes: ${response.code} - $bodyString")
                    return@withContext Result.failure(
                        IOException("Failed to get resumes (${response.code}): $bodyString")
                    )
                }

                val results = JSONArray(bodyString ?: "[]")
                val resumes = mutableListOf<Resume>()

                for (i in 0 until results.length()) {
                    try {
                        val json = results.getJSONObject(i)
                        // Use mapper to parse Neon row to Resume
                        val resume = NeonResumeMapper.fromNeonResumeRow(json)
                        resumes.add(resume)
                    } catch (e: Exception) {
                        Log.w(TAG, "[NeonSync] [$operationId] Failed to parse resume at index $i", e)
                    }
                }

                Log.d(TAG, "[NeonSync] [$operationId] Successfully retrieved ${resumes.size} resumes")
                Result.success(resumes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[NeonSync] [$operationId] Error getting resumes for user", e)
            Result.failure(e)
        }
    }

    // Deletes a resume
    suspend fun deleteResume(
        resumeId: String,
        authToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val operationId = "deleteResume-${resumeId.take(8)}"
        Log.d(TAG, "[NeonSync] [$operationId] Starting deletion")
        try {
            val authorizationHeader = resolveAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("No Neon auth credentials available"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val encodedId = URLEncoder.encode(resumeId, UTF_8.name())

            // Delete related tables first (foreign key constraint)
            deleteRelatedTables(resumeId, authorizationHeader, apiUrl, operationId)

            // Delete main resume
            val request = Request.Builder()
                .url("$apiUrl/Resume?id=eq.$encodedId")
                .addHeader("Authorization", authorizationHeader)
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val bodyString = response.body?.string()
                    Log.e(TAG, "[NeonSync] [$operationId] Failed to delete resume: ${response.code} - $bodyString")
                    return@withContext Result.failure(
                        IOException("Failed to delete resume (${response.code}): $bodyString")
                    )
                }
                Log.d(TAG, "[NeonSync] [$operationId] Successfully deleted resume")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[NeonSync] [$operationId] Error deleting resume", e)
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
