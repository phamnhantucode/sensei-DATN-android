package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

/**
 * Image Cache Implementation using Coil
 *
 * Handles both local (file://) and remote (http://, https://) images
 */
class CoilImageCache(private val context: Context) : ImageCache {
    private val imageLoader = ImageLoader(context)
    private val cache = mutableMapOf<String, Bitmap?>()

    override suspend fun getImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        // Check cache first
        if (cache.containsKey(url)) {
            return@withContext cache[url]
        }

        try {
            val bitmap = when {
                // Handle content:// URIs (from Android photo picker)
                url.startsWith("content://") -> loadContentUri(url)
                // Handle file:// URIs
                url.startsWith("file://") -> loadLocalImage(url)
                // Handle HTTP/HTTPS URLs
                url.startsWith("http://") || url.startsWith("https://") -> loadRemoteImage(url)
                // Handle relative paths
                else -> loadLocalImage("file://$url")
            }

            // Cache the result (including null for failed loads)
            cache[url] = bitmap
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            cache[url] = null
            null
        }
    }

    private suspend fun loadContentUri(contentUri: String): Bitmap? {
        return try {
            val uri = Uri.parse(contentUri)

            // Use ImageDecoder for Android P+ or MediaStore for older versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun loadLocalImage(fileUri: String): Bitmap? {
        return try {
            val uri = URI.create(fileUri)
            val file = File(uri.path)

            if (!file.exists()) {
                return null
            }

            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun loadRemoteImage(url: String): Bitmap? {
        return try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // Need software bitmap for PDF
                .build()

            val result = imageLoader.execute(request)

            if (result is SuccessResult) {
                // Convert drawable to bitmap
                val drawable = result.drawable
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun clear() {
        cache.values.filterNotNull().forEach { it.recycle() }
        cache.clear()
    }
}
