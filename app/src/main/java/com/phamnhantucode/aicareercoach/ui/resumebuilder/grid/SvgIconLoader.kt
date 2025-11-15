package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * SVG Icon Loader and Renderer
 * Loads SVG icons from assets/iconpack directory
 */
object SvgIconLoader {
    
    private val iconCache = mutableMapOf<String, SVG>()
    
    /**
     * Load SVG icon from assets
     */
    suspend fun loadSvg(context: Context, iconName: String): SVG? = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            if (iconCache.containsKey(iconName)) {
                return@withContext iconCache[iconName]
            }
            
            // Construct file path
            val fileName = getIconFileName(iconName)
            val inputStream: InputStream = context.assets.open("iconpack/$fileName")
            
            val svg = SVG.getFromInputStream(inputStream)
            iconCache[iconName] = svg
            inputStream.close()
            
            svg
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Load SVG synchronously (for PDF rendering)
     */
    fun loadSvgSync(context: Context, iconName: String): SVG? {
        try {
            // Check cache first
            if (iconCache.containsKey(iconName)) {
                return iconCache[iconName]
            }
            
            val fileName = getIconFileName(iconName)
            val inputStream: InputStream = context.assets.open("iconpack/$fileName")
            
            val svg = SVG.getFromInputStream(inputStream)
            iconCache[iconName] = svg
            inputStream.close()
            
            return svg
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Render SVG to bitmap
     */
    fun renderToBitmap(svg: SVG, width: Int, height: Int, color: Int? = null): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Set document size
        svg.documentWidth = width.toFloat()
        svg.documentHeight = height.toFloat()
        
        // Render SVG first
        svg.renderToCanvas(canvas)
        
        // Apply color tint if specified
        if (color != null) {
            val paint = Paint().apply {
                colorFilter = android.graphics.PorterDuffColorFilter(
                    color,
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            
            // Create a new bitmap with the tint applied
            val tintedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val tintCanvas = Canvas(tintedBitmap)
            tintCanvas.drawBitmap(bitmap, 0f, 0f, paint)
            return tintedBitmap
        }
        
        return bitmap
    }
    
    /**
     * Render SVG directly to canvas
     */
    fun renderToCanvas(svg: SVG, canvas: Canvas, width: Float, height: Float, color: Int? = null) {
        canvas.save()
        
        // Set document size
        svg.documentWidth = width
        svg.documentHeight = height
        
        if (color != null) {
            // Create a temporary bitmap to render the SVG
            val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
            val tempCanvas = Canvas(bitmap)
            svg.renderToCanvas(tempCanvas)
            
            // Apply color tint
            val paint = Paint().apply {
                colorFilter = android.graphics.PorterDuffColorFilter(
                    color,
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            bitmap.recycle()
        } else {
            svg.renderToCanvas(canvas)
        }
        
        canvas.restore()
    }
    
    /**
     * Map icon names to file names
     */
    private fun getIconFileName(iconName: String): String {
        return when (iconName.lowercase()) {
            "email", "mail" -> "ic_mail_1.svg"
            "phone", "call" -> "ic_phone_1.svg"
            "location", "place", "location_on", "address" -> "ic_address_1.svg"
            else -> {
                // Try direct filename
                if (iconName.endsWith(".svg")) iconName else "$iconName.svg"
            }
        }
    }
    
    /**
     * Get list of available icons
     */
    fun getAvailableIcons(context: Context): List<String> {
        return try {
            context.assets.list("iconpack")?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Clear cache
     */
    fun clearCache() {
        iconCache.clear()
    }
}
