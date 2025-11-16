package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.Base64
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ColorConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Generates small thumbnail previews of resume designs
 * Used for displaying design previews in the Resume Design screen
 *
 * This generator uses full-quality PNG rendering and scales it down to thumbnail size
 * for much better quality compared to simplified element rendering.
 */
class ThumbnailGenerator(private val context: Context) {

    private val colorConverter = ColorConverter()
    private val pageRenderer = ResumePageRenderer(context)

    // Thumbnail dimensions (keep aspect ratio of A4: 210x297mm or roughly 1:1.41)
    private val thumbnailWidth = 300 // pixels
    private val thumbnailHeight = 420 // pixels

    // Full-size render dimensions (A4 at 72 DPI)
    private val fullWidth = 595 // points
    private val fullHeight = 842 // points
    
    /**
     * Generates a thumbnail for a GridResume and returns it as a Base64 encoded string
     *
     * This method now uses a two-step process for superior quality:
     * 1. Render the full-quality first page at A4 resolution (595x842)
     * 2. Scale down to thumbnail size (300x420) using high-quality filtering
     *
     * @param gridResume The resume to generate thumbnail for
     * @return Base64 encoded PNG image string, or empty string if generation fails
     */
    suspend fun generateThumbnail(gridResume: GridResume): String = withContext(Dispatchers.Default) {
        try {
            val thumbnail = createThumbnailBitmap(gridResume)
            if (thumbnail == null) {
                android.util.Log.e("ThumbnailGenerator", "Failed to create thumbnail bitmap")
                return@withContext ""
            }

            val base64String = bitmapToBase64(thumbnail)
            thumbnail.recycle()
            base64String
        } catch (e: Exception) {
            android.util.Log.e("ThumbnailGenerator", "Failed to generate thumbnail", e)
            ""
        }
    }

    /**
     * Creates a bitmap thumbnail of the resume using full-quality rendering
     *
     * Process:
     * 1. Render first page at full A4 resolution using ResumePageRenderer
     * 2. Scale down to thumbnail size with high-quality bilinear filtering
     * 3. Recycle full-size bitmap to free memory
     *
     * @return Thumbnail bitmap, or null if rendering fails
     */
    private suspend fun createThumbnailBitmap(gridResume: GridResume): Bitmap? {
        // Step 1: Render full-quality page
        val fullSizeBitmap = pageRenderer.renderFirstPage(
            gridResume,
            width = fullWidth,
            height = fullHeight
        ) ?: return createFallbackThumbnail(gridResume)

        try {
            // Step 2: Scale down to thumbnail size with high quality
            val thumbnail = scaleBitmapHighQuality(
                fullSizeBitmap,
                thumbnailWidth,
                thumbnailHeight
            )

            // Step 3: Clean up full-size bitmap
            fullSizeBitmap.recycle()

            return thumbnail
        } catch (e: Exception) {
            android.util.Log.e("ThumbnailGenerator", "Failed to scale bitmap", e)
            fullSizeBitmap.recycle()
            return createFallbackThumbnail(gridResume)
        }
    }

