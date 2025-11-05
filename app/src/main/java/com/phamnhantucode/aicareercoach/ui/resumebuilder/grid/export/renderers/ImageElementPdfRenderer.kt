package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers

import android.graphics.*
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ImageScale
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ElementPdfRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.GridCoordinateMapper
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportException
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfRenderContext

/**
 * Image Element PDF Renderer
 *
 * Renders images with proper scaling, cropping, and corner radius
 */
class ImageElementPdfRenderer : ElementPdfRenderer<ResumeElement.ImageElement> {

    override suspend fun render(
        canvas: Canvas,
        element: ResumeElement.ImageElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        if (element.imageUrl.isBlank()) return

        // Load image from cache
        val bitmap = context.imageCache.getImage(element.imageUrl)
            ?: run {
                // Draw placeholder if image failed to load
                drawPlaceholder(canvas, bounds, mapper, context)
                return
            }

        // Draw background and border
        drawElementStyle(canvas, element.style, bounds, mapper, context, element.cornerRadius)

        // Calculate image destination rect based on content scale
        val imageRect = calculateImageRect(bitmap, bounds, element.contentScale)

        // Draw image with optional corner radius
        if (element.cornerRadius > 0f) {
            drawRoundedImage(canvas, bitmap, imageRect, element.cornerRadius, mapper, element.style.opacity)
        } else {
            drawImage(canvas, bitmap, imageRect, element.style.opacity)
        }
    }

    /**
     * Calculate image destination rectangle based on content scale mode
     */
    private fun calculateImageRect(
        bitmap: Bitmap,
        bounds: RectF,
        contentScale: ImageScale
    ): RectF {
        val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val boundsAspectRatio = bounds.width() / bounds.height()

        return when (contentScale) {
            ImageScale.FIT -> {
                // Fit inside bounds, maintain aspect ratio
                if (imageAspectRatio > boundsAspectRatio) {
                    // Image is wider - fit to width
                    val scaledHeight = bounds.width() / imageAspectRatio
                    val yOffset = (bounds.height() - scaledHeight) / 2f
                    RectF(
                        bounds.left,
                        bounds.top + yOffset,
                        bounds.right,
                        bounds.top + yOffset + scaledHeight
                    )
                } else {
                    // Image is taller - fit to height
                    val scaledWidth = bounds.height() * imageAspectRatio
                    val xOffset = (bounds.width() - scaledWidth) / 2f
                    RectF(
                        bounds.left + xOffset,
                        bounds.top,
                        bounds.left + xOffset + scaledWidth,
                        bounds.bottom
                    )
                }
            }

            ImageScale.FILL -> {
                // Fill bounds, maintain aspect ratio, may crop
                if (imageAspectRatio > boundsAspectRatio) {
                    // Image is wider - fit to height, crop width
                    val scaledWidth = bounds.height() * imageAspectRatio
                    val xOffset = (bounds.width() - scaledWidth) / 2f
                    RectF(
                        bounds.left + xOffset,
                        bounds.top,
                        bounds.left + xOffset + scaledWidth,
                        bounds.bottom
                    )
                } else {
                    // Image is taller - fit to width, crop height
                    val scaledHeight = bounds.width() / imageAspectRatio
                    val yOffset = (bounds.height() - scaledHeight) / 2f
                    RectF(
                        bounds.left,
                        bounds.top + yOffset,
                        bounds.right,
                        bounds.top + yOffset + scaledHeight
                    )
                }
            }

            ImageScale.STRETCH -> {
                // Stretch to fill bounds, ignore aspect ratio
                RectF(bounds)
            }
        }
    }

    /**
     * Draw image without corner radius
     */
    private fun drawImage(
        canvas: Canvas,
        bitmap: Bitmap,
        destRect: RectF,
        opacity: Float
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
        }

        canvas.drawBitmap(
            bitmap,
            null, // source rect (null = entire bitmap)
            destRect,
            paint
        )
    }

    /**
     * Draw image with corner radius
     */
    private fun drawRoundedImage(
        canvas: Canvas,
        bitmap: Bitmap,
        destRect: RectF,
        cornerRadiusDp: Float,
        mapper: GridCoordinateMapper,
        opacity: Float
    ) {
        val radius = mapper.cornerRadiusToPdfPoints(cornerRadiusDp)

        val paint = Paint().apply {
            isAntiAlias = true
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
        }

        // Create rounded rectangle path
        val path = Path().apply {
            addRoundRect(destRect, radius, radius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(bitmap, null, destRect, paint)
        canvas.restore()
    }

    /**
     * Draw placeholder for failed images
     */
    private fun drawPlaceholder(
        canvas: Canvas,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#F0F0F0") // Light gray background
            style = Paint.Style.FILL
        }

        canvas.drawRect(bounds, paint)

        // Draw X mark
        paint.color = Color.parseColor("#CCCCCC")
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE

        canvas.drawLine(
            bounds.left + bounds.width() * 0.3f,
            bounds.top + bounds.height() * 0.3f,
            bounds.right - bounds.width() * 0.3f,
            bounds.bottom - bounds.height() * 0.3f,
            paint
        )

        canvas.drawLine(
            bounds.right - bounds.width() * 0.3f,
            bounds.top + bounds.height() * 0.3f,
            bounds.left + bounds.width() * 0.3f,
            bounds.bottom - bounds.height() * 0.3f,
            paint
        )
    }

    /**
     * Draw element background, border, and shadow
     */
    private fun drawElementStyle(
        canvas: Canvas,
        style: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementStyle,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext,
        cornerRadius: Float
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
        }

        val radius = mapper.cornerRadiusToPdfPoints(cornerRadius.coerceAtLeast(style.borderRadius))

        // Draw shadow
        if (style.shadowColor != null && style.shadowBlur > 0f) {
            val shadowPaint = Paint(paint).apply {
                color = context.colorConverter.toIntColorWithOpacity(
                    style.shadowColor,
                    style.opacity
                )
                setShadowLayer(
                    style.shadowBlur,
                    style.shadowOffsetX,
                    style.shadowOffsetY,
                    context.colorConverter.toIntColor(style.shadowColor)
                )
            }

            val shadowBounds = RectF(bounds)
            shadowBounds.offset(style.shadowOffsetX, style.shadowOffsetY)

            if (radius > 0f) {
                canvas.drawRoundRect(shadowBounds, radius, radius, shadowPaint)
            } else {
                canvas.drawRect(shadowBounds, shadowPaint)
            }
        }

        // Draw background
        if (style.backgroundColor != null && !context.colorConverter.isTransparent(style.backgroundColor)) {
            paint.color = context.colorConverter.toIntColorWithOpacity(
                style.backgroundColor,
                style.opacity
            )
            paint.style = Paint.Style.FILL

            if (radius > 0f) {
                canvas.drawRoundRect(bounds, radius, radius, paint)
            } else {
                canvas.drawRect(bounds, paint)
            }
        }

        // Draw border
        if (style.borderColor != null && style.borderWidth > 0f) {
            paint.color = context.colorConverter.toIntColorWithOpacity(
                style.borderColor,
                style.opacity
            )
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = mapper.borderWidthToPdfPoints(style.borderWidth)

            if (radius > 0f) {
                canvas.drawRoundRect(bounds, radius, radius, paint)
            } else {
                canvas.drawRect(bounds, paint)
            }
        }
    }
}
