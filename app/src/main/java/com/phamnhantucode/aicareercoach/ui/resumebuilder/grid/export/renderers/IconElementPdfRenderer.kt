package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.IconType
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ElementPdfRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.GridCoordinateMapper
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfRenderContext

/**
 * Icon Element PDF Renderer
 *
 * Renders icons and emojis
 * Note: Material icons are challenging to render in PDF without font files
 * For Phase 1, we'll render emoji only. Phase 2 can add Material icon font support.
 */
class IconElementPdfRenderer : ElementPdfRenderer<ResumeElement.IconElement> {

    override suspend fun render(
        canvas: Canvas,
        element: ResumeElement.IconElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        if (element.iconName.isBlank()) return

        when (element.iconType) {
            IconType.EMOJI -> drawEmoji(canvas, element, bounds, mapper, context)
            IconType.MATERIAL -> drawMaterialIcon(canvas, element, bounds, mapper, context)
        }
    }

    /**
     * Draw emoji icon
     */
    private fun drawEmoji(
        canvas: Canvas,
        element: ResumeElement.IconElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT

            // Scale font size to fit bounds
            textSize = bounds.height() * 0.7f // 70% of bounds height

            // Apply element color if background color is set (treat it as icon color)
            if (element.style.backgroundColor != null) {
                color = context.colorConverter.toIntColorWithOpacity(
                    element.style.backgroundColor,
                    element.style.opacity
                )
            } else {
                color = android.graphics.Color.BLACK
            }
        }

        // Calculate text baseline
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        val textOffset = (textHeight / 2) - fontMetrics.descent

        canvas.drawText(
            element.iconName,
            bounds.centerX(),
            bounds.centerY() + textOffset,
            paint
        )
    }

    /**
     * Draw Material icon
     *
     * Phase 1: Fallback to emoji/unicode or placeholder
     * Phase 2: Load Material Icons font and render properly
     */
    private fun drawMaterialIcon(
        canvas: Canvas,
        element: ResumeElement.IconElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        // Material icon mapping (limited set for Phase 1)
        val iconUnicode = getMaterialIconUnicode(element.iconName)

        if (iconUnicode != null) {
            // Render as unicode character
            val paint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT
                textSize = bounds.height() * 0.7f

                if (element.style.backgroundColor != null) {
                    color = context.colorConverter.toIntColorWithOpacity(
                        element.style.backgroundColor,
                        element.style.opacity
                    )
                } else {
                    color = android.graphics.Color.BLACK
                }
            }

            val fontMetrics = paint.fontMetrics
            val textHeight = fontMetrics.descent - fontMetrics.ascent
            val textOffset = (textHeight / 2) - fontMetrics.descent

            canvas.drawText(
                iconUnicode,
                bounds.centerX(),
                bounds.centerY() + textOffset,
                paint
            )
        } else {
            // Fallback: Draw placeholder circle
            drawIconPlaceholder(canvas, bounds, context)
        }
    }

    /**
     * Map common Material icon names to Unicode equivalents
     * This is a limited set for Phase 1
     */
    private fun getMaterialIconUnicode(iconName: String): String? {
        return when (iconName.lowercase()) {
            "email", "mail" -> "✉"
            "phone" -> "☎"
            "location", "place" -> "📍"
            "link" -> "🔗"
            "web", "public" -> "🌐"
            "work", "business" -> "💼"
            "school", "education" -> "🎓"
            "star" -> "⭐"
            "check", "done" -> "✓"
            "close", "clear" -> "✕"
            "arrow_forward", "arrow_right" -> "→"
            "arrow_back", "arrow_left" -> "←"
            "arrow_up" -> "↑"
            "arrow_down" -> "↓"
            "info" -> "ℹ"
            "warning" -> "⚠"
            "error" -> "⚠"
            "home" -> "🏠"
            "person", "account" -> "👤"
            "language" -> "🌍"
            "calendar" -> "📅"
            "time", "schedule" -> "⏰"
            else -> null // No mapping found
        }
    }

    /**
     * Draw placeholder for unsupported icons
     */
    private fun drawIconPlaceholder(
        canvas: Canvas,
        bounds: RectF,
        context: PdfRenderContext
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        val radius = minOf(bounds.width(), bounds.height()) / 2f * 0.6f
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), radius, paint)
    }
}
