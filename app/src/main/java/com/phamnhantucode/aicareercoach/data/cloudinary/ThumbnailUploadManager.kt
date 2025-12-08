package com.phamnhantucode.aicareercoach.data.cloudinary

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Manages thumbnail image uploads to Cloudinary for resume designs.
 * Converts Base64 thumbnails to hosted URLs for faster loading in the resume list.
 * 
 * Benefits of using Cloudinary for thumbnails:
 * - Faster loading: CDN-hosted images load faster than Base64 decoding
 * - Smaller database: URLs (~100 chars) vs Base64 (~50KB+ per thumbnail)
 * - Better caching: Browser/app can cache URL-based images
 * - Image transformations: Cloudinary supports on-the-fly resizing
 */
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

    /**
     * Upload state flow for observing upload progress
     */
    private val _uploadState = MutableStateFlow<ThumbnailUploadState>(ThumbnailUploadState.Idle)
    val uploadState: StateFlow<ThumbnailUploadState> = _uploadState.asStateFlow()

    /**
     * Uploads a Base64 encoded thumbnail to Cloudinary
     *
     * @param base64Thumbnail Base64 encoded PNG image from ThumbnailGenerator
     * @param resumeId Resume ID to use for naming the uploaded file
     * @return Result with the Cloudinary URL on success, or the original Base64 on failure
     */
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

    /**
     * Checks if a thumbnail string is a URL or Base64 data
     *
     * @param thumbnail The thumbnail string to check
     * @return true if the thumbnail is a URL, false if Base64 or empty
     */
    fun isUrl(thumbnail: String): Boolean {
        return thumbnail.startsWith("http://") || thumbnail.startsWith("https://")
    }

    /**
     * Resets upload state to idle
     */
    fun resetState() {
        _uploadState.value = ThumbnailUploadState.Idle
        Log.d(TAG, "Upload state reset to idle")
    }
}

/**
 * Upload state for thumbnail uploads
 */
sealed class ThumbnailUploadState {
    data object Idle : ThumbnailUploadState()
    data class Uploading(val resumeId: String) : ThumbnailUploadState()
    data class Success(val thumbnailUrl: String) : ThumbnailUploadState()
    data class Error(val message: String) : ThumbnailUploadState()
}
