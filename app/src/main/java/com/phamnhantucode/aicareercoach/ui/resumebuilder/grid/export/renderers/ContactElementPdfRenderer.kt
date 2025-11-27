package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.SvgIconLoader
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ElementPdfRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.GridCoordinateMapper
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfRenderContext

/**
 * Contact Element PDF Renderer
 *
 * Renders contact information with icons or labels
 */
class ContactElementPdfRenderer : ElementPdfRenderer<ResumeElement.ContactElement> {

    override suspend fun render(
        canvas: Canvas,
        element: ResumeElement.ContactElement,
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

        // Create text paint
        val textPaint = createTextPaint(element, mapper, context)
        val boldTextPaint = createBoldTextPaint(element, mapper, context)

        canvas.save()
        canvas.translate(contentBounds.left, contentBounds.top)
        canvas.clipRect(0f, 0f, contentBounds.width(), contentBounds.height())

        // Calculate layout based on display style and orientation
        when (element.displayStyle ?: ContactDisplayStyle.STANDARD) {
            ContactDisplayStyle.ONE_LINE -> {
                renderOneLineLayout(
                    canvas,
                    element,
                    contentBounds,
                    textPaint,
                    boldTextPaint,
                    mapper,
                    context
                )
            }
            ContactDisplayStyle.STANDARD -> {
                when (element.orientation) {
                    ContactOrientation.VERTICAL -> {
                        renderVerticalLayout(
                            canvas,
                            element,
                            contentBounds,
                            textPaint,
                            boldTextPaint,
                            mapper,
                            context
                        )
                    }
                    ContactOrientation.HORIZONTAL -> {
                        renderHorizontalLayout(
                            canvas,
                            element,
                            contentBounds,
                            textPaint,
                            boldTextPaint,
                            mapper,
                            context
                        )
                    }
                }
            }
        }

        canvas.restore()
    }

    /**
     * Render contact items in vertical layout
     */
    private fun renderVerticalLayout(
        canvas: Canvas,
        element: ResumeElement.ContactElement,
        bounds: RectF,
        textPaint: TextPaint,
        boldTextPaint: TextPaint,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val iconAfterText = (element.horizontalAlignment ?: HorizontalAlignment.START) == HorizontalAlignment.END

        // Calculate vertical starting position based on vertical alignment (matching SkillElementPdfRenderer pattern)
        val itemCount = element.items.count { it.value.isNotEmpty() }
        val totalItemHeight = itemCount * textPaint.textSize + 
                              if (itemCount > 1) (spacing * (itemCount - 1)) else 0f

        var currentY = when (element.verticalAlignment ?: VerticalAlignment.CENTER) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalItemHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalItemHeight
        }

        var isFirstItem = true
        element.items.forEach { item ->
            if (item.value.isEmpty()) return@forEach

            // Add spacing between items (not before first item)
            if (!isFirstItem) {
                currentY += spacing
            }
            isFirstItem = false

            // Measure the entire item width
            val itemWidth = measureItemWidth(item, element, textPaint, boldTextPaint, mapper)

            // Calculate horizontal starting position based on horizontal alignment
            var currentX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                HorizontalAlignment.START -> 0f
                HorizontalAlignment.CENTER -> (bounds.width() - itemWidth) / 2f
                HorizontalAlignment.END -> bounds.width() - itemWidth
            }

            // Render in order based on iconAfterText
            if (!iconAfterText) {
                currentX += drawIconOrLabel(
                    canvas, item, element, currentX, currentY,
                    textPaint, boldTextPaint, mapper, context
                )
            }

            // Draw value
            canvas.drawText(
                item.value,
                currentX,
                currentY + textPaint.textSize,
                textPaint
            )
            currentX += textPaint.measureText(item.value)

            if (iconAfterText) {
                currentX += mapper.borderWidthToPdfPoints(4f)
                drawIconOrLabel(
                    canvas, item, element, currentX, currentY,
                    textPaint, boldTextPaint, mapper, context
                )
            }

