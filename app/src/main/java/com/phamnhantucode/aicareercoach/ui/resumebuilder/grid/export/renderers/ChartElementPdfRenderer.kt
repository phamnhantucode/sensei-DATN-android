package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ChartType
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ElementPdfRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.GridCoordinateMapper
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfRenderContext

/**
 * Chart Element PDF Renderer
 *
 * Renders skill bars, progress circles, and other chart types
 */
class ChartElementPdfRenderer : ElementPdfRenderer<ResumeElement.ChartElement> {

    override suspend fun render(
        canvas: Canvas,
        element: ResumeElement.ChartElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        when (element.chartType) {
            ChartType.HORIZONTAL_BAR -> drawHorizontalBar(canvas, element, bounds, mapper, context)
            ChartType.VERTICAL_BAR -> drawVerticalBar(canvas, element, bounds, mapper, context)
            ChartType.CIRCLE_PROGRESS -> drawCircleProgress(canvas, element, bounds, mapper, context)
            ChartType.DOT_METER -> drawDotMeter(canvas, element, bounds, mapper, context)
        }
    }

    /**
     * Draw horizontal skill bar
     */
    private fun drawHorizontalBar(
        canvas: Canvas,
        element: ResumeElement.ChartElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        // Use first item or default
        val item = element.data.items.firstOrNull() ?: return

        val percentage = item.value.coerceIn(0f, 1f) // Value is already 0-1

        val barHeight = bounds.height() * 0.6f // 60% of available height
        val barTop = bounds.centerY() - barHeight / 2

        // Draw background bar
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#E0E0E0")
            style = Paint.Style.FILL
        }

        val cornerRadius = mapper.cornerRadiusToPdfPoints(4f) // Small corner radius
        canvas.drawRoundRect(
            RectF(bounds.left, barTop, bounds.right, barTop + barHeight),
            cornerRadius,
            cornerRadius,
            bgPaint
        )

        // Draw progress bar
        if (percentage > 0) {
            val progressPaint = Paint().apply {
                isAntiAlias = true
                color = item.color?.let { context.colorConverter.toIntColor(it) }
                    ?: android.graphics.Color.parseColor("#2196F3")
                style = Paint.Style.FILL
            }

            val progressWidth = bounds.width() * percentage
            canvas.drawRoundRect(
                RectF(bounds.left, barTop, bounds.left + progressWidth, barTop + barHeight),
                cornerRadius,
                cornerRadius,
                progressPaint
            )
        }
    }

    /**
     * Draw vertical bar chart
     */
    private fun drawVerticalBar(
        canvas: Canvas,
        element: ResumeElement.ChartElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        // Use first item or default
        val item = element.data.items.firstOrNull() ?: return

        val percentage = item.value.coerceIn(0f, 1f)

        val barWidth = bounds.width() * 0.6f // 60% of available width
        val barLeft = bounds.centerX() - barWidth / 2

        // Draw background bar
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#E0E0E0")
            style = Paint.Style.FILL
        }

        val cornerRadius = mapper.cornerRadiusToPdfPoints(4f)
        canvas.drawRoundRect(
            RectF(barLeft, bounds.top, barLeft + barWidth, bounds.bottom),
            cornerRadius,
            cornerRadius,
            bgPaint
        )

        // Draw progress bar (from bottom to top)
        if (percentage > 0) {
            val progressPaint = Paint().apply {
                isAntiAlias = true
                color = item.color?.let { context.colorConverter.toIntColor(it) }
                    ?: android.graphics.Color.parseColor("#2196F3")
                style = Paint.Style.FILL
            }

            val progressHeight = bounds.height() * percentage
            canvas.drawRoundRect(
                RectF(barLeft, bounds.bottom - progressHeight, barLeft + barWidth, bounds.bottom),
                cornerRadius,
                cornerRadius,
                progressPaint
            )
        }
    }

    /**
     * Draw circular progress indicator
     */
    private fun drawCircleProgress(
        canvas: Canvas,
        element: ResumeElement.ChartElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        // Use first item or default
        val item = element.data.items.firstOrNull() ?: return

        val percentage = item.value.coerceIn(0f, 1f)

        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val radius = minOf(bounds.width(), bounds.height()) / 2f * 0.8f // 80% of available space
        val strokeWidth = radius * 0.2f // 20% of radius

        // Draw background circle
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
        }

        canvas.drawCircle(centerX, centerY, radius - strokeWidth / 2, bgPaint)

        // Draw progress arc
        if (percentage > 0) {
            val progressPaint = Paint().apply {
                isAntiAlias = true
                color = item.color?.let { context.colorConverter.toIntColor(it) }
                    ?: android.graphics.Color.parseColor("#2196F3")
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.ROUND
            }

            val arcBounds = RectF(
                centerX - radius + strokeWidth / 2,
                centerY - radius + strokeWidth / 2,
                centerX + radius - strokeWidth / 2,
                centerY + radius - strokeWidth / 2
            )

            // Start from top (-90 degrees) and sweep clockwise
            canvas.drawArc(
                arcBounds,
                -90f,
                360f * percentage,
                false,
                progressPaint
            )
        }
    }

    /**
     * Draw dot meter (skill rating with dots)
     */
    private fun drawDotMeter(
        canvas: Canvas,
        element: ResumeElement.ChartElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        // Use first item or default
        val item = element.data.items.firstOrNull() ?: return

        val totalDots = 5 // Standard 5-dot rating
        val filledDots = (item.value * totalDots).toInt().coerceIn(0, totalDots)

        val dotRadius = minOf(bounds.width(), bounds.height()) / (totalDots * 3f) // Space between dots
        val spacing = dotRadius * 2.5f
        val startX = bounds.centerX() - (totalDots - 1) * spacing / 2

        // Draw dots
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        for (i in 0 until totalDots) {
            val x = startX + i * spacing
            val y = bounds.centerY()

            paint.color = if (i < filledDots) {
                item.color?.let { context.colorConverter.toIntColor(it) }
                    ?: android.graphics.Color.parseColor("#2196F3")
            } else {
                android.graphics.Color.parseColor("#E0E0E0")
            }

            canvas.drawCircle(x, y, dotRadius, paint)
        }
    }
}
