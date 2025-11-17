package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.*
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ElementPdfRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.GridCoordinateMapper
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfRenderContext

/**
 * Language Element PDF Renderer
 *
 * Renders languages with various display styles
 */
class LanguageElementPdfRenderer : ElementPdfRenderer<ResumeElement.LanguageElement> {

    override suspend fun render(
        canvas: Canvas,
        element: ResumeElement.LanguageElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        if (element.items.isEmpty()) return

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

        canvas.save()
        canvas.translate(contentBounds.left, contentBounds.top)
        canvas.clipRect(0f, 0f, contentBounds.width(), contentBounds.height())

        // Render based on display style
        when (element.displayStyle) {
            LanguageDisplayStyle.TEXT_LABELS -> {
                renderTextLabelsLayout(canvas, element, contentBounds, mapper, context)
            }
            LanguageDisplayStyle.PROGRESS_BARS -> {
                renderProgressBarsLayout(canvas, element, contentBounds, mapper, context)
            }
            LanguageDisplayStyle.DOTS -> {
                renderDotsLayout(canvas, element, contentBounds, mapper, context)
            }
            LanguageDisplayStyle.TAGS -> {
                renderTagsLayout(canvas, element, contentBounds, mapper, context)
            }
        }

        canvas.restore()
    }

