package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ElementPdfRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.GridCoordinateMapper
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfRenderContext

/**
 * Container Element PDF Renderer
 * Renders the container frame (background, border) and handles clipping
 */
class ContainerElementPdfRenderer(
    private val renderChildren: suspend (Canvas, ResumeElement.ContainerElement) -> Unit
) : ElementPdfRenderer<ResumeElement.ContainerElement> {

    override suspend fun render(
        canvas: Canvas,
        element: ResumeElement.ContainerElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Draw shadow
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
            val shadowBounds = RectF(bounds)
            shadowBounds.offset(element.style.shadowOffsetX, element.style.shadowOffsetY)
            
            val radius = mapper.cornerRadiusToPdfPoints(element.style.borderRadius)
            if (radius > 0f) {
                canvas.drawRoundRect(shadowBounds, radius, radius, shadowPaint)
            } else {
                canvas.drawRect(shadowBounds, shadowPaint)
            }
        }

        // Draw background
        if (element.style.backgroundColor != null && 
            !context.colorConverter.isTransparent(element.style.backgroundColor)) {
            paint.style = Paint.Style.FILL
            paint.color = context.colorConverter.toIntColorWithOpacity(
                element.style.backgroundColor,
                element.style.opacity
            )
            
            val radius = mapper.cornerRadiusToPdfPoints(element.style.borderRadius)
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
            
            val radius = mapper.cornerRadiusToPdfPoints(element.style.borderRadius)
            if (radius > 0f) {
                canvas.drawRoundRect(bounds, radius, radius, paint)
            } else {
                canvas.drawRect(bounds, paint)
            }
        }

        // Handle clipping and children rendering
        if (element.clipContent) {
            canvas.save()
            val radius = mapper.cornerRadiusToPdfPoints(element.style.borderRadius)
            if (radius > 0f) {
                // Clip to rounded rect is complex in standard Canvas without Path
                // But simple clipRect is safe for now, or use Path
                val path = android.graphics.Path()
                path.addRoundRect(bounds, radius, radius, android.graphics.Path.Direction.CW)
                canvas.clipPath(path)
            } else {
                canvas.clipRect(bounds)
            }
        } else {
            canvas.save()
        }

        // Translate canvas to container's top-left for child rendering
        // Children coordinates are relative to the container
        canvas.translate(bounds.left, bounds.top)
        
        // Render children via callback
        renderChildren(canvas, element)
        
        canvas.restore()
    }
}
