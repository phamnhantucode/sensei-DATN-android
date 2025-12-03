package com.phamnhantucode.aicareercoach.data.imgbb

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Manages avatar image uploads with caching and state tracking
 * Orchestrates the flow between cache checking, upload, and state management
 */
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

    /**
     * Upload state flow for observing upload progress
     */
    private val _uploadState = MutableStateFlow<ImageUploadState>(ImageUploadState.Idle)
    val uploadState: StateFlow<ImageUploadState> = _uploadState.asStateFlow()

    /**
     * Uploads avatar image to imgbb
     * First checks cache, then uploads if not cached
     *
     * @param localUri Content URI from photo picker
     * @return Result with remote URL (either cached or newly uploaded)
     */
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

            // Not cached - upload to imgbb
            Log.d(TAG, "Uploading avatar to imgbb: $uriString")
            _uploadState.value = ImageUploadState.Uploading

            val uploadResult = ImgbbUploadService.uploadImage(context, localUri)

            if (uploadResult.isSuccess) {
                val response = uploadResult.getOrNull()!!
                val remoteUrl = response.data.displayUrl

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

    /**
     * Retries a failed upload
     * Used when user clicks retry button after error
     *
     * @param localUri Content URI from photo picker
     * @return Result with remote URL
     */
    suspend fun retryUpload(localUri: Uri): Result<String> {
        Log.d(TAG, "Retrying upload for: $localUri")
        // Reset state and re-upload (bypassing cache for retry)
        _uploadState.value = ImageUploadState.Idle
        return uploadAvatar(localUri)
    }

    /**
     * Gets cached URL without uploading
     *
     * @param localUri Content URI to check
     * @return Cached URL if exists, null otherwise
     */
    suspend fun getCachedUrl(localUri: Uri): String? {
        return cache.getCachedUrl(localUri.toString())
    }

    /**
     * Resets upload state to idle
     * Call when user navigates away or cancels upload
     */
    fun resetState() {
        _uploadState.value = ImageUploadState.Idle
        Log.d(TAG, "Upload state reset to idle")
    }
}
