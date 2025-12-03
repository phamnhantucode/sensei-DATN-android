package com.phamnhantucode.aicareercoach.data.imgbb

/**
 * Response from imgbb upload API
 */
data class ImgbbUploadResponse(
    val data: ImgbbImageData,
    val success: Boolean,
    val status: Int
)

/**
 * Image data returned from imgbb API
 */
data class ImgbbImageData(
    val id: String,
    val title: String,
    val url: String,
    val displayUrl: String,    // Display URL - use this for avatars
    val width: Int,
    val height: Int,
    val size: Int,
    val time: Long,
    val expiration: Long?,     // Null if no expiration
    val deleteUrl: String?     // URL to delete image (optional)
)

/**
 * Upload state for UI feedback
 */
sealed class ImageUploadState {
    data object Idle : ImageUploadState()
    data object Uploading : ImageUploadState()
    data class Success(val remoteUrl: String) : ImageUploadState()
    data class Error(val message: String, val localUri: String) : ImageUploadState()
}
