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
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)

        // Calculate total content height
        val totalHeight = element.items.count { it.name.isNotEmpty() } * (textPaint.textSize + spacing) - spacing

        // Apply vertical alignment
        var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

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

                canvas.drawText(text, xPosition, currentY + textPaint.textSize, textPaint)
                currentY += textPaint.textSize + spacing
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

        // First pass: calculate total height needed for vertical alignment
        var simulateX = 0f
        var numRows = 1
        element.items.forEach { item ->
            if (item.name.isEmpty()) return@forEach
            val textWidth = textPaint.measureText(item.name)
            val tagWidth = textWidth + tagPadding * 2
            if (simulateX + tagWidth > bounds.width() && simulateX > 0) {
                simulateX = 0f
                numRows++
            }
            simulateX += tagWidth + spacing
        }
        val totalHeight = (rowHeight * numRows) + (spacing * (numRows - 1))

        // Apply vertical alignment
        val startY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        // Second pass: actually render tags
        var currentX = 0f
        var currentY = startY

        element.items.forEach { item ->
            if (item.name.isEmpty()) return@forEach

            val textWidth = textPaint.measureText(item.name)
            val tagWidth = textWidth + tagPadding * 2

            // Check if we need to wrap to next row
            if (currentX + tagWidth > bounds.width() && currentX > 0) {
                currentX = 0f
                currentY += rowHeight + spacing
            }

            // Draw tag background
            val tagRect = RectF(currentX, currentY, currentX + tagWidth, currentY + rowHeight)
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
                item.name,
                currentX + tagPadding,
                currentY + tagPadding + textPaint.textSize,
                textPaint
            )

            currentX += tagWidth + spacing
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

        // Calculate total height for vertical alignment
        val itemCount = element.items.count { it.name.isNotEmpty() }
        val totalHeight = itemCount * (textPaint.textSize + 4f + barHeight + spacing) - spacing

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

        element.items.forEach { item ->
            if (item.name.isEmpty()) return@forEach

            val textWidth = textPaint.measureText(item.name)

            // Apply horizontal alignment for text
            val xPosition = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                HorizontalAlignment.START -> 0f
                HorizontalAlignment.CENTER -> (bounds.width() - textWidth) / 2f
                HorizontalAlignment.END -> bounds.width() - textWidth
            }

            // Draw skill name
            canvas.drawText(item.name, xPosition, currentY + textPaint.textSize, textPaint)
            currentY += textPaint.textSize + 4f

            // Draw progress bar background (full width, not affected by text alignment)
            val barRect = RectF(0f, currentY, bounds.width(), currentY + barHeight)
            canvas.drawRoundRect(barRect, barRadius, barRadius, backgroundPaint)

            // Draw progress bar fill
            val proficiency = item.proficiency?.coerceIn(0f, 1f) ?: 0.5f
            val fillWidth = bounds.width() * proficiency
            if (fillWidth > 0) {
                val fillRect = RectF(0f, currentY, fillWidth, currentY + barHeight)
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
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val dotSize = mapper.borderWidthToPdfPoints(element.dotSize)
        val dotSpacing = mapper.borderWidthToPdfPoints(4f)

        // Calculate total height for vertical alignment
        val itemCount = element.items.count { it.name.isNotEmpty() }
        val totalHeight = itemCount * (textPaint.textSize + spacing) - spacing

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
            if (item.name.isEmpty()) return@forEach

            val textWidth = textPaint.measureText(item.name)

            // Apply horizontal alignment for text (dots stay on right side)
            val xPosition = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                HorizontalAlignment.START -> 0f
                HorizontalAlignment.CENTER -> (bounds.width() - textWidth) / 2f
                HorizontalAlignment.END -> bounds.width() - textWidth
            }

            // Draw skill name
            canvas.drawText(item.name, xPosition, currentY + textPaint.textSize, textPaint)

            // Calculate dot starting position (right side)
            val dotsWidth = (dotSize * element.maxDots) + (dotSpacing * (element.maxDots - 1))
            var dotX = bounds.width() - dotsWidth

            // Draw dots
            val proficiency = item.proficiency?.coerceIn(0f, 1f) ?: 0.5f
            val filledDots = (proficiency * element.maxDots).toInt()

            repeat(element.maxDots) { index ->
                val paint = if (index < filledDots) filledPaint else emptyPaint
                canvas.drawCircle(
                    dotX + dotSize / 2,
                    currentY + textPaint.textSize / 2,
                    dotSize / 2,
                    paint
                )
                dotX += dotSize + dotSpacing
            }

            currentY += textPaint.textSize + spacing
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
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val groupSpacing = mapper.borderWidthToPdfPoints(element.groupSpacing)

        // Calculate total height for vertical alignment
        val groups = element.items.groupBy { it.category }
        var totalHeight = 0f
        groups.forEach { (category, skills) ->
            if (category.isNotEmpty()) {
                totalHeight += categoryTextPaint.textSize + 4f
            }
            totalHeight += skills.count { it.name.isNotEmpty() } * (skillTextPaint.textSize + spacing)
            totalHeight += groupSpacing
        }
        totalHeight -= groupSpacing // Remove last group spacing

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
                canvas.drawText(category, xPosition, currentY + categoryTextPaint.textSize, categoryTextPaint)
                currentY += categoryTextPaint.textSize + 4f
            }

            // Draw skills in this category
            skills.forEach { item ->
                if (item.name.isNotEmpty()) {
                    val bullet = if (element.showBullets) getBulletCharacter(element.bulletStyle) + " " else ""
                    val text = bullet + item.name
                    val textWidth = skillTextPaint.measureText(text)

                    val xPosition = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                        HorizontalAlignment.START -> 0f
                        HorizontalAlignment.CENTER -> (bounds.width() - textWidth) / 2f
                        HorizontalAlignment.END -> bounds.width() - textWidth
                    }

                    canvas.drawText(text, xPosition, currentY + skillTextPaint.textSize, skillTextPaint)
                    currentY += skillTextPaint.textSize + spacing
                }
            }

            currentY += groupSpacing - spacing // Add extra spacing between groups
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
            textSize = mapper.spToPdfPoints(textStyle.fontSize)
            color = context.colorConverter.toIntColorWithOpacity(textStyle.color, opacity)

            val typefaceStyle = when {
                textStyle.isBold && textStyle.isItalic -> Typeface.BOLD_ITALIC
                textStyle.isBold -> Typeface.BOLD
                textStyle.isItalic -> Typeface.ITALIC
                else -> when (textStyle.fontWeight) {
                    androidx.compose.ui.text.font.FontWeight.Bold,
                    androidx.compose.ui.text.font.FontWeight.SemiBold,
                    androidx.compose.ui.text.font.FontWeight.ExtraBold -> Typeface.BOLD
                    else -> Typeface.NORMAL
                }
            }
            typeface = Typeface.create(Typeface.DEFAULT, typefaceStyle)

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
