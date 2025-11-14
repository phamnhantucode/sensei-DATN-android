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

        canvas.save()
        canvas.translate(bounds.left, bounds.top)
        canvas.clipRect(0f, 0f, bounds.width(), bounds.height())

        // Render based on display style
        when (element.displayStyle) {
            SkillDisplayStyle.LIST -> {
                renderListLayout(canvas, element, bounds, mapper, context)
            }
            SkillDisplayStyle.TAGS -> {
                renderTagsLayout(canvas, element, bounds, mapper, context)
            }
            SkillDisplayStyle.PROGRESS_BARS -> {
                renderProgressBarsLayout(canvas, element, bounds, mapper, context)
            }
            SkillDisplayStyle.DOTS -> {
                renderDotsLayout(canvas, element, bounds, mapper, context)
            }
            SkillDisplayStyle.GROUPED -> {
                renderGroupedLayout(canvas, element, bounds, mapper, context)
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
        var currentY = spacing / 2

        element.items.forEach { item ->
            if (item.name.isNotEmpty()) {
                val bullet = if (element.showBullets) getBulletCharacter(element.bulletStyle) + " " else ""
                val text = bullet + item.name

                canvas.drawText(text, 0f, currentY + textPaint.textSize, textPaint)
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
        
        val tagBackgroundPaint = Paint().apply {
            color = element.tagBackgroundColor?.toInt() ?: 0xFFE3F2FD.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        var currentX = 0f
        var currentY = 0f
        val rowHeight = textPaint.textSize + tagPadding * 2

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
        var currentY = 0f

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

            // Draw skill name
            canvas.drawText(item.name, 0f, currentY + textPaint.textSize, textPaint)
            currentY += textPaint.textSize + 4f

            // Draw progress bar background
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
        var currentY = textPaint.textSize / 2

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

            // Draw skill name
            canvas.drawText(item.name, 0f, currentY + textPaint.textSize, textPaint)

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
        var currentY = 0f

        element.items.groupBy { it.category }.forEach { (category, skills) ->
            // Draw category header
            if (category.isNotEmpty()) {
                canvas.drawText(category, 0f, currentY + categoryTextPaint.textSize, categoryTextPaint)
                currentY += categoryTextPaint.textSize + 4f
            }

            // Draw skills in this category
            skills.forEach { item ->
                if (item.name.isNotEmpty()) {
                    val bullet = if (element.showBullets) getBulletCharacter(element.bulletStyle) + " " else ""
                    val text = bullet + item.name

                    canvas.drawText(text, 0f, currentY + skillTextPaint.textSize, skillTextPaint)
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
