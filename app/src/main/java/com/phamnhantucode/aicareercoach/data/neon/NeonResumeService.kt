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

/**
 * Client for Neon REST SQL API to manage resume data
 */
object NeonResumeService {

    private const val TAG = "NeonResumeService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Creates or updates a resume in Neon database (all tables)
     * @param gridResume Optional GridResume to store in the 'json' field instead of form data
     */
    suspend fun saveResume(
        resume: Resume,
        userId: String,
        authToken: String? = null,
        gridResume: GridResume? = null
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
                return@withContext updateResume(resume, authToken, gridResume)
            } else {
                // Resume doesn't exist, create new one
                Log.d(TAG, "[NeonSync] [$operationId] Creating new resume")
                val now = java.time.Instant.now().toString()

                // 1. Save to Resume table
                val payload = NeonResumeMapper.toNeonResumePayload(resume, userId, gridResume).apply {
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
                            return@withContext updateResume(resume, authToken, gridResume)
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

    /**
     * Saves data to related tables (PersonalInfo, Education, Experience, Project)
     */
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
            val payload = JSONObject().apply {
                put("id", java.util.UUID.randomUUID().toString())
                put("resumeId", resume.id)
                put("fullName", personalInfo.fullName)
                put("email", personalInfo.email)
                put("phone", personalInfo.phone)
                put("location", personalInfo.location)
                put("linkedin", personalInfo.linkedIn)
                put("website", personalInfo.portfolio)
                put("image", personalInfo.avatar)
                put("profession", "") // App doesn't have this
            }

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
                val payload = JSONObject().apply {
                    put("id", edu.id)
                    put("resumeId", resume.id)
                    put("degree", edu.degree)
                    put("institution", edu.institution)
                    put("field", "") // Extract from degree if needed
                    put("graduationDate", edu.endDate?.toString() ?: "")
                    put("gpa", edu.gpa)
                }

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
                val payload = JSONObject().apply {
                    put("id", exp.id)
                    put("resumeId", resume.id)
                    put("title", exp.jobTitle)
                    put("organization", exp.company)
                    put("description", exp.responsibilities.joinToString("\n• ", prefix = "• "))
                    put("startDate", exp.startDate?.toString() ?: "")
                    put("endDate", exp.endDate?.toString() ?: "")
                    put("isCurrent", exp.isCurrentRole)
                }

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
                val payload = JSONObject().apply {
                    put("id", project.id)
                    put("resumeId", resume.id)
                    put("name", project.title)
                    put("description", project.description)
                    put("type", project.technologies.firstOrNull() ?: "Other")
                }

                val request = Request.Builder()
                    .url("$apiUrl/ResumeProject")
                    .addHeader("Authorization", authorizationHeader)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

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

    /**
     * Updates an existing resume in Neon database
     * @param gridResume Optional GridResume to store in the 'json' field instead of form data
     */
    suspend fun updateResume(
        resume: Resume,
        authToken: String? = null,
        gridResume: GridResume? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val operationId = "updateResume-${resume.id.take(8)}"
        Log.d(TAG, "[NeonSync] [$operationId] Starting update")
        try {
            val authorizationHeader = resolveAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("No Neon auth credentials available"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val now = java.time.Instant.now().toString()

            // 1. Update Resume table
            val payload = NeonResumeMapper.toNeonResumeUpdatePayload(resume, gridResume).apply {
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

    /**
     * Deletes related table entries for a resume
     */
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

    /**
     * Gets a specific resume by ID from Neon database
     */
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

            val request = Request.Builder()
                .url("$apiUrl/Resume?id=eq.$encodedId&limit=1")
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
                
                // Use mapper to parse Neon row to Resume
                val resume = NeonResumeMapper.fromNeonResumeRow(json)

                Log.d(TAG, "[NeonSync] [$operationId] Successfully retrieved resume")
                Result.success(resume)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[NeonSync] [$operationId] Error getting resume", e)
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
        val operationId = "getAllResumes-${userId.take(8)}"
        Log.d(TAG, "[NeonSync] [$operationId] Fetching resumes for user")
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

    /**
     * Deletes a resume from Neon database (including related tables)
     */
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