            currentY += textPaint.textSize
        }
    }

    /**
     * Render contact items in horizontal layout
     */
    private fun renderHorizontalLayout(
        canvas: Canvas,
        element: ResumeElement.ContactElement,
        bounds: RectF,
        textPaint: TextPaint,
        boldTextPaint: TextPaint,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val iconAfterText = (element.horizontalAlignment ?: HorizontalAlignment.START) == HorizontalAlignment.END

        // Calculate vertical position based on vertical alignment
        val baselineY = when (element.verticalAlignment ?: VerticalAlignment.CENTER) {
            VerticalAlignment.TOP -> textPaint.textSize
            VerticalAlignment.CENTER -> bounds.height() / 2f + textPaint.textSize / 3f
            VerticalAlignment.BOTTOM -> bounds.height()
        }

        // Calculate total width of all items (matching SkillElementPdfRenderer pattern)
        val itemCount = element.items.count { it.value.isNotEmpty() }
        val totalWidth = element.items.filter { it.value.isNotEmpty() }.sumOf {
            measureItemWidth(it, element, textPaint, boldTextPaint, mapper).toDouble()
        }.toFloat() + if (itemCount > 1) (spacing * (itemCount - 1)) else 0f

        // Calculate horizontal starting position based on horizontal alignment
        var currentX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (bounds.width() - totalWidth) / 2f
            HorizontalAlignment.END -> bounds.width() - totalWidth
        }

        var isFirstItem = true
        element.items.forEach { item ->
            if (item.value.isEmpty()) return@forEach

            // Add spacing between items (not before first item)
            if (!isFirstItem) {
                currentX += spacing
            }
            isFirstItem = false

            // Render in order based on iconAfterText
            if (!iconAfterText) {
                currentX += drawIconOrLabel(
                    canvas, item, element, currentX, baselineY - textPaint.textSize,
                    textPaint, boldTextPaint, mapper, context
                )
            }

            // Draw value
            canvas.drawText(
                item.value,
                currentX,
                baselineY,
                textPaint
            )
            currentX += textPaint.measureText(item.value)

            if (iconAfterText) {
                currentX += mapper.borderWidthToPdfPoints(4f)
                currentX += drawIconOrLabel(
                    canvas, item, element, currentX, baselineY - textPaint.textSize,
                    textPaint, boldTextPaint, mapper, context
                )
            }
        }
    }

    /**
     * Render contact items in one line layout with separators
     */
    private fun renderOneLineLayout(
        canvas: Canvas,
        element: ResumeElement.ContactElement,
        bounds: RectF,
        textPaint: TextPaint,
        boldTextPaint: TextPaint,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val iconAfterText = (element.horizontalAlignment ?: HorizontalAlignment.START) == HorizontalAlignment.END
        
        // Calculate vertical position based on vertical alignment
        val baselineY = when (element.verticalAlignment ?: VerticalAlignment.CENTER) {
            VerticalAlignment.TOP -> textPaint.textSize
            VerticalAlignment.CENTER -> bounds.height() / 2f + textPaint.textSize / 3f
            VerticalAlignment.BOTTOM -> bounds.height()
        }

        // Calculate total width of all items + separators
        val visibleItems = element.items.filter { it.value.isNotEmpty() }
        val separator = element.separator ?: " • "
        val separatorWidth = textPaint.measureText(separator)
        
        val totalWidth = visibleItems.sumOf {
            measureItemWidth(it, element, textPaint, boldTextPaint, mapper).toDouble()
        }.toFloat() + if (visibleItems.size > 1) (separatorWidth * (visibleItems.size - 1)) else 0f

        // Calculate horizontal starting position based on horizontal alignment
        var currentX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (bounds.width() - totalWidth) / 2f
            HorizontalAlignment.END -> bounds.width() - totalWidth
        }

        visibleItems.forEachIndexed { index, item ->
            // Render in order based on iconAfterText
            if (!iconAfterText) {
                currentX += drawIconOrLabel(
                    canvas, item, element, currentX, baselineY - textPaint.textSize,
                    textPaint, boldTextPaint, mapper, context
                )
            }

            // Draw value
            canvas.drawText(
                item.value,
                currentX,
                baselineY,
                textPaint
            )
            currentX += textPaint.measureText(item.value)

            if (iconAfterText) {
                currentX += mapper.borderWidthToPdfPoints(4f)
                currentX += drawIconOrLabel(
                    canvas, item, element, currentX, baselineY - textPaint.textSize,
                    textPaint, boldTextPaint, mapper, context
                )
            }
            
            // Draw separator if not last item
            if (index < visibleItems.size - 1) {
                canvas.drawText(
                    separator,
                    currentX,
                    baselineY,
                    textPaint
                )
                currentX += separatorWidth
            }
        }
    }

    /**
     * Measure the total width of an item (icon/label + value + spacing)
     */
    private fun measureItemWidth(
        item: ContactItem,
        element: ResumeElement.ContactElement,
        textPaint: TextPaint,
        boldTextPaint: TextPaint,
        mapper: GridCoordinateMapper
    ): Float {
        var width = textPaint.measureText(item.value)

        when (element.iconStyle) {
            ContactIconStyle.ICON -> {
                val iconSize = mapper.borderWidthToPdfPoints(element.iconSize)
                width += iconSize + mapper.borderWidthToPdfPoints(4f)
            }
            ContactIconStyle.BOLD_LABEL -> {
                if (item.label.isNotEmpty()) {
                    width += boldTextPaint.measureText(item.label) + mapper.borderWidthToPdfPoints(4f)
                }
            }
            ContactIconStyle.NONE -> {
                // No additional width
            }
        }

        return width
    }

    /**
     * Draw icon or label and return the width consumed
     */
    private fun drawIconOrLabel(
        canvas: Canvas,
        item: ContactItem,
        element: ResumeElement.ContactElement,
        x: Float,
        y: Float,
        textPaint: TextPaint,
        boldTextPaint: TextPaint,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): Float {
        return when (element.iconStyle) {
            ContactIconStyle.ICON -> {
                val iconSize = mapper.borderWidthToPdfPoints(element.iconSize)
                
                // Try to load and render SVG icon
                val svg = SvgIconLoader.loadSvgSync(context.context, item.iconName)
                
                if (svg != null) {
                    // Render SVG icon
                    val iconColor = context.colorConverter.toIntColorWithOpacity(
                        element.textStyle.color,
                        element.style.opacity
                    )
                    
                    canvas.save()
                    canvas.translate(x, y + (textPaint.textSize - iconSize) / 2f)
                    
                    SvgIconLoader.renderToCanvas(
                        svg,
                        canvas,
                        iconSize,
                        iconSize,
                        iconColor
                    )
                    
                    canvas.restore()
                } else {
                    // Fallback: Draw placeholder circle
                    val iconPaint = Paint().apply {
                        color = context.colorConverter.toIntColorWithOpacity(
                            element.textStyle.color,
                            element.style.opacity
                        )
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f
                        isAntiAlias = true
                    }
                    
                    canvas.drawCircle(
                        x + iconSize / 2f,
                        y + textPaint.textSize / 2f,
                        iconSize / 3f,
                        iconPaint
                    )
                }
                iconSize + mapper.borderWidthToPdfPoints(4f)
            }
            ContactIconStyle.BOLD_LABEL -> {
                if (item.label.isNotEmpty()) {
                    canvas.drawText(
                        item.label,
                        x,
                        y + textPaint.textSize,
                        boldTextPaint
                    )
                    boldTextPaint.measureText(item.label) + mapper.borderWidthToPdfPoints(4f)
                } else {
                    0f
                }
            }
            ContactIconStyle.NONE -> {
                0f
            }
        }
    }

    /**
     * Create text paint with styling
     */
    private fun createTextPaint(
        element: ResumeElement.ContactElement,
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

            // Letter spacing (in EM units)
            if (element.textStyle.letterSpacing != 0f) {
                letterSpacing = element.textStyle.letterSpacing / element.textStyle.fontSize
            }
        }
    }

    /**
     * Create bold text paint for labels
     */
    private fun createBoldTextPaint(
        element: ResumeElement.ContactElement,
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
            // Use Poppins Bold font
            typeface = com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager.getPoppinsTypeface(
                context.context,
                androidx.compose.ui.text.font.FontWeight.Bold,
                false
            )
            isUnderlineText = element.textStyle.isUnderlined

            // Letter spacing (in EM units)
            if (element.textStyle.letterSpacing != 0f) {
                letterSpacing = element.textStyle.letterSpacing / element.textStyle.fontSize
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
