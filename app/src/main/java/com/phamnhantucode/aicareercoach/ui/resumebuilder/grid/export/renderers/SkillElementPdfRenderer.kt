package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager
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

        // Use element's padding or default to 8dp for backward compatibility
        val paddingDp = element.padding ?: Padding(8f, 8f, 8f, 8f)
        val paddingLeft = mapper.borderWidthToPdfPoints(paddingDp.left)
        val paddingRight = mapper.borderWidthToPdfPoints(paddingDp.right)
        val paddingTop = mapper.borderWidthToPdfPoints(paddingDp.top)
        val paddingBottom = mapper.borderWidthToPdfPoints(paddingDp.bottom)

        val contentBounds = RectF(
            bounds.left + paddingLeft,
            bounds.top + paddingTop,
            bounds.right - paddingRight,
            bounds.bottom - paddingBottom
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
        val spacingPerItem = mapper.borderWidthToPdfPoints(element.spacing)

        // Calculate total content height using StaticLayout
        val itemCount = element.items.count { it.name.isNotEmpty() }
        var totalHeight = 0f
        val layouts = mutableListOf<Pair<String, android.text.StaticLayout>>()
        
        element.items.forEach { item ->
            if (item.name.isNotEmpty()) {
                val bullet = if (element.showBullets) getBulletCharacter(element.bulletStyle) + " " else ""
                val text = bullet + item.name
                
                val layout = android.text.StaticLayout.Builder
                    .obtain(text, 0, text.length, textPaint, bounds.width().toInt().coerceAtLeast(1))
                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1f)
                    .setIncludePad(false)
                    .build()
                
                layouts.add(text to layout)
                totalHeight += layout.height.toFloat()
            }
        }
        
        if (itemCount > 1) {
            totalHeight += spacingPerItem * (itemCount - 1)
        }

        // Apply vertical alignment
        var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        layouts.forEach { (text, layout) ->
            val textWidth = textPaint.measureText(text)

            // Apply horizontal alignment
            val xPosition = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                HorizontalAlignment.START -> 0f
                HorizontalAlignment.CENTER -> (bounds.width() - textWidth) / 2f
                HorizontalAlignment.END -> bounds.width() - textWidth
            }

            canvas.save()
            canvas.translate(xPosition, currentY)
            layout.draw(canvas)
            canvas.restore()
            
            currentY += layout.height.toFloat() + spacingPerItem
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
        val textPaint = createTextPaint(element.skillStyle.copy(color = element.tagTextColor), element.style.opacity, mapper, context)
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

        // Calculate total height using StaticLayout
        val itemCount = element.items.count { it.name.isNotEmpty() }
        var totalHeight = 0f
        val layouts = mutableListOf<Pair<SkillItem, android.text.StaticLayout>>()
        
        element.items.forEach { item ->
            if (item.name.isNotEmpty()) {
                val layout = android.text.StaticLayout.Builder
                    .obtain(item.name, 0, item.name.length, textPaint, bounds.width().toInt().coerceAtLeast(1))
                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1f)
                    .setIncludePad(false)
                    .build()
                
                layouts.add(item to layout)
                totalHeight += layout.height.toFloat()
            }
        }
        
        if (itemCount > 1) {
            totalHeight += spacingPerItem * (itemCount - 1)
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

        layouts.forEach { (item, layout) ->
            val textWidth = textPaint.measureText(item.name)
            val dotsWidth = (dotSize * element.maxDots) + (dotSpacing * (element.maxDots - 1))
            val textDotSpacing = mapper.borderWidthToPdfPoints(8f)
            val totalItemWidth = textWidth + textDotSpacing + dotsWidth
            val itemHeight = layout.height.toFloat()

            // Apply horizontal alignment to entire item (text + dots together)
            val itemStartX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                HorizontalAlignment.START -> 0f
                HorizontalAlignment.CENTER -> (bounds.width() - totalItemWidth) / 2f
                HorizontalAlignment.END -> bounds.width() - totalItemWidth
            }

            // Draw skill name
            canvas.save()
            canvas.translate(itemStartX, currentY)
            layout.draw(canvas)
            canvas.restore()

            // Draw dots (positioned after text, vertically centered with text)
            var dotX = itemStartX + textWidth + textDotSpacing
            val proficiency = item.proficiency?.coerceIn(0f, 1f) ?: 0.5f
            val filledDots = (proficiency * element.maxDots).toInt()

            repeat(element.maxDots) { index ->
                val paint = if (index < filledDots) filledPaint else emptyPaint
                canvas.drawCircle(
                    dotX + dotSize / 2,
                    currentY + itemHeight / 2,
                    dotSize / 2,
                    paint
                )
                dotX += dotSize + dotSpacing
            }

            currentY += itemHeight + spacingPerItem
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

        // Calculate total height using StaticLayout
        val groups = element.items.groupBy { it.category }
        var totalHeight = 0f
        val groupLayouts = mutableListOf<Triple<String, android.text.StaticLayout?, List<Pair<String, android.text.StaticLayout>>>>()
        
        groups.entries.forEachIndexed { groupIndex, entry ->
            val category = entry.key
            val skills = entry.value
            
            var categoryLayout: android.text.StaticLayout? = null
            if (category.isNotEmpty()) {
                categoryLayout = android.text.StaticLayout.Builder
                    .obtain(category, 0, category.length, categoryTextPaint, bounds.width().toInt().coerceAtLeast(1))
                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1f)
                    .setIncludePad(false)
                    .build()
                totalHeight += categoryLayout.height.toFloat() + 4f
            }
            
            val skillLayouts = mutableListOf<Pair<String, android.text.StaticLayout>>()
            skills.forEach { item ->
                if (item.name.isNotEmpty()) {
                    val bullet = if (element.showBullets) getBulletCharacter(element.bulletStyle) + " " else ""
                    val text = bullet + item.name
                    
                    val layout = android.text.StaticLayout.Builder
                        .obtain(text, 0, text.length, skillTextPaint, bounds.width().toInt().coerceAtLeast(1))
                        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1f)
                        .setIncludePad(false)
                        .build()
                    
                    skillLayouts.add(text to layout)
                    totalHeight += layout.height.toFloat()
                }
            }
            
            if (skillLayouts.size > 1) {
                totalHeight += spacingPerItem * (skillLayouts.size - 1)
            }
            
            if (groupIndex < groups.size - 1) {
                totalHeight += groupSpacing
            }
            
            groupLayouts.add(Triple(category, categoryLayout, skillLayouts))
        }

        // Apply vertical alignment
        var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        groupLayouts.forEach { (category, categoryLayout, skillLayouts) ->
            // Draw category header
            if (categoryLayout != null) {
                val categoryWidth = categoryTextPaint.measureText(category)
                val xPosition = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                    HorizontalAlignment.START -> 0f
                    HorizontalAlignment.CENTER -> (bounds.width() - categoryWidth) / 2f
                    HorizontalAlignment.END -> bounds.width() - categoryWidth
                }
                
                canvas.save()
                canvas.translate(xPosition, currentY)
                categoryLayout.draw(canvas)
                canvas.restore()
                
                currentY += categoryLayout.height.toFloat() + 4f
            }

            // Draw skills in this category
            skillLayouts.forEach { (text, layout) ->
                val textWidth = skillTextPaint.measureText(text)

                val xPosition = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                    HorizontalAlignment.START -> 0f
                    HorizontalAlignment.CENTER -> (bounds.width() - textWidth) / 2f
                    HorizontalAlignment.END -> bounds.width() - textWidth
                }

                canvas.save()
                canvas.translate(xPosition, currentY)
                layout.draw(canvas)
                canvas.restore()
                
                currentY += layout.height.toFloat() + spacingPerItem
            }

            currentY += groupSpacing
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
