package com.phamnhantucode.aicareercoach.data.imgbb

import android.content.Context
import android.net.Uri
import android.util.Log
import com.phamnhantucode.aicareercoach.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Service for uploading images to imgbb API
 * Uses imgbb's public image hosting API for avatar uploads
 */
object ImgbbUploadService {

    private const val TAG = "ImgbbUploadService"
    private const val IMGBB_API_BASE = "https://api.imgbb.com/1"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Uploads an image to imgbb
     *
     * @param context Android context for reading content URI
     * @param imageUri Content URI from Android photo picker
     * @param fileName Optional filename for the uploaded image
     * @return Result with ImgbbUploadResponse on success
     */
    suspend fun uploadImage(
        context: Context,
        imageUri: Uri,
        fileName: String? = null
    ): Result<ImgbbUploadResponse> = withContext(Dispatchers.IO) {
        try {
            // Validate API key
            if (BuildConfig.IMGBB_API_KEY.isBlank()) {
                return@withContext Result.failure(
                    Exception("IMGBB_API_KEY not configured in local.properties")
                )
            }

            // Convert URI to File
            val imageFile = uriToFile(context, imageUri)
                ?: return@withContext Result.failure(
                    Exception("Failed to read image file")
                )

            // Build upload request
            val request = buildUploadRequest(imageFile, fileName)

            // Execute upload
            client.newCall(request).execute().use { response ->
                // Clean up temporary file
                imageFile.delete()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response"))

                    // Parse JSON response
                    val jsonResponse = JSONObject(responseBody)

                    if (!jsonResponse.optBoolean("success", false)) {
                        val errorMessage = jsonResponse.optJSONObject("error")
                            ?.optString("message", "Upload failed")
                            ?: "Upload failed"
                        return@withContext Result.failure(Exception(errorMessage))
                    }

                    val dataObj = jsonResponse.getJSONObject("data")
                    val imageData = ImgbbImageData(
                        id = dataObj.getString("id"),
                        title = dataObj.getString("title"),
                        url = dataObj.getString("url"),
                        displayUrl = dataObj.getString("display_url"),
                        width = dataObj.optInt("width", 0),
                        height = dataObj.optInt("height", 0),
                        size = dataObj.optInt("size", 0),
                        time = dataObj.optLong("time", 0),
                        expiration = dataObj.optLong("expiration", 0).takeIf { it > 0 },
                        deleteUrl = dataObj.optString("delete_url", null)
                    )

                    val uploadResponse = ImgbbUploadResponse(
                        data = imageData,
                        success = true,
                        status = response.code
                    )

                    Log.d(TAG, "Successfully uploaded image: ${imageData.displayUrl}")
                    Result.success(uploadResponse)
                } else {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Upload failed: ${response.code} - $errorBody")
                    Result.failure(
                        Exception("Upload failed: ${response.message}")
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            Result.failure(e)
        }
    }

    /**
     * Convert content:// URI to temporary File for upload
     * Pattern adapted from ClerkUserUpdateService
     */
    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Create temporary file with appropriate extension
            val extension = context.contentResolver.getType(uri)?.let {
                when (it) {
                    "image/jpeg" -> ".jpg"
                    "image/png" -> ".png"
                    "image/gif" -> ".gif"
                    "image/webp" -> ".webp"
                    else -> ".jpg"
                }
            } ?: ".jpg"

            val tempFile = File.createTempFile(
                "avatar_upload_",
                extension,
                context.cacheDir
            )

            // Copy URI contents to temp file
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()

            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to file", e)
            null
        }
    }

    /**
     * Build multipart upload request for imgbb API
     */
    private fun buildUploadRequest(file: File, fileName: String?): Request {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("key", BuildConfig.IMGBB_API_KEY)
            .addFormDataPart(
                "image",
                fileName ?: file.name,
                file.asRequestBody("image/*".toMediaType())
            )
            .apply {
                // Add filename if provided
                fileName?.let {
                    addFormDataPart("name", it)
                }
            }
            .build()

        return Request.Builder()
            .url("$IMGBB_API_BASE/upload")
            .post(requestBody)
            .build()
    }
}
