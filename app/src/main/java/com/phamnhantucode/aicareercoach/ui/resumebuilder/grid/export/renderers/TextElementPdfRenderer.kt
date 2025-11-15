package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.text.font.FontWeight
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextAlignment
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.VerticalTextAlignment
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ElementPdfRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.GridCoordinateMapper
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfRenderContext

/**
 * Text Element PDF Renderer
 *
 * Renders text elements with styling, alignment, and line wrapping
 */
class TextElementPdfRenderer : ElementPdfRenderer<ResumeElement.TextElement> {

    override suspend fun render(
        canvas: Canvas,
        element: ResumeElement.TextElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        if (element.content.isBlank()) return

        // Draw background and borders
        drawElementStyle(canvas, element.style, bounds, mapper, context)

        // Apply 8dp content padding (matching canvas editor behavior)
        val contentPadding = mapper.borderWidthToPdfPoints(8f)
        val contentBounds = RectF(
            bounds.left + contentPadding,
            bounds.top + contentPadding,
            bounds.right - contentPadding,
            bounds.bottom - contentPadding
        )

        // Create text paint
        val textPaint = createTextPaint(element, mapper, context)

        // Create layout for text with padded width
        val layout = createTextLayout(
            element.content,
            textPaint,
            contentBounds.width().toInt(),
            element.alignment,
            element.maxLines
        )

        // Calculate vertical alignment within content bounds
        val textHeight = layout.height.toFloat()
        val availableHeight = contentBounds.height()
        val yOffset = when (element.verticalAlignment ?: VerticalTextAlignment.CENTER) {
            VerticalTextAlignment.TOP -> 0f
            VerticalTextAlignment.CENTER -> (availableHeight - textHeight) / 2f
            VerticalTextAlignment.BOTTOM -> availableHeight - textHeight
        }

        // Draw text within content bounds
        canvas.save()
        canvas.translate(contentBounds.left, contentBounds.top + yOffset)

        // Clip to content bounds
        canvas.clipRect(0f, 0f, contentBounds.width(), contentBounds.height())

        layout.draw(canvas)
        canvas.restore()
    }

    /**
     * Create text paint with styling
     */
    private fun createTextPaint(
        element: ResumeElement.TextElement,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): TextPaint {
        return TextPaint().apply {
            isAntiAlias = true
            textSize = mapper.spToPdfPoints(element.textStyle.fontSize)
            color = context.colorConverter.toIntColorWithOpacity(
                element.textStyle.color,
                element.style.opacity
            )

            // Font weight and style
            val typefaceStyle = when {
                element.textStyle.isBold && element.textStyle.isItalic -> Typeface.BOLD_ITALIC
                element.textStyle.isBold -> Typeface.BOLD
                element.textStyle.isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            typeface = Typeface.create(Typeface.DEFAULT, typefaceStyle)

            // Underline
            isUnderlineText = element.textStyle.isUnderlined

            // Letter spacing (in EM units)
            if (element.textStyle.letterSpacing != 0f) {
                letterSpacing = element.textStyle.letterSpacing / element.textStyle.fontSize
            }
        }
    }

    /**
     * Create text layout with proper alignment and wrapping
     */
    private fun createTextLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        alignment: TextAlignment,
        maxLines: Int?
    ): StaticLayout {
        val layoutAlignment = when (alignment) {
            TextAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
            TextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            TextAlignment.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL // Note: full justify not supported in StaticLayout
        }

        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(layoutAlignment)
            .setLineSpacing(0f, 1.15f) // 15% line height increase (default)
            .setIncludePad(false)
            .setMaxLines(maxLines ?: Int.MAX_VALUE)
            .build()
    }

    /**
     * Draw element background, border, and shadow
     */
    private fun drawElementStyle(
        canvas: Canvas,
        style: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementStyle,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
        }

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

            if (style.borderRadius > 0f) {
                val radius = mapper.cornerRadiusToPdfPoints(style.borderRadius)
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

            if (style.borderRadius > 0f) {
                val radius = mapper.cornerRadiusToPdfPoints(style.borderRadius)
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

            if (style.borderRadius > 0f) {
                val radius = mapper.cornerRadiusToPdfPoints(style.borderRadius)
                canvas.drawRoundRect(bounds, radius, radius, paint)
            } else {
                canvas.drawRect(bounds, paint)
            }
        }
    }
}
