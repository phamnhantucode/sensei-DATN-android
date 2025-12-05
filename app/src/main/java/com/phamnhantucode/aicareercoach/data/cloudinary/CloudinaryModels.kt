package com.phamnhantucode.aicareercoach.data.cloudinary

/**
 * Response from Cloudinary upload API
 */
data class CloudinaryUploadResponse(
    val data: CloudinaryImageData,
    val success: Boolean,
    val status: Int
)

/**
 * Image data returned from Cloudinary API
 */
data class CloudinaryImageData(
    val publicId: String,
    val url: String,
    val secureUrl: String,    // HTTPS URL - use this for images
    val width: Int,
    val height: Int,
    val bytes: Int,
    val format: String,
    val createdAt: String
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
