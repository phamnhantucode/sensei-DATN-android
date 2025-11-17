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

        // No content padding - render directly to bounds
        val contentBounds = RectF(bounds)

        // Create text paint
        val textPaint = createTextPaint(element, mapper, context)

        // Create layout for text with padded width
        val layout = createTextLayout(
            element.content,
            textPaint,
            contentBounds.width().toInt(),
            element.alignment,
            element.maxLines,
            element.textStyle
        )

        // Calculate vertical alignment within content bounds
        // Account for StaticLayout's internal top padding (from font metrics)
        val topPadding = layout.getLineTop(0).toFloat()
        val textHeight = layout.height.toFloat() - topPadding
        val availableHeight = contentBounds.height()
        val yOffset = when (element.verticalAlignment ?: VerticalTextAlignment.CENTER) {
            VerticalTextAlignment.TOP -> -topPadding  // Remove top padding for proper alignment
            VerticalTextAlignment.CENTER -> (availableHeight - textHeight) / 2f - topPadding
            VerticalTextAlignment.BOTTOM -> availableHeight - textHeight - topPadding
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
            textSize = mapper.fontSizeToPdfPoints(element.textStyle.fontSize)
            color = context.colorConverter.toIntColorWithOpacity(
                element.textStyle.color,
                element.style.opacity
            )

            // Use Poppins font with proper weight mapping
            typeface = com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager.getPoppinsTypeface(
                context.context,
                element.textStyle.fontWeight,
                element.textStyle.isItalic
            )

            // Underline
            isUnderlineText = element.textStyle.isUnderlined

            // Letter spacing (match Compose - no conversion needed, already in proper units)
            letterSpacing = element.textStyle.letterSpacing
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
        maxLines: Int?,
        textStyle: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextStyle
    ): StaticLayout {
        val layoutAlignment = when (alignment) {
            TextAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
            TextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            TextAlignment.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL // Note: full justify not supported in StaticLayout
        }

        // Calculate line spacing multiplier from lineHeight
        // If lineHeight is specified, convert it to a multiplier relative to fontSize
        // Otherwise use default 1.0f (no extra spacing - match Compose default)
        val lineSpacingMultiplier = textStyle.lineHeight?.let {
            it / textStyle.fontSize
        } ?: 1.0f

        val builder = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(layoutAlignment)
            .setLineSpacing(0f, lineSpacingMultiplier)
            .setIncludePad(false) // Match Compose behavior - no extra padding
            .setMaxLines(maxLines ?: Int.MAX_VALUE)
        
        // For justify alignment, use inter-word justification (API 23+)
        if (alignment == TextAlignment.JUSTIFY && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            builder.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD)
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // For non-justify text, explicitly set no justification to match Compose
            builder.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_NONE)
        }
        
        return builder.build()
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