    /**
     * Scales a bitmap to target dimensions using high-quality bilinear filtering
     */
    private fun scaleBitmapHighQuality(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    /**
     * Creates a fallback thumbnail using simplified rendering if full rendering fails
     * This maintains backward compatibility with the old approach
     */
    private fun createFallbackThumbnail(gridResume: GridResume): Bitmap {
        val bitmap = Bitmap.createBitmap(thumbnailWidth, thumbnailHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Get first page
        val page = gridResume.pages.firstOrNull() ?: return bitmap

        // Draw background
        val bgColor = try {
            colorConverter.toIntColor(page.backgroundColor)
        } catch (e: Exception) {
            Color.WHITE
        }
        canvas.drawColor(bgColor)

        // Calculate scale factor
        val scaleX = thumbnailWidth / fullWidth.toFloat()
        val scaleY = thumbnailHeight / fullHeight.toFloat()
        val scale = minOf(scaleX, scaleY)

        // Calculate grid cell size
        val gridConfig = gridResume.gridConfig
        val cellWidth = (fullWidth / gridConfig.columns) * scale
        val cellHeight = (fullHeight / gridConfig.rows) * scale

        // Draw simplified elements
        val sortedElements = page.elements.sortedBy { it.zIndex }
        for (element in sortedElements) {
            drawElementThumbnail(canvas, element, cellWidth, cellHeight, scale)
        }

        return bitmap
    }
    
    /**
     * Draws a simplified representation of an element on the thumbnail canvas
     */
    private fun drawElementThumbnail(
        canvas: Canvas,
        element: ResumeElement,
        cellWidth: Float,
        cellHeight: Float,
        scale: Float
    ) {
        val position = element.position
        
        // Calculate element bounds in thumbnail coordinates
        val left = position.col * cellWidth
        val top = position.row * cellHeight
        val right = left + (position.colSpan * cellWidth)
        val bottom = top + (position.rowSpan * cellHeight)
        
        val rect = RectF(left, top, right, bottom)
        
        when (element) {
            is ResumeElement.TextElement -> {
                drawTextElementThumbnail(canvas, element, rect, scale)
            }
            is ResumeElement.ImageElement -> {
                drawImageElementThumbnail(canvas, rect)
            }
            is ResumeElement.ShapeElement -> {
                drawShapeElementThumbnail(canvas, element, rect)
            }
            is ResumeElement.ContactElement -> {
                drawContactElementThumbnail(canvas, rect)
            }
            is ResumeElement.WorkExperienceElement -> {
                drawWorkExperienceElementThumbnail(canvas, rect)
            }
            is ResumeElement.EducationElement -> {
                drawEducationElementThumbnail(canvas, rect)
            }
            is ResumeElement.SkillElement -> {
                drawSkillElementThumbnail(canvas, rect)
            }
            is ResumeElement.ProjectElement -> {
                drawProjectElementThumbnail(canvas, rect)
            }
            is ResumeElement.CertificationElement -> {
                drawCertificationElementThumbnail(canvas, rect)
            }
            is ResumeElement.LanguageElement -> {
                drawLanguageElementThumbnail(canvas, rect)
            }
            is ResumeElement.IconElement -> {
                drawIconElementThumbnail(canvas, rect)
            }
            is ResumeElement.ChartElement -> {
                drawChartElementThumbnail(canvas, rect)
            }
            is ResumeElement.ContainerElement -> {
                drawContainerElementThumbnail(canvas, element, rect)
            }
        }
    }
    
    private fun drawTextElementThumbnail(canvas: Canvas, element: ResumeElement.TextElement, rect: RectF, scale: Float) {
        val paint = Paint().apply {
            color = colorConverter.toIntColor(element.textStyle.color)
            textSize = element.textStyle.fontSize * scale * 0.5f // Scale down text for thumbnail
            isAntiAlias = true
        }
        
        // Draw simplified text (just a line to indicate text presence)
        val text = if (element.content.length > 20) "${element.content.take(20)}..." else element.content
        canvas.drawText(text, rect.left + 2, rect.top + paint.textSize + 2, paint)
    }
    
    private fun drawImageElementThumbnail(canvas: Canvas, rect: RectF) {
        val paint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.FILL
        }
        canvas.drawRect(rect, paint)
        
        // Draw border
        paint.style = Paint.Style.STROKE
        paint.color = Color.GRAY
        paint.strokeWidth = 1f
        canvas.drawRect(rect, paint)
    }
    
    private fun drawShapeElementThumbnail(canvas: Canvas, element: ResumeElement.ShapeElement, rect: RectF) {
        val paint = Paint().apply {
            color = colorConverter.toIntColor(element.style.backgroundColor ?: 0xFFE0E0E0)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        when (element.shapeType) {
            ShapeType.RECTANGLE, ShapeType.DIVIDER, ShapeType.LINE -> {
                canvas.drawRect(rect, paint)
            }
            ShapeType.CIRCLE -> {
                canvas.drawOval(rect, paint)
            }
        }
    }
    
    private fun drawContactElementThumbnail(canvas: Canvas, rect: RectF) {
        drawGenericElementBox(canvas, rect, Color.parseColor("#E3F2FD"))
    }
    
    private fun drawWorkExperienceElementThumbnail(canvas: Canvas, rect: RectF) {
        drawGenericElementBox(canvas, rect, Color.parseColor("#FFF3E0"))
    }
    
    private fun drawEducationElementThumbnail(canvas: Canvas, rect: RectF) {
        drawGenericElementBox(canvas, rect, Color.parseColor("#F3E5F5"))
    }
    
    private fun drawSkillElementThumbnail(canvas: Canvas, rect: RectF) {
        drawGenericElementBox(canvas, rect, Color.parseColor("#E8F5E9"))
    }
    
    private fun drawProjectElementThumbnail(canvas: Canvas, rect: RectF) {
        drawGenericElementBox(canvas, rect, Color.parseColor("#FFF9C4"))
    }
    
    private fun drawCertificationElementThumbnail(canvas: Canvas, rect: RectF) {
        drawGenericElementBox(canvas, rect, Color.parseColor("#FFE0B2"))
    }
    
    private fun drawLanguageElementThumbnail(canvas: Canvas, rect: RectF) {
        drawGenericElementBox(canvas, rect, Color.parseColor("#F1F8E9"))
    }
    
    private fun drawIconElementThumbnail(canvas: Canvas, rect: RectF) {
        val paint = Paint().apply {
            color = Color.DKGRAY
            style = Paint.Style.FILL
        }
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        val radius = minOf(rect.width(), rect.height()) / 4
        canvas.drawCircle(centerX, centerY, radius, paint)
    }
    
    private fun drawChartElementThumbnail(canvas: Canvas, rect: RectF) {
        drawGenericElementBox(canvas, rect, Color.parseColor("#E1F5FE"))
    }
    
    private fun drawContainerElementThumbnail(canvas: Canvas, element: ResumeElement.ContainerElement, rect: RectF) {
        val paint = Paint().apply {
            color = colorConverter.toIntColor(element.style.backgroundColor ?: 0xFFF5F5F5)
            style = Paint.Style.FILL
        }
        canvas.drawRect(rect, paint)
        
        // Draw border if exists
        element.style.borderColor?.let { borderColor ->
            paint.style = Paint.Style.STROKE
            paint.color = colorConverter.toIntColor(borderColor)
            paint.strokeWidth = 1f
            canvas.drawRect(rect, paint)
        }
    }
    
    /**
     * Helper to draw a generic colored box for complex elements
     */
    private fun drawGenericElementBox(canvas: Canvas, rect: RectF, color: Int) {
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(rect, paint)
        
        // Draw subtle border
        paint.style = Paint.Style.STROKE
        paint.color = Color.GRAY
        paint.strokeWidth = 0.5f
        canvas.drawRect(rect, paint)
    }
    
    /**
     * Converts a Bitmap to Base64 encoded PNG string
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
