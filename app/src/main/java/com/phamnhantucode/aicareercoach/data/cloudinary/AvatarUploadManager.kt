package com.phamnhantucode.aicareercoach.data.cloudinary

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

// Manages avatar uploads
class AvatarUploadManager private constructor(private val context: Context) {

    private val cache = ImageUrlCache.getInstance(context)

    companion object {
        private const val TAG = "AvatarUploadManager"

        @Volatile
        private var INSTANCE: AvatarUploadManager? = null

        fun getInstance(context: Context): AvatarUploadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AvatarUploadManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // Upload state
    private val _uploadState = MutableStateFlow<ImageUploadState>(ImageUploadState.Idle)
    val uploadState: StateFlow<ImageUploadState> = _uploadState.asStateFlow()

    // Uploads avatar
    suspend fun uploadAvatar(localUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uriString = localUri.toString()

            // Check cache first
            val cachedUrl = cache.getCachedUrl(uriString)
            if (cachedUrl != null) {
                Log.d(TAG, "Using cached URL for: $uriString")
                _uploadState.value = ImageUploadState.Success(cachedUrl)
                return@withContext Result.success(cachedUrl)
            }

            // Not cached - upload to Cloudinary
            Log.d(TAG, "Uploading avatar to Cloudinary: $uriString")
            _uploadState.value = ImageUploadState.Uploading

            val uploadResult = CloudinaryUploadService.uploadImage(context, localUri)

            if (uploadResult.isSuccess) {
                val response = uploadResult.getOrNull()!!
                val remoteUrl = response.data.secureUrl

                // Cache the mapping
                cache.cacheUrl(uriString, remoteUrl)

                _uploadState.value = ImageUploadState.Success(remoteUrl)
                Log.d(TAG, "Successfully uploaded avatar: $remoteUrl")
                Result.success(remoteUrl)
            } else {
                val error = uploadResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Upload failed"
                Log.e(TAG, "Failed to upload avatar", error)
                _uploadState.value = ImageUploadState.Error(errorMessage, uriString)
                Result.failure(error ?: Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in uploadAvatar", e)
            _uploadState.value = ImageUploadState.Error(
                e.message ?: "Unknown error",
                localUri.toString()
            )
            Result.failure(e)
        }
    }

    // Retries upload
    suspend fun retryUpload(localUri: Uri): Result<String> {
        Log.d(TAG, "Retrying upload for: $localUri")
        // Reset state and re-upload (bypassing cache for retry)
        _uploadState.value = ImageUploadState.Idle
        return uploadAvatar(localUri)
    }

    // Get cached URL
    suspend fun getCachedUrl(localUri: Uri): String? {
        return cache.getCachedUrl(localUri.toString())
    }

    // Reset state
    fun resetState() {
        _uploadState.value = ImageUploadState.Idle
        Log.d(TAG, "Upload state reset to idle")
    }
}
