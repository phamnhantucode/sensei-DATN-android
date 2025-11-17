package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.*
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ElementPdfRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.GridCoordinateMapper
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfRenderContext

/**
 * Skill Element PDF Renderer
 *
 * Renders skills with various display styles
 */
class SkillElementPdfRenderer : ElementPdfRenderer<ResumeElement.SkillElement> {

    override suspend fun render(
        canvas: Canvas,
        element: ResumeElement.SkillElement,
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
            SkillDisplayStyle.LIST -> {
                renderListLayout(canvas, element, contentBounds, mapper, context)
            }
            SkillDisplayStyle.TAGS -> {
                renderTagsLayout(canvas, element, contentBounds, mapper, context)
            }
            SkillDisplayStyle.PROGRESS_BARS -> {
                renderProgressBarsLayout(canvas, element, contentBounds, mapper, context)
            }
            SkillDisplayStyle.DOTS -> {
                renderDotsLayout(canvas, element, contentBounds, mapper, context)
            }
            SkillDisplayStyle.GROUPED -> {
                renderGroupedLayout(canvas, element, contentBounds, mapper, context)
            }
        }

        canvas.restore()
    }

    /**
     * Render skills as a simple list
     */
    private fun renderListLayout(
        canvas: Canvas,
        element: ResumeElement.SkillElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val textPaint = createSkillTextPaint(element, mapper, context)
        
        // Get actual text height using font metrics (includes ascent + descent)
        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        
        // Grid editor uses padding(vertical = spacing/2) on each Row, meaning:
        // - Each item gets spacing/2 padding above and below
        // - Total vertical space per item = textHeight + spacing
        val spacingPerItem = mapper.borderWidthToPdfPoints(element.spacing)

        // Calculate total content height
        // Each item occupies (textHeight + spacing), minus spacing after last item
        val itemCount = element.items.count { it.name.isNotEmpty() }
        val totalHeight = if (itemCount > 0) {
            itemCount * (textHeight + spacingPerItem) - spacingPerItem
        } else {
            0f
        }

        // Apply vertical alignment
        // Note: In grid editor, each Row has padding(vertical = spacing/2), which adds spacing/2
        // above the first item and below the last item. We need to account for this.
        var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> spacingPerItem / 2  // Start with spacing/2 padding like grid editor
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight - spacingPerItem / 2  // End with spacing/2 padding
        }

        var itemIndex = 0
        element.items.forEach { item ->
            if (item.name.isNotEmpty()) {
                val bullet = if (element.showBullets) getBulletCharacter(element.bulletStyle) + " " else ""
                val text = bullet + item.name
                val textWidth = textPaint.measureText(text)

                // Apply horizontal alignment
                val xPosition = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                    HorizontalAlignment.START -> 0f
                    HorizontalAlignment.CENTER -> (bounds.width() - textWidth) / 2f
                    HorizontalAlignment.END -> bounds.width() - textWidth
                }

                // Draw text at baseline
                // currentY is the top of the text box, so we need to offset by -ascent to get to baseline
                canvas.drawText(text, xPosition, currentY - fontMetrics.ascent, textPaint)
                
                // Move to next item: advance by textHeight + spacing
                // This matches grid editor's Row with padding(vertical = spacing/2)
                currentY += textHeight + spacingPerItem
                itemIndex++
            }
        }
    }

    /**
     * Render skills as tags/chips
     */
    private fun renderTagsLayout(
        canvas: Canvas,
        element: ResumeElement.SkillElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val textPaint = createSkillTextPaint(element, mapper, context)
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val tagPadding = mapper.borderWidthToPdfPoints(12f)
        val tagRadius = mapper.borderWidthToPdfPoints(element.tagCornerRadius)
        val rowHeight = textPaint.textSize + tagPadding * 2

        val tagBackgroundPaint = Paint().apply {
            color = element.tagBackgroundColor?.toInt() ?: 0xFFE3F2FD.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // First pass: group tags into rows and calculate dimensions
        data class TagInfo(val name: String, val width: Float)
        val rows = mutableListOf<MutableList<TagInfo>>()
        var currentRow = mutableListOf<TagInfo>()
        var currentRowWidth = 0f

        element.items.forEach { item ->
            if (item.name.isEmpty()) return@forEach
            val textWidth = textPaint.measureText(item.name)
            val tagWidth = textWidth + tagPadding * 2

            // Check if we need to start a new row
            if (currentRowWidth + tagWidth > bounds.width() && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0f
            }

            currentRow.add(TagInfo(item.name, tagWidth))
            currentRowWidth += tagWidth + if (currentRow.size > 1) spacing else 0f
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val totalHeight = (rowHeight * rows.size) + (spacing * (rows.size - 1))

        // Apply vertical alignment
        val startY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        // Second pass: render tags with per-row horizontal alignment
        var currentY = startY

        rows.forEach { row ->
            // Calculate row width
            val rowWidth = row.sumOf { it.width.toDouble() }.toFloat() + (spacing * (row.size - 1))

            // Apply horizontal alignment for this row
            var currentX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                HorizontalAlignment.START -> 0f
                HorizontalAlignment.CENTER -> (bounds.width() - rowWidth) / 2f
                HorizontalAlignment.END -> bounds.width() - rowWidth
            }

            // Render tags in this row
            row.forEach { tag ->
                // Draw tag background
                val tagRect = RectF(currentX, currentY, currentX + tag.width, currentY + rowHeight)
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

                // Draw text
                canvas.drawText(
                    tag.name,
                    currentX + tagPadding,
                    currentY + tagPadding + textPaint.textSize,
                    textPaint
                )

                currentX += tag.width + spacing
            }

            currentY += rowHeight + spacing
        }
    }

    /**
     * Render skills with progress bars
     */
    private fun renderProgressBarsLayout(
        canvas: Canvas,
        element: ResumeElement.SkillElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val textPaint = createSkillTextPaint(element, mapper, context)
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val barHeight = mapper.borderWidthToPdfPoints(element.progressBarHeight)
        val barRadius = mapper.borderWidthToPdfPoints(element.progressBarCornerRadius)

        // Calculate maximum text width for consistent alignment
        val maxTextWidth = element.items
            .filter { it.name.isNotEmpty() }
            .maxOfOrNull { textPaint.measureText(it.name) } ?: 0f

        // Use max text width or available width (whichever is smaller) for bar width
        val barWidth = minOf(maxTextWidth, bounds.width())

        // Calculate total height for vertical alignment
        val itemCount = element.items.count { it.name.isNotEmpty() }
        val totalHeight = itemCount * (textPaint.textSize + 4f + barHeight + spacing) - spacing

        // Apply vertical alignment
        var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        // Apply horizontal alignment for the entire column (text + bars together)
        val columnStartX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (bounds.width() - barWidth) / 2f
            HorizontalAlignment.END -> bounds.width() - barWidth
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

        element.items.forEach { item ->
            if (item.name.isEmpty()) return@forEach

            // Draw skill name (left-aligned within the column)
            canvas.drawText(item.name, columnStartX, currentY + textPaint.textSize, textPaint)
            currentY += textPaint.textSize + 4f

            // Draw progress bar background (same width as text column)
            val barRect = RectF(columnStartX, currentY, columnStartX + barWidth, currentY + barHeight)
            canvas.drawRoundRect(barRect, barRadius, barRadius, backgroundPaint)

            // Draw progress bar fill
            val proficiency = item.proficiency?.coerceIn(0f, 1f) ?: 0.5f
            val fillWidth = barWidth * proficiency
            if (fillWidth > 0) {
                val fillRect = RectF(columnStartX, currentY, columnStartX + fillWidth, currentY + barHeight)
                canvas.drawRoundRect(fillRect, barRadius, barRadius, progressPaint)
            }

            currentY += barHeight + spacing
        }
    }

    /**
     * Render skills with dot-based proficiency
     */
    private fun renderDotsLayout(
        canvas: Canvas,
        element: ResumeElement.SkillElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val textPaint = createSkillTextPaint(element, mapper, context)
        val spacingPerItem = mapper.borderWidthToPdfPoints(element.spacing)
        val dotSize = mapper.borderWidthToPdfPoints(element.dotSize)
        val dotSpacing = mapper.borderWidthToPdfPoints(4f)
        
        // Get actual text height using font metrics
        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent

        // Calculate total height for vertical alignment
        // Each item occupies textHeight + spacing, minus spacing after last item
        val itemCount = element.items.count { it.name.isNotEmpty() }
        val totalHeight = itemCount * (textHeight + spacingPerItem) - spacingPerItem

        // Apply vertical alignment
        // Note: Grid editor uses Arrangement.spacedBy(spacing), which adds spacing between items
        // but NOT before first or after last item, so no adjustment needed for DOTS layout
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
            if (item.name.isEmpty()) return@forEach

            val textWidth = textPaint.measureText(item.name)
            val dotsWidth = (dotSize * element.maxDots) + (dotSpacing * (element.maxDots - 1))
            val textDotSpacing = mapper.borderWidthToPdfPoints(8f) // Space between text and dots
            val totalItemWidth = textWidth + textDotSpacing + dotsWidth

            // Apply horizontal alignment to entire item (text + dots together)
            val itemStartX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                HorizontalAlignment.START -> 0f
                HorizontalAlignment.CENTER -> (bounds.width() - totalItemWidth) / 2f
                HorizontalAlignment.END -> bounds.width() - totalItemWidth
            }

            // Draw skill name
            canvas.drawText(item.name, itemStartX, currentY - fontMetrics.ascent, textPaint)

            // Draw dots (positioned after text, vertically centered with text)
            var dotX = itemStartX + textWidth + textDotSpacing
            val proficiency = item.proficiency?.coerceIn(0f, 1f) ?: 0.5f
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

            // Move to next item
            currentY += textHeight + spacingPerItem
        }
    }

    /**
     * Render skills grouped by category
     */
    private fun renderGroupedLayout(
        canvas: Canvas,
        element: ResumeElement.SkillElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val skillTextPaint = createSkillTextPaint(element, mapper, context)
        val categoryTextPaint = createCategoryTextPaint(element, mapper, context)
        val spacingPerItem = mapper.borderWidthToPdfPoints(element.spacing)
        val groupSpacing = mapper.borderWidthToPdfPoints(element.groupSpacing)
        
        // Get actual text heights using font metrics
        val skillFontMetrics = skillTextPaint.fontMetrics
        val skillTextHeight = skillFontMetrics.descent - skillFontMetrics.ascent
        val categoryFontMetrics = categoryTextPaint.fontMetrics
        val categoryTextHeight = categoryFontMetrics.descent - categoryFontMetrics.ascent

        // Calculate total height for vertical alignment
        val groups = element.items.groupBy { it.category }
        var totalHeight = 0f
        groups.entries.forEachIndexed { groupIndex, entry ->
            val category = entry.key
            val skills = entry.value
            if (category.isNotEmpty()) {
                totalHeight += categoryTextHeight + 4f
            }
            val skillCount = skills.count { it.name.isNotEmpty() }
            if (skillCount > 0) {
                // Each skill item occupies textHeight + spacing, minus spacing after last item in group
                totalHeight += skillCount * (skillTextHeight + spacingPerItem) - spacingPerItem
            }
            if (groupIndex < groups.size - 1) {
                totalHeight += groupSpacing
            }
        }

        // Apply vertical alignment
        var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        groups.forEach { (category, skills) ->
            // Draw category header
            if (category.isNotEmpty()) {
                val categoryWidth = categoryTextPaint.measureText(category)
                val xPosition = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                    HorizontalAlignment.START -> 0f
                    HorizontalAlignment.CENTER -> (bounds.width() - categoryWidth) / 2f
                    HorizontalAlignment.END -> bounds.width() - categoryWidth
                }
                canvas.drawText(category, xPosition, currentY - categoryFontMetrics.ascent, categoryTextPaint)
                currentY += categoryTextHeight + 4f
            }

            // Draw skills in this category
            val skillsInCategory = skills.filter { it.name.isNotEmpty() }
            
            // Add spacing/2 before first skill (matching Row's vertical padding in grid editor)
            if (skillsInCategory.isNotEmpty()) {
                currentY += spacingPerItem / 2
            }
            
            skillsInCategory.forEachIndexed { skillIndex, item ->
                val bullet = if (element.showBullets) getBulletCharacter(element.bulletStyle) + " " else ""
                val text = bullet + item.name
                val textWidth = skillTextPaint.measureText(text)

                val xPosition = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                    HorizontalAlignment.START -> 0f
                    HorizontalAlignment.CENTER -> (bounds.width() - textWidth) / 2f
                    HorizontalAlignment.END -> bounds.width() - textWidth
                }

                canvas.drawText(text, xPosition, currentY - skillFontMetrics.ascent, skillTextPaint)
                
                // Move to next item: advance by textHeight + spacing
                currentY += skillTextHeight + spacingPerItem
            }
            
            // Subtract spacing/2 after last skill to match grid editor
            if (skillsInCategory.isNotEmpty()) {
                currentY -= spacingPerItem / 2
            }

            currentY += groupSpacing // Add group spacing after this group
        }
    }

    /**
     * Create text paint for skill text
     */
    private fun createSkillTextPaint(
        element: ResumeElement.SkillElement,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): TextPaint {
        return createTextPaint(element.skillStyle, element.style.opacity, mapper, context)
    }

    /**
     * Create text paint for category text
     */
    private fun createCategoryTextPaint(
        element: ResumeElement.SkillElement,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): TextPaint {
        return createTextPaint(element.categoryStyle, element.style.opacity, mapper, context)
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

    /**
     * Get bullet character based on bullet style
     */
    private fun getBulletCharacter(bulletStyle: BulletStyle): String {
        return when (bulletStyle) {
            BulletStyle.DISC -> "•"
            BulletStyle.DASH -> "-"
            BulletStyle.ARROW -> "→"
            BulletStyle.CHEVRON -> "›"
            BulletStyle.NUMBERED -> "1."
            BulletStyle.CUSTOM_ICON -> "•"
            BulletStyle.NONE -> ""
        }
    }
}
