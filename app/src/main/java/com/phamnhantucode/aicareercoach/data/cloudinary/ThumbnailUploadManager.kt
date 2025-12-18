package com.phamnhantucode.aicareercoach.data.cloudinary

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

// Uploads thumbnails to Cloudinary
class ThumbnailUploadManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ThumbnailUploadManager"

        @Volatile
        private var INSTANCE: ThumbnailUploadManager? = null

        fun getInstance(context: Context): ThumbnailUploadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThumbnailUploadManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // Upload state flow
    private val _uploadState = MutableStateFlow<ThumbnailUploadState>(ThumbnailUploadState.Idle)
    val uploadState: StateFlow<ThumbnailUploadState> = _uploadState.asStateFlow()

    // Uploads Base64 thumbnail
    suspend fun uploadThumbnail(
        base64Thumbnail: String,
        resumeId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Skip if thumbnail is empty
            if (base64Thumbnail.isBlank()) {
                Log.d(TAG, "Empty thumbnail, skipping upload for resume: $resumeId")
                return@withContext Result.success("")
            }

            // Skip if already a URL (already uploaded)
            if (base64Thumbnail.startsWith("http://") || base64Thumbnail.startsWith("https://")) {
                Log.d(TAG, "Thumbnail already uploaded: $base64Thumbnail")
                return@withContext Result.success(base64Thumbnail)
            }

            Log.d(TAG, "Uploading thumbnail for resume: $resumeId (base64 length: ${base64Thumbnail.length})")
            _uploadState.value = ThumbnailUploadState.Uploading(resumeId)

            // Generate a unique filename (stable for this resume to overwrite previous versions)
            val fileName = "resume_thumb_$resumeId"

            // Upload to Cloudinary
            val uploadResult = CloudinaryUploadService.uploadBase64Image(base64Thumbnail, fileName)

            if (uploadResult.isSuccess) {
                val response = uploadResult.getOrNull()!!
                val thumbnailUrl = response.data.secureUrl

                _uploadState.value = ThumbnailUploadState.Success(thumbnailUrl)
                Log.d(TAG, "Successfully uploaded thumbnail: $thumbnailUrl")
                Result.success(thumbnailUrl)
            } else {
                val error = uploadResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Upload failed"
                Log.e(TAG, "Failed to upload thumbnail to Cloudinary: $errorMessage", error)
                _uploadState.value = ThumbnailUploadState.Error(errorMessage)
                
                // Return original Base64 as fallback so data isn't lost
                Log.d(TAG, "Falling back to base64 thumbnail (length: ${base64Thumbnail.length})")
                Result.success(base64Thumbnail)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading thumbnail", e)
            _uploadState.value = ThumbnailUploadState.Error(e.message ?: "Unknown error")
            
            // Return original Base64 as fallback
            Log.d(TAG, "Falling back to base64 thumbnail after exception (length: ${base64Thumbnail.length})")
            Result.success(base64Thumbnail)
        }
    }

    // Check if URL
    fun isUrl(thumbnail: String): Boolean {
        return thumbnail.startsWith("http://") || thumbnail.startsWith("https://")
    }

    // Reset state
    fun resetState() {
        _uploadState.value = ThumbnailUploadState.Idle
        Log.d(TAG, "Upload state reset to idle")
    }
}

// Thumbnail upload state
sealed class ThumbnailUploadState {
    data object Idle : ThumbnailUploadState()
    data class Uploading(val resumeId: String) : ThumbnailUploadState()
    data class Success(val thumbnailUrl: String) : ThumbnailUploadState()
    data class Error(val message: String) : ThumbnailUploadState()
}
