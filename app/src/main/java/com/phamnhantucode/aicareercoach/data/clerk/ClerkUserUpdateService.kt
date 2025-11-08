package com.phamnhantucode.aicareercoach.data.clerk

import android.content.Context
import android.net.Uri
import android.util.Log
import com.clerk.api.Clerk
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

/**
 * Service for updating Clerk user profile data via Clerk Backend API
 * Uses Clerk Secret Key for authentication (server-side operations)
 */
class ClerkUserUpdateService(private val context: Context) {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "ClerkUserUpdateService"
        private const val CLERK_API_BASE = "https://api.clerk.com/v1"
    }

    /**
     * Update user's first and last name
     *
     * @param firstName New first name
     * @param lastName New last name
     * @return Result with success/error message
     */
    suspend fun updateUserName(
        firstName: String,
        lastName: String
    ): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val userId = Clerk.user?.id
            if (userId == null) {
                return@withContext UpdateResult.Error("User not signed in")
            }

            // Build JSON payload
            val payload = JSONObject().apply {
                put("first_name", firstName)
                put("last_name", lastName)
            }

            // Make PATCH request to Clerk Backend API using secret key
            val request = Request.Builder()
                .url("$CLERK_API_BASE/users/$userId")
                .addHeader("Authorization", "Bearer ${BuildConfig.CLERK_SECRET_KEY}")
                .addHeader("Content-Type", "application/json")
                .patch(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully updated user name")
                    UpdateResult.Success
                } else {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Failed to update name: ${response.code} - $errorBody")
                    UpdateResult.Error("Failed to update name: ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user name", e)
            UpdateResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Update user's profile image
     *
     * @param imageUri URI of the image to upload
     * @return Result with success/error message
     */
    suspend fun updateProfileImage(imageUri: Uri): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val userId = Clerk.user?.id
            if (userId == null) {
                return@withContext UpdateResult.Error("User not signed in")
            }

            // Convert URI to File
            val imageFile = uriToFile(imageUri)
                ?: return@withContext UpdateResult.Error("Failed to read image file")

            // Build multipart request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    imageFile.name,
                    imageFile.asRequestBody("image/*".toMediaType())
                )
                .build()

            // Upload image to Clerk Backend API using secret key
            val uploadRequest = Request.Builder()
                .url("$CLERK_API_BASE/users/$userId/profile_image")
                .addHeader("Authorization", "Bearer ${BuildConfig.CLERK_SECRET_KEY}")
                .post(requestBody)
                .build()

            client.newCall(uploadRequest).execute().use { response ->
                // Clean up temporary file
                imageFile.delete()

                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully updated profile image")
                    UpdateResult.Success
                } else {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Failed to update image: ${response.code} - $errorBody")
                    UpdateResult.Error("Failed to update image: ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile image", e)
            UpdateResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Convert URI to temporary File for upload
     */
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Create temporary file
            val tempFile = File.createTempFile(
                "profile_image_",
                ".jpg",
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
     * Result sealed class for update operations
     */
    sealed class UpdateResult {
        data object Success : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }
}
