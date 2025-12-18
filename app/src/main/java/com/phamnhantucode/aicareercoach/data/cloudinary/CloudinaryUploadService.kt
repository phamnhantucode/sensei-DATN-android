package com.phamnhantucode.aicareercoach.data.cloudinary

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
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

// Uploads images to Cloudinary
object CloudinaryUploadService {

    private const val TAG = "CloudinaryUploadService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Uploads image
    suspend fun uploadImage(
        context: Context,
        imageUri: Uri,
        fileName: String? = null
    ): Result<CloudinaryUploadResponse> = withContext(Dispatchers.IO) {
        try {
            // Validate config
            if (BuildConfig.CLDNR_CLOUD_NAME.isBlank() || BuildConfig.CLDNR_UPLOAD_PRESET.isBlank()) {
                return@withContext Result.failure(
                    Exception("Cloudinary configuration not set in local.properties")
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

                    if (jsonResponse.has("error")) {
                        val errorMessage = jsonResponse.getJSONObject("error")
                            .optString("message", "Upload failed")
                        return@withContext Result.failure(Exception(errorMessage))
                    }

                    val imageData = CloudinaryImageData(
                        publicId = jsonResponse.getString("public_id"),
                        url = jsonResponse.getString("url"),
                        secureUrl = jsonResponse.getString("secure_url"),
                        width = jsonResponse.optInt("width", 0),
                        height = jsonResponse.optInt("height", 0),
                        bytes = jsonResponse.optInt("bytes", 0),
                        format = jsonResponse.optString("format", ""),
                        createdAt = jsonResponse.optString("created_at", "")
                    )

                    val uploadResponse = CloudinaryUploadResponse(
                        data = imageData,
                        success = true,
                        status = response.code
                    )

                    Log.d(TAG, "Successfully uploaded image: ${imageData.secureUrl}")
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

    // Convert URI to File
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

    // Uploads Base64 image
    suspend fun uploadBase64Image(
        base64Image: String,
        fileName: String? = null
    ): Result<CloudinaryUploadResponse> = withContext(Dispatchers.IO) {
        try {
            // Validate config
            if (BuildConfig.CLDNR_CLOUD_NAME.isBlank() || BuildConfig.CLDNR_UPLOAD_PRESET.isBlank()) {
                return@withContext Result.failure(
                    Exception("Cloudinary configuration not set in local.properties")
                )
            }

            // Validate base64 string
            if (base64Image.isBlank()) {
                return@withContext Result.failure(
                    Exception("Base64 image string is empty")
                )
            }

            // Build upload request with base64 data
            val request = buildBase64UploadRequest(base64Image, fileName)

            // Execute upload
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response"))

                    // Parse JSON response
                    val jsonResponse = JSONObject(responseBody)

                    if (jsonResponse.has("error")) {
                        val errorMessage = jsonResponse.getJSONObject("error")
                            .optString("message", "Upload failed")
                        return@withContext Result.failure(Exception(errorMessage))
                    }

                    val imageData = CloudinaryImageData(
                        publicId = jsonResponse.getString("public_id"),
                        url = jsonResponse.getString("url"),
                        secureUrl = jsonResponse.getString("secure_url"),
                        width = jsonResponse.optInt("width", 0),
                        height = jsonResponse.optInt("height", 0),
                        bytes = jsonResponse.optInt("bytes", 0),
                        format = jsonResponse.optString("format", ""),
                        createdAt = jsonResponse.optString("created_at", "")
                    )

                    val uploadResponse = CloudinaryUploadResponse(
                        data = imageData,
                        success = true,
                        status = response.code
                    )

                    Log.d(TAG, "Successfully uploaded base64 image: ${imageData.secureUrl}")
                    Result.success(uploadResponse)
                } else {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Base64 upload failed: ${response.code} - $errorBody")
                    Result.failure(
                        Exception("Upload failed: ${response.message}")
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading base64 image", e)
            Result.failure(e)
        }
    }

    // Build upload request
    private fun buildUploadRequest(file: File, fileName: String?): Request {
        val uploadUrl = "https://api.cloudinary.com/v1_1/${BuildConfig.CLDNR_CLOUD_NAME}/image/upload"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", BuildConfig.CLDNR_UPLOAD_PRESET)
            .addFormDataPart(
                "file",
                fileName ?: file.name,
                file.asRequestBody("image/*".toMediaType())
            )
            .apply {
                // Add public_id if provided
                fileName?.let {
                    addFormDataPart("public_id", it)
                }
            }
            .build()

        return Request.Builder()
            .url(uploadUrl)
            .post(requestBody)
            .build()
    }

    // Build base64 upload request
    private fun buildBase64UploadRequest(base64Image: String, fileName: String?): Request {
        val uploadUrl = "https://api.cloudinary.com/v1_1/${BuildConfig.CLDNR_CLOUD_NAME}/image/upload"

        // Cloudinary requires data URI format for base64
        val dataUri = if (base64Image.startsWith("data:")) {
            base64Image
        } else {
            "data:image/png;base64,$base64Image"
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", BuildConfig.CLDNR_UPLOAD_PRESET)
            .addFormDataPart("file", dataUri)
            .apply {
                // Add public_id if provided
                fileName?.let {
                    addFormDataPart("public_id", it)
                }
            }
            .build()

        return Request.Builder()
            .url(uploadUrl)
            .post(requestBody)
            .build()
    }
}
