package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export

import android.graphics.Canvas
import android.graphics.RectF
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumeElement

/**
 * Base interface for rendering resume elements to PDF canvas
 */
interface ElementPdfRenderer<T : ResumeElement> {
    /**
     * Render an element to the PDF canvas
     *
     * @param canvas PDF canvas to draw on
     * @param element The element to render
     * @param bounds PDF coordinates (already converted from grid)
     * @param mapper Coordinate mapper for additional conversions
     * @param context Rendering context for shared resources
     */
    suspend fun render(
        canvas: Canvas,
        element: T,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    )
}

/**
 * PDF Rendering Context
 * Contains shared resources and utilities for rendering
 */
data class PdfRenderContext(
    val context: android.content.Context,
    val config: PdfExportConfig,
    val imageCache: ImageCache,
    val colorConverter: ColorConverter
)

/**
 * Image Cache for downloaded/loaded images
 */
interface ImageCache {
    /**
     * Get image from cache or download it
     * @param url Image URL (file:// or http://)
     * @return Image bitmap or null if failed
     */
    suspend fun getImage(url: String): android.graphics.Bitmap?

    /**
     * Clear the cache
     */
    fun clear()
}

/**
 * Color Converter
 * Converts Compose Color (Long ARGB) to Android Graphics Color (Int)
 */
class ColorConverter {
    /**
     * Convert Long color (Compose) to Int color (Android Graphics)
     *
     * Compose stores color as Long: 0xAARRGGBB
     * Android Graphics uses Int: 0xAARRGGBB
     */
    fun toIntColor(colorLong: Long): Int {
        return colorLong.toInt()
    }

    /**
     * Convert Long color with opacity applied
     */
    fun toIntColorWithOpacity(colorLong: Long, opacity: Float): Int {
        val alpha = ((colorLong shr 24) and 0xFF)
        val red = ((colorLong shr 16) and 0xFF)
        val green = ((colorLong shr 8) and 0xFF)
        val blue = (colorLong and 0xFF)

        // Apply opacity to alpha channel
        val finalAlpha = (alpha * opacity).toInt().coerceIn(0, 255)

        return (finalAlpha shl 24) or (red.toInt() shl 16) or (green.toInt() shl 8) or blue.toInt()
    }

    /**
     * Check if color is transparent
     */
    fun isTransparent(colorLong: Long?): Boolean {
        if (colorLong == null) return true
        val alpha = ((colorLong shr 24) and 0xFF)
        return alpha == 0L
    }
}
