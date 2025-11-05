package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ShapeType
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ElementPdfRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.GridCoordinateMapper
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfRenderContext

/**
 * Shape Element PDF Renderer
 *
 * Renders rectangles, circles, lines, and dividers
 */
class ShapeElementPdfRenderer : ElementPdfRenderer<ResumeElement.ShapeElement> {

    override suspend fun render(
        canvas: Canvas,
        element: ResumeElement.ShapeElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        when (element.shapeType) {
            ShapeType.RECTANGLE -> drawRectangle(canvas, element, bounds, mapper, context)
            ShapeType.CIRCLE -> drawCircle(canvas, element, bounds, mapper, context)
            ShapeType.LINE -> drawLine(canvas, element, bounds, mapper, context)
            ShapeType.DIVIDER -> drawDivider(canvas, element, bounds, mapper, context)
        }
    }

    /**
     * Draw rectangle
     */
    private fun drawRectangle(
        canvas: Canvas,
        element: ResumeElement.ShapeElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val paint = createPaint(element, context)

        // Draw shadow
        drawShadow(canvas, bounds, element.style, element.cornerRadius, mapper, context)

        // Draw fill
        if (element.style.backgroundColor != null &&
            !context.colorConverter.isTransparent(element.style.backgroundColor)) {
            paint.style = Paint.Style.FILL
            paint.color = context.colorConverter.toIntColorWithOpacity(
                element.style.backgroundColor,
                element.style.opacity
            )

            val radius = mapper.cornerRadiusToPdfPoints(
                element.cornerRadius.coerceAtLeast(element.style.borderRadius)
            )
            if (radius > 0f) {
                canvas.drawRoundRect(bounds, radius, radius, paint)
            } else {
                canvas.drawRect(bounds, paint)
            }
        }

        // Draw border
        if (element.style.borderColor != null && element.style.borderWidth > 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = mapper.borderWidthToPdfPoints(element.style.borderWidth)
            paint.color = context.colorConverter.toIntColorWithOpacity(
                element.style.borderColor,
                element.style.opacity
            )

            val radius = mapper.cornerRadiusToPdfPoints(
                element.cornerRadius.coerceAtLeast(element.style.borderRadius)
            )
            if (radius > 0f) {
                canvas.drawRoundRect(bounds, radius, radius, paint)
            } else {
                canvas.drawRect(bounds, paint)
            }
        }
    }

    /**
     * Draw circle
     */
    private fun drawCircle(
        canvas: Canvas,
        element: ResumeElement.ShapeElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val paint = createPaint(element, context)
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val radius = minOf(bounds.width(), bounds.height()) / 2f

        // Draw shadow (approximate with circle)
        if (element.style.shadowColor != null && element.style.shadowBlur > 0f) {
            val shadowPaint = Paint(paint).apply {
                color = context.colorConverter.toIntColorWithOpacity(
                    element.style.shadowColor,
                    element.style.opacity
                )
                setShadowLayer(
                    element.style.shadowBlur,
                    element.style.shadowOffsetX,
                    element.style.shadowOffsetY,
                    context.colorConverter.toIntColor(element.style.shadowColor)
                )
            }
            canvas.drawCircle(
                centerX + element.style.shadowOffsetX,
                centerY + element.style.shadowOffsetY,
                radius,
                shadowPaint
            )
        }

        // Draw fill
        if (element.style.backgroundColor != null &&
            !context.colorConverter.isTransparent(element.style.backgroundColor)) {
            paint.style = Paint.Style.FILL
            paint.color = context.colorConverter.toIntColorWithOpacity(
                element.style.backgroundColor,
                element.style.opacity
            )
            canvas.drawCircle(centerX, centerY, radius, paint)
        }

        // Draw border
        if (element.style.borderColor != null && element.style.borderWidth > 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = mapper.borderWidthToPdfPoints(element.style.borderWidth)
            paint.color = context.colorConverter.toIntColorWithOpacity(
                element.style.borderColor,
                element.style.opacity
            )
            canvas.drawCircle(centerX, centerY, radius - paint.strokeWidth / 2, paint)
        }
    }

    /**
     * Draw line
     */
    private fun drawLine(
        canvas: Canvas,
        element: ResumeElement.ShapeElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val paint = createPaint(element, context)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = mapper.borderWidthToPdfPoints(
            element.style.borderWidth.coerceAtLeast(1f)
        )

        // Use background color for line color (or border color as fallback)
        val lineColor = element.style.backgroundColor ?: element.style.borderColor
        if (lineColor != null) {
            paint.color = context.colorConverter.toIntColorWithOpacity(
                lineColor,
                element.style.opacity
            )
            canvas.drawLine(bounds.left, bounds.centerY(), bounds.right, bounds.centerY(), paint)
        }
    }

    /**
     * Draw divider (horizontal line, typically thin)
     */
    private fun drawDivider(
        canvas: Canvas,
        element: ResumeElement.ShapeElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val paint = createPaint(element, context)
        paint.style = Paint.Style.FILL

        // Use background color for divider
        val dividerColor = element.style.backgroundColor ?: element.style.borderColor
        if (dividerColor != null) {
            paint.color = context.colorConverter.toIntColorWithOpacity(
                dividerColor,
                element.style.opacity
            )

            // Draw as filled rectangle (supports corner radius)
            val radius = mapper.cornerRadiusToPdfPoints(element.cornerRadius)
            if (radius > 0f) {
                canvas.drawRoundRect(bounds, radius, radius, paint)
            } else {
                canvas.drawRect(bounds, paint)
            }
        }
    }

    /**
     * Draw shadow for shape
     */
    private fun drawShadow(
        canvas: Canvas,
        bounds: RectF,
        style: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementStyle,
        cornerRadius: Float,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        if (style.shadowColor == null || style.shadowBlur <= 0f) return

        val shadowPaint = Paint().apply {
            isAntiAlias = true
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

        val radius = mapper.cornerRadiusToPdfPoints(cornerRadius)
        if (radius > 0f) {
            canvas.drawRoundRect(shadowBounds, radius, radius, shadowPaint)
        } else {
            canvas.drawRect(shadowBounds, shadowPaint)
        }
    }

    /**
     * Create base paint
     */
    private fun createPaint(
        element: ResumeElement.ShapeElement,
        context: PdfRenderContext
    ): Paint {
        return Paint().apply {
            isAntiAlias = true
        }
    }
}
