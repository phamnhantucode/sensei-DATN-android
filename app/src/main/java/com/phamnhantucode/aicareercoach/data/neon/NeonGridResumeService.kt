package com.phamnhantucode.aicareercoach.data.neon

import android.util.Base64
import android.util.Log
import com.google.gson.GsonBuilder
import com.phamnhantucode.aicareercoach.BuildConfig
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumeElementTypeAdapter
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridResume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
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
 * Client for Neon REST SQL API to manage GridResume data
 * Stores GridResume as JSONB in the 'json' field of the Resume table
 */
object NeonGridResumeService {

    private const val TAG = "NeonGridResumeService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Reuse existing Gson from GridResumeConverters
    private val gson = GsonBuilder()
        .registerTypeAdapter(ResumeElement::class.java, ResumeElementTypeAdapter())
        .serializeNulls()
        .create()

    /**
     * Creates or updates a GridResume in Neon database (stored in Resume table's json field)
     */
    suspend fun saveGridResume(
        gridResume: GridResume,
        userId: String,
        thumbnail: String = "",
        authToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val operationId = "saveGridResume-${gridResume.id.take(8)}"
        Log.d(TAG, "[NeonSync] [$operationId] Starting save for user $userId")
        try {
            val authorizationHeader = resolveAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("No Neon auth credentials available"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')

            // Check if resume already exists (either as GridResume or just a basic Resume)
            // We first try to get it as a GridResume
            val existingGridResume = getGridResume(gridResume.id, authToken)
            
            // If it exists as a GridResume, or if we can confirm the ID exists in the Resume table
            // For now, we'll assume if getGridResume returns success (even if null), the ID might exist
            // But a better check is needed. Let's try to update first, if it fails with 404, then insert.
            // However, Neon/PostgREST doesn't return 404 for update on non-existent, it just updates 0 rows.
            
            // Strategy: Try to update first. If 0 rows updated, then insert.
            // But our updateGridResume throws exception on failure, not row count.
            // So let's stick to the check.
            
            // If getGridResume returns a resume, definitely update
            if (existingGridResume.isSuccess && existingGridResume.getOrNull() != null) {
                Log.d(TAG, "[NeonSync] [$operationId] GridResume already exists, updating...")
                return@withContext updateGridResume(gridResume, thumbnail, authToken)
            } 
            
            // If getGridResume returned null (success but no grid data), it might still exist as a form resume
            // We should try to update it first to avoid PK violation
            Log.d(TAG, "[NeonSync] [$operationId] GridResume not found, attempting update in case it exists as form resume...")
            val updateResult = updateGridResume(gridResume, thumbnail, authToken)
            
            if (updateResult.isSuccess) {
                Log.d(TAG, "[NeonSync] [$operationId] Update successful (resume existed)")
                return@withContext Result.success(Unit)
            }
            
            // If update failed (likely because it didn't exist or other error), try to create
            // Note: We need to distinguish between "doesn't exist" and "network error"
            // For now, if update fails, we assume it might not exist and try insert
            
            Log.d(TAG, "[NeonSync] [$operationId] Update failed or no rows affected, creating new GridResume")
                val now = java.time.Instant.now().toString()

                // Serialize GridResume to JSON string (include thumbnail)
                val gridResumeWithThumbnail = gridResume.copy(thumbnail = thumbnail)
                val gridResumeJsonString = gson.toJson(gridResumeWithThumbnail)

                // Build payload for Resume table
                val payload = JSONObject().apply {
                    put("id", gridResume.id)
                    put("userId", userId)
                    put("content", "") // Empty content for grid-based resumes
                    put("title", gridResume.name) // Save title to main table
                    put("skills", "{}") // Save empty skills array to main table
                    put("json", gridResumeJsonString) // Store GridResume in json field
                    put("template", gridResume.metadata.templateId.ifEmpty { "classic" }) // Save template type
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
                        val errorMessage = bodyString ?: "Empty response body"
                        Log.e(TAG, "[NeonSync] [$operationId] Failed to save GridResume: ${response.code} - $errorMessage")
                        return@withContext Result.failure(
                            IOException("Failed to save GridResume (${response.code}): $errorMessage")
                        )
                    }
                    Log.d(TAG, "[NeonSync] [$operationId] Successfully saved GridResume")
                }

                Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[NeonSync] [$operationId] Error saving GridResume", e)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing GridResume in Neon database
     */
    suspend fun updateGridResume(
        gridResume: GridResume,
        thumbnail: String? = null,
        authToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val operationId = "updateGridResume-${gridResume.id.take(8)}"
        Log.d(TAG, "[NeonSync] [$operationId] Starting update")
        try {
            val authorizationHeader = resolveAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("No Neon auth credentials available"))

            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val encodedId = URLEncoder.encode(gridResume.id, UTF_8.name())
            val now = java.time.Instant.now().toString()

            // Serialize GridResume to JSON string (include thumbnail if provided, or preserve existing)
            val finalThumbnail = when {
                // New thumbnail provided - use it
                thumbnail != null -> thumbnail
                // gridResume already has a thumbnail - keep it
                gridResume.thumbnail.isNotBlank() -> gridResume.thumbnail
                // No thumbnail - try to fetch existing from database to preserve it
                else -> {
                    val existing = getGridResume(gridResume.id, authToken).getOrNull()
                    existing?.thumbnail ?: ""
                }
            }
            val gridResumeWithThumbnail = gridResume.copy(thumbnail = finalThumbnail)
            val gridResumeJsonString = gson.toJson(gridResumeWithThumbnail)
            
            Log.d(TAG, "[NeonSync] [$operationId] Serializing GridResume with thumbnail: ${gridResumeWithThumbnail.thumbnail.take(50)}...")

            // Build update payload for Resume table
            val payload = JSONObject().apply {
                put("json", gridResumeJsonString)
                put("title", gridResume.name) // Update title in main table
                put("template", gridResume.metadata.templateId.ifEmpty { "classic" }) // Update template type
                put("updatedAt", now)
            }

            val request = Request.Builder()
                .url("$apiUrl/Resume?id=eq.$encodedId")
                .addHeader("Authorization", authorizationHeader)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    val errorMessage = bodyString ?: "Empty response body"
                    Log.e(TAG, "[NeonSync] [$operationId] Failed to update GridResume: ${response.code} - $errorMessage")
                    return@withContext Result.failure(
                        IOException("Failed to update GridResume (${response.code}): $errorMessage")
                    )
                }
                Log.d(TAG, "[NeonSync] [$operationId] Successfully updated GridResume")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[NeonSync] [$operationId] Error updating GridResume", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves a specific GridResume from Neon database
     */
    suspend fun getGridResume(
        resumeId: String,
        authToken: String? = null
    ): Result<GridResume?> = withContext(Dispatchers.IO) {
        val operationId = "getGridResume-${resumeId.take(8)}"
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
                    Log.e(TAG, "[NeonSync] [$operationId] Failed to get GridResume: ${response.code} - $bodyString")
                    return@withContext Result.failure(
                        IOException("Failed to get GridResume (${response.code}): $bodyString")
                    )
                }

                val results = JSONArray(bodyString ?: "[]")
                if (results.length() == 0) {
                    Log.d(TAG, "[NeonSync] [$operationId] GridResume not found")
                    return@withContext Result.success(null)
                }

                val row = results.getJSONObject(0)
                val jsonField = row.optString("json")

                if (jsonField.isBlank()) {
                    Log.w(TAG, "[NeonSync] [$operationId] GridResume has no json data")
                    return@withContext Result.success(null)
                }
                
                // Log first 200 chars of json for debugging
                Log.d(TAG, "[NeonSync] [$operationId] JSON field preview: ${jsonField.take(200)}...")
                
                // Quick check if this looks like a GridResume (has 'pages' field)
                try {
                    val jsonObj = JSONObject(jsonField)
                    if (!jsonObj.has("pages")) {
                        Log.w(TAG, "[NeonSync] [$operationId] JSON is not GridResume format (no 'pages' field)")
                        return@withContext Result.success(null)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "[NeonSync] [$operationId] Failed to parse JSON structure: ${e.message}")
                    return@withContext Result.success(null)
                }

                // Try to deserialize as GridResume
                try {
                    val gridResume = gson.fromJson(jsonField, GridResume::class.java)
                    if (gridResume == null || gridResume.pages.isEmpty()) {
                        Log.w(TAG, "[NeonSync] [$operationId] GridResume deserialized to null or empty pages")
                        return@withContext Result.success(null)
                    }
                    Log.d(TAG, "[NeonSync] [$operationId] Successfully retrieved GridResume: id=${gridResume.id}")
                    return@withContext Result.success(gridResume)
                } catch (e: Exception) {
                    // json field might contain old Resume format, not GridResume
                    Log.w(TAG, "[NeonSync] [$operationId] Failed to parse json as GridResume: ${e.message}")
                    return@withContext Result.success(null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[NeonSync] [$operationId] Error getting GridResume", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves all GridResumes for a specific user from Neon database
     */
    suspend fun getAllGridResumesForUser(
        userId: String,
        authToken: String? = null
    ): Result<List<GridResume>> = withContext(Dispatchers.IO) {
        val operationId = "getAllGridResumes-${userId.take(8)}"
        Log.d(TAG, "[NeonSync] [$operationId] Fetching GridResumes for user")
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
                    Log.e(TAG, "[NeonSync] [$operationId] Failed to get GridResumes: ${response.code} - $bodyString")
                    return@withContext Result.failure(
                        IOException("Failed to get GridResumes (${response.code}): $bodyString")
                    )
                }

                val results = JSONArray(bodyString ?: "[]")
                val gridResumes = mutableListOf<GridResume>()

                for (i in 0 until results.length()) {
                    try {
                        val row = results.getJSONObject(i)
                        val jsonField = row.optString("json")
                        val rowId = row.optString("id")

                        if (jsonField.isNotBlank()) {
                            // Quick check if this looks like a GridResume (has 'pages' field)
                            // vs old form-based Resume JSON (has 'personalInfo' field)
                            try {
                                val jsonObj = JSONObject(jsonField)
                                if (!jsonObj.has("pages")) {
                                    // This is likely old Resume format, not GridResume
                                    Log.d(TAG, "[NeonSync] [$operationId] Skipping row at index $i (id=$rowId): JSON is not GridResume format (no 'pages' field)")
                                    continue
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "[NeonSync] [$operationId] Failed to parse JSON structure at index $i (id=$rowId): ${e.message}")
                                continue
                            }
                            
                            try {
                                val gridResume = gson.fromJson(jsonField, GridResume::class.java)
                                if (gridResume != null && gridResume.pages.isNotEmpty()) {
                                    Log.d(TAG, "[NeonSync] [$operationId] Parsed GridResume at index $i: id=${gridResume.id}, thumbnail=${gridResume.thumbnail.take(50).ifEmpty { "(empty)" }}...")
                                    gridResumes.add(gridResume)
                                } else {
                                    Log.w(TAG, "[NeonSync] [$operationId] GridResume at index $i (id=$rowId) deserialized to null or empty pages. JSON preview: ${jsonField.take(100)}...")
                                }
                            } catch (e: Exception) {
                                // Skip entries that aren't valid GridResume format
                                Log.w(TAG, "[NeonSync] [$operationId] Failed to parse GridResume at index $i (id=$rowId): ${e.message}")
                            }
                        } else {
                            Log.d(TAG, "[NeonSync] [$operationId] Skipping row at index $i (id=$rowId): json field is blank")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "[NeonSync] [$operationId] Failed to process row at index $i", e)
                    }
                }

                Log.d(TAG, "[NeonSync] [$operationId] Successfully retrieved ${gridResumes.size} GridResumes")
                Result.success(gridResumes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[NeonSync] [$operationId] Error getting GridResumes for user", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a GridResume from Neon database (deletes the Resume entry)
     */
    suspend fun deleteGridResume(
        resumeId: String,
        authToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val operationId = "deleteGridResume-${resumeId.take(8)}"
        Log.d(TAG, "[NeonSync] [$operationId] Starting deletion")
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
                    Log.e(TAG, "[NeonSync] [$operationId] Failed to delete GridResume: ${response.code} - $bodyString")
                    return@withContext Result.failure(
                        IOException("Failed to delete GridResume (${response.code}): $bodyString")
                    )
                }
                Log.d(TAG, "[NeonSync] [$operationId] Successfully deleted GridResume")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[NeonSync] [$operationId] Error deleting GridResume", e)
            Result.failure(e)
        }
    }

    /**
     * Resolves authorization header from token or BuildConfig credentials
     */
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