    /**
     * Render languages as simple text labels
     */
    private fun renderTextLabelsLayout(
        canvas: Canvas,
        element: ResumeElement.LanguageElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val languageTextPaint = createLanguageTextPaint(element, mapper, context)
        val proficiencyTextPaint = createProficiencyTextPaint(element, mapper, context)
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        
        // Get actual text heights using font metrics
        val languageFontMetrics = languageTextPaint.fontMetrics
        val languageTextHeight = languageFontMetrics.descent - languageFontMetrics.ascent
        val proficiencyFontMetrics = proficiencyTextPaint.fontMetrics
        val proficiencyTextHeight = proficiencyFontMetrics.descent - proficiencyFontMetrics.ascent
        
        // Use the larger of the two text heights for row height
        val rowHeight = maxOf(languageTextHeight, proficiencyTextHeight)

        // Calculate total content height
        val itemCount = element.items.count { it.name.isNotEmpty() }
        val totalHeight = if (itemCount > 0) {
            itemCount * rowHeight + (itemCount - 1) * spacing
        } else {
            0f
        }

        // Apply vertical alignment
        var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        element.items.forEach { item ->
            if (item.name.isNotEmpty()) {
                // Measure text widths
                val languageWidth = languageTextPaint.measureText(item.name)
                val proficiencyText = getProficiencyText(item, element.proficiencyType)
                val proficiencyWidth = if (proficiencyText.isNotEmpty()) {
                    proficiencyTextPaint.measureText(proficiencyText)
                } else {
                    0f
                }

                // Calculate positions based on horizontal alignment
                val languageX: Float
                val proficiencyX: Float

                when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                    HorizontalAlignment.START -> {
                        languageX = 0f
                        proficiencyX = bounds.width() - proficiencyWidth
                    }
                    HorizontalAlignment.CENTER -> {
                        val totalWidth = languageWidth + 16f + proficiencyWidth // 16f spacing between
                        languageX = (bounds.width() - totalWidth) / 2f
                        proficiencyX = languageX + languageWidth + 16f
                    }
                    HorizontalAlignment.END -> {
                        proficiencyX = bounds.width() - proficiencyWidth
                        languageX = proficiencyX - 16f - languageWidth
                    }
                }

                // Draw language name (vertically centered in row)
                val languageY = currentY + (rowHeight - languageTextHeight) / 2f - languageFontMetrics.ascent
                canvas.drawText(item.name, languageX, languageY, languageTextPaint)

                // Draw proficiency label (vertically centered in row)
                if (proficiencyText.isNotEmpty()) {
                    val proficiencyY = currentY + (rowHeight - proficiencyTextHeight) / 2f - proficiencyFontMetrics.ascent
                    canvas.drawText(proficiencyText, proficiencyX, proficiencyY, proficiencyTextPaint)
                }

                currentY += rowHeight + spacing
            }
        }
    }

    /**
     * Render languages with progress bars
     */
    private fun renderProgressBarsLayout(
        canvas: Canvas,
        element: ResumeElement.LanguageElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val languageTextPaint = createLanguageTextPaint(element, mapper, context)
        val proficiencyTextPaint = createProficiencyTextPaint(element, mapper, context)
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val barHeight = mapper.borderWidthToPdfPoints(element.progressBarHeight)
        val barRadius = mapper.borderWidthToPdfPoints(element.progressBarCornerRadius)
        
        // Get actual text heights
        val languageFontMetrics = languageTextPaint.fontMetrics
        val languageTextHeight = languageFontMetrics.descent - languageFontMetrics.ascent
        val proficiencyFontMetrics = proficiencyTextPaint.fontMetrics
        val proficiencyTextHeight = proficiencyFontMetrics.descent - proficiencyFontMetrics.ascent
        val headerHeight = maxOf(languageTextHeight, proficiencyTextHeight)

        // Calculate total height for vertical alignment
        val itemCount = element.items.count { it.name.isNotEmpty() }
        val totalHeight = if (itemCount > 0) {
            itemCount * (headerHeight + 2f + barHeight) + (itemCount - 1) * spacing
        } else {
            0f
        }

        // Apply vertical alignment
        var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        val backgroundPaint = Paint().apply {
            color = element.progressBarBackgroundColor?.toInt() ?: 0xFFE0E0E0.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val progressPaint = Paint().apply {
            color = element.progressBarColor?.toInt() ?: 0xFF2196F3.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Apply horizontal alignment for the entire column
        val columnStartX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (bounds.width() - bounds.width()) / 2f // Use full width
            HorizontalAlignment.END -> 0f
        }

        element.items.forEach { item ->
            if (item.name.isNotEmpty()) {
                // Draw language name and proficiency label on same line
                val proficiencyText = getProficiencyText(item, element.proficiencyType)
                val languageWidth = languageTextPaint.measureText(item.name)
                val proficiencyWidth = if (proficiencyText.isNotEmpty()) {
                    proficiencyTextPaint.measureText(proficiencyText)
                } else {
                    0f
                }

                // Language name on left
                val languageY = currentY + (headerHeight - languageTextHeight) / 2f - languageFontMetrics.ascent
                canvas.drawText(item.name, columnStartX, languageY, languageTextPaint)

                // Proficiency label on right
                if (proficiencyText.isNotEmpty()) {
                    val proficiencyY = currentY + (headerHeight - proficiencyTextHeight) / 2f - proficiencyFontMetrics.ascent
                    canvas.drawText(proficiencyText, columnStartX + bounds.width() - proficiencyWidth, proficiencyY, proficiencyTextPaint)
                }

                currentY += headerHeight + 2f

                // Draw progress bar
                val barRect = RectF(columnStartX, currentY, columnStartX + bounds.width(), currentY + barHeight)
                canvas.drawRoundRect(barRect, barRadius, barRadius, backgroundPaint)

                // Draw progress fill
                val proficiency = item.proficiency.coerceIn(0f, 1f)
                val fillWidth = bounds.width() * proficiency
                if (fillWidth > 0) {
                    val fillRect = RectF(columnStartX, currentY, columnStartX + fillWidth, currentY + barHeight)
                    canvas.drawRoundRect(fillRect, barRadius, barRadius, progressPaint)
                }

                currentY += barHeight + spacing
            }
        }
    }

    /**
     * Render languages with dot indicators
     */
    private fun renderDotsLayout(
        canvas: Canvas,
        element: ResumeElement.LanguageElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val languageTextPaint = createLanguageTextPaint(element, mapper, context)
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val dotSize = mapper.borderWidthToPdfPoints(element.dotSize)
        val dotSpacing = mapper.borderWidthToPdfPoints(4f)
        
        // Get actual text height
        val fontMetrics = languageTextPaint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent

        // Calculate total height for vertical alignment
        val itemCount = element.items.count { it.name.isNotEmpty() }
        val totalHeight = if (itemCount > 0) {
            itemCount * textHeight + (itemCount - 1) * spacing
        } else {
            0f
        }

        // Apply vertical alignment
        var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        val filledPaint = Paint().apply {
            color = element.progressBarColor?.toInt() ?: 0xFF2196F3.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val emptyPaint = Paint().apply {
            color = element.progressBarBackgroundColor?.toInt() ?: 0xFFE0E0E0.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        element.items.forEach { item ->
            if (item.name.isNotEmpty()) {
                val languageWidth = languageTextPaint.measureText(item.name)
                val dotsWidth = (dotSize * element.maxDots) + (dotSpacing * (element.maxDots - 1))
                val textDotSpacing = mapper.borderWidthToPdfPoints(8f)
                val totalItemWidth = languageWidth + textDotSpacing + dotsWidth

                // Apply horizontal alignment to entire item
                val itemStartX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                    HorizontalAlignment.START -> 0f
                    HorizontalAlignment.CENTER -> (bounds.width() - totalItemWidth) / 2f
                    HorizontalAlignment.END -> bounds.width() - totalItemWidth
                }

                // Draw language name
                canvas.drawText(item.name, itemStartX, currentY - fontMetrics.ascent, languageTextPaint)

                // Draw dots
                var dotX = itemStartX + languageWidth + textDotSpacing
                val proficiency = item.proficiency.coerceIn(0f, 1f)
                val filledDots = (proficiency * element.maxDots).toInt()

                repeat(element.maxDots) { index ->
                    val paint = if (index < filledDots) filledPaint else emptyPaint
                    canvas.drawCircle(
                        dotX + dotSize / 2,
                        currentY + textHeight / 2,
                        dotSize / 2,
                        paint
                    )
                    dotX += dotSize + dotSpacing
                }

                currentY += textHeight + spacing
            }
        }
    }

    /**
     * Render languages as tags/chips
     */
    private fun renderTagsLayout(
        canvas: Canvas,
        element: ResumeElement.LanguageElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val languageTextPaint = createLanguageTextPaint(element, mapper, context)
        val proficiencyTextPaint = createProficiencyTextPaint(element, mapper, context)
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val tagPadding = mapper.borderWidthToPdfPoints(12f)
        val verticalPadding = mapper.borderWidthToPdfPoints(6f)
        val tagRadius = mapper.borderWidthToPdfPoints(element.tagCornerRadius)
        
        // Get text heights
        val languageFontMetrics = languageTextPaint.fontMetrics
        val languageTextHeight = languageFontMetrics.descent - languageFontMetrics.ascent
        val proficiencyFontMetrics = proficiencyTextPaint.fontMetrics
        val proficiencyTextHeight = proficiencyFontMetrics.descent - proficiencyFontMetrics.ascent

        val tagBackgroundPaint = Paint().apply {
            color = element.tagBackgroundColor?.toInt() ?: 0xFFE3F2FD.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // First pass: calculate tag dimensions and group into rows
        data class TagInfo(
            val name: String,
            val proficiencyText: String,
            val width: Float,
            val height: Float
        )
        
        val rows = mutableListOf<MutableList<TagInfo>>()
        var currentRow = mutableListOf<TagInfo>()
        var currentRowWidth = 0f

        element.items.forEach { item ->
            if (item.name.isEmpty()) return@forEach
            
            val proficiencyText = getProficiencyText(item, element.proficiencyType)
            val nameWidth = languageTextPaint.measureText(item.name)
            val proficiencyWidth = if (proficiencyText.isNotEmpty()) {
                proficiencyTextPaint.measureText(proficiencyText)
            } else {
                0f
            }
            
            val textWidth = maxOf(nameWidth, proficiencyWidth)
            val tagWidth = textWidth + tagPadding * 2
            val tagHeight = if (proficiencyText.isNotEmpty()) {
                languageTextHeight + 2f + proficiencyTextHeight + verticalPadding * 2
            } else {
                languageTextHeight + verticalPadding * 2
            }

            // Check if we need to start a new row
            if (currentRowWidth + tagWidth > bounds.width() && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0f
            }

            currentRow.add(TagInfo(item.name, proficiencyText, tagWidth, tagHeight))
            currentRowWidth += tagWidth + if (currentRow.size > 1) spacing else 0f
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        // Calculate total height
        val totalHeight = rows.sumOf { row -> 
            row.maxOfOrNull { it.height }?.toDouble() ?: 0.0 
        }.toFloat() + (rows.size - 1) * spacing / 2

        // Apply vertical alignment
        val startY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        // Second pass: render tags
        var currentY = startY

        rows.forEach { row ->
            val rowHeight = row.maxOfOrNull { it.height } ?: 0f
            val rowWidth = row.sumOf { it.width.toDouble() }.toFloat() + (spacing * (row.size - 1))

            // Apply horizontal alignment for this row
            var currentX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                HorizontalAlignment.START -> 0f
                HorizontalAlignment.CENTER -> (bounds.width() - rowWidth) / 2f
                HorizontalAlignment.END -> bounds.width() - rowWidth
            }

            row.forEach { tag ->
                // Draw tag background
                val tagRect = RectF(currentX, currentY, currentX + tag.width, currentY + tag.height)
                canvas.drawRoundRect(tagRect, tagRadius, tagRadius, tagBackgroundPaint)

                // Draw tag border if specified
                if (element.tagBorderColor != null && element.tagBorderWidth > 0) {
                    val borderPaint = Paint().apply {
                        color = element.tagBorderColor.toInt()
                        style = Paint.Style.STROKE
                        strokeWidth = mapper.borderWidthToPdfPoints(element.tagBorderWidth)
                        isAntiAlias = true
                    }
                    canvas.drawRoundRect(tagRect, tagRadius, tagRadius, borderPaint)
                }

                // Draw language name (centered horizontally in tag)
                val nameWidth = languageTextPaint.measureText(tag.name)
                val nameX = currentX + (tag.width - nameWidth) / 2f
                val nameY = currentY + verticalPadding - languageFontMetrics.ascent
                canvas.drawText(tag.name, nameX, nameY, languageTextPaint)

                // Draw proficiency text if present (centered horizontally in tag)
                if (tag.proficiencyText.isNotEmpty()) {
                    val proficiencyWidth = proficiencyTextPaint.measureText(tag.proficiencyText)
                    val proficiencyX = currentX + (tag.width - proficiencyWidth) / 2f
                    val proficiencyY = nameY + languageTextHeight + 2f
                    canvas.drawText(tag.proficiencyText, proficiencyX, proficiencyY, proficiencyTextPaint)
                }

                currentX += tag.width + spacing
            }

            currentY += rowHeight + spacing / 2
        }
    }

    /**
     * Get proficiency text based on proficiency type
     */
    private fun getProficiencyText(item: LanguageItem, proficiencyType: LanguageProficiencyType): String {
        return when (proficiencyType) {
            LanguageProficiencyType.TEXT -> item.proficiencyLabel
            LanguageProficiencyType.CEFR -> item.cefrLevel ?: item.proficiencyLabel
            LanguageProficiencyType.NUMERIC -> {
                val percentage = (item.proficiency * 100).toInt()
                "$percentage%"
            }
        }
    }

    /**
     * Create text paint for language text
     */
    private fun createLanguageTextPaint(
        element: ResumeElement.LanguageElement,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): TextPaint {
        return createTextPaint(element.languageStyle, element.style.opacity, mapper, context)
    }

    /**
     * Create text paint for proficiency text
     */
    private fun createProficiencyTextPaint(
        element: ResumeElement.LanguageElement,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): TextPaint {
        return createTextPaint(element.proficiencyLabelStyle, element.style.opacity, mapper, context)
    }

    /**
     * Create text paint with styling
     */
    private fun createTextPaint(
        textStyle: TextStyle,
        opacity: Float,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): TextPaint {
        return TextPaint().apply {
            isAntiAlias = true
            textSize = mapper.fontSizeToPdfPoints(textStyle.fontSize)
            color = context.colorConverter.toIntColorWithOpacity(textStyle.color, opacity)

            // Use Poppins font with proper weight mapping
            typeface = com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager.getPoppinsTypeface(
                context.context,
                textStyle.fontWeight,
                textStyle.isItalic
            )

            isUnderlineText = textStyle.isUnderlined

            if (textStyle.letterSpacing != 0f) {
                letterSpacing = textStyle.letterSpacing / textStyle.fontSize
            }
        }
    }

    /**
     * Draw element background, border, and shadow
     */
    private fun drawElementStyle(
        canvas: Canvas,
        style: ElementStyle,
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
