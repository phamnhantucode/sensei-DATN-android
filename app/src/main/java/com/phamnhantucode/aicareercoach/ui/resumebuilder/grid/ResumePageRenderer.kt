package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ColorConverter
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.CoilImageCache
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.GridCoordinateMapper
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ImageCache
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportConfig
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfRenderContext
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ResumeCanvasRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers.*
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders resume pages to high-quality PNG bitmaps
 *
 * This class reuses the PDF rendering infrastructure to generate full-quality
 * bitmaps of resume pages, which can then be scaled down for thumbnails.
 *
 * Benefits over simplified rendering:
 * - Uses actual element renderers with proper fonts, colors, and styling
 * - Produces pixel-perfect representations of the resume
 * - Consistent with PDF export quality
 */
class ResumePageRenderer(private val context: Context) {

    private val imageCache: ImageCache = CoilImageCache(context)
    private val colorConverter = ColorConverter()

    /**
     * Renders the first page of a resume to a bitmap at A4 resolution
     *
     * @param gridResume The resume to render
     * @param width Target bitmap width (default: 595 points = A4 width at 72 DPI)
     * @param height Target bitmap height (default: 842 points = A4 height at 72 DPI)
     * @return High-quality bitmap of the first page, or null if rendering fails
     */
    suspend fun renderFirstPage(
        gridResume: GridResume,
        width: Int = 595,
        height: Int = 842
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            // Get first page
            val page = gridResume.pages.firstOrNull() ?: return@withContext null

            // Pre-load all images from the page
            preloadImages(page)

            // Create bitmap at full resolution
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Draw page background
            val bgColor = try {
                colorConverter.toIntColor(page.backgroundColor)
            } catch (e: Exception) {
                android.graphics.Color.WHITE
            }
            canvas.drawColor(bgColor)

            // Configure rendering
            val config = PdfExportConfig(
                pageWidth = width,
                pageHeight = height,
                dpi = 72
            )

            // Render all elements (sorted by z-index)
            val sortedElements = page.elements.sortedBy { it.zIndex }
            renderElements(canvas, sortedElements, gridResume.gridConfig, config)

            // Clean up
            imageCache.clear()

            bitmap
        } catch (e: Exception) {
            android.util.Log.e("ResumePageRenderer", "Failed to render page", e)
            imageCache.clear()
            null
        }
    }

    /**
     * Pre-load all images from a page
     */
    private suspend fun preloadImages(page: ResumePage) {
        page.elements.filterIsInstance<ResumeElement.ImageElement>().forEach { imageElement ->
            if (imageElement.imageUrl.isNotBlank()) {
                try {
                    imageCache.getImage(imageElement.imageUrl)
                } catch (e: Exception) {
                    android.util.Log.w("ResumePageRenderer", "Failed to preload image: ${imageElement.imageUrl}", e)
                }
            }
        }
    }

    /**
     * Render all elements on the canvas using the PDF renderers
     */
    /**
     * Render all elements on the canvas using the shared ResumeCanvasRenderer
     */
    private suspend fun renderElements(
        canvas: Canvas,
        elements: List<ResumeElement>,
        gridConfig: GridConfig,
        config: PdfExportConfig
    ) {
        val renderer = ResumeCanvasRenderer(context, config, imageCache, colorConverter)
        renderer.renderElements(canvas, elements, gridConfig)
    }
}
