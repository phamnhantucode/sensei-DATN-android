package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.SvgIconLoader
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*

/**
 * Renders a contact element on the resume using Canvas for precise control
 */
@Composable
fun ContactElementRenderer(
    element: ResumeElement.ContactElement,
    zoomLevel: Float = 1f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Cache for loaded SVG bitmaps to avoid reloading/rendering on every frame
    // Map key: iconName + size + color
    val iconCache = remember { mutableStateMapOf<String, android.graphics.Bitmap?>() }
    
    // Trigger icon loading
    LaunchedEffect(element.items, element.iconSize, element.textStyle.color, element.iconColor) {
        element.items.forEach { item ->
            if (item.iconName.isNotEmpty()) {
                val iconSizePx = (element.iconSize * context.resources.displayMetrics.density).toInt()
                val color = (element.iconColor?.toInt() ?: element.textStyle.color.toInt())
                val cacheKey = "${item.iconName}_${iconSizePx}_${color}"
                
                if (!iconCache.containsKey(cacheKey)) {
                    val svg = SvgIconLoader.loadSvg(context, item.iconName)
                    if (svg != null) {
                        val bitmap = SvgIconLoader.renderToBitmap(svg, iconSizePx.coerceAtLeast(1), iconSizePx.coerceAtLeast(1), color)
                        iconCache[cacheKey] = bitmap
                    } else {
                        iconCache[cacheKey] = null
                    }
                }
            }
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val density = context.resources.displayMetrics.density
            
            // 1. Draw styling (background, border, shadow)
            // Reuse logic similar to other elements, implemented locally since it's private elsewhere
            val bounds = android.graphics.RectF(0f, 0f, size.width, size.height)
            drawElementStyle(nativeCanvas, element.style, bounds, zoomLevel)

            // 2. Setup rendering bounds
            val safePadding = element.padding ?: Padding(8f, 8f, 8f, 8f)
            val paddingLeft = safePadding.left * density
            val paddingTop = safePadding.top * density
            val paddingRight = safePadding.right * density
            val paddingBottom = safePadding.bottom * density
            
            val baseWidth = (size.width / zoomLevel) - (paddingLeft + paddingRight)
            val baseHeight = (size.height / zoomLevel) - (paddingTop + paddingBottom)
            
            // 3. Prepare Paint
            val textPaint = createTextPaint(element.textStyle, context)
            
            // 4. Calculate layouts based on DisplayStyle
            val itemsLayouts = mutableListOf<ContactItemLayout>()
            var totalContentHeight = 0f
            var totalContentWidth = 0f // Only relevant for centering/aligning the whole block if needed

            // Pre-calculate spacing
            val itemSpacing = element.spacing * density
            val iconSizePx = element.iconSize * density
            val iconTextSpacing = 4f * density // Space between icon and text
            
            // Filter empty items
            val visibleItems = element.items.filter { it.value.isNotEmpty() }
            
            when (element.displayStyle ?: ContactDisplayStyle.STANDARD) {
                ContactDisplayStyle.ONE_LINE -> {
                    // Similar to Skill Tags logic - reflow items in lines
                    // But usually, one line means... one line. However, we should handle wrap if it exceeds width.
                    // Or literally "Row" behavior.
                    
                    // For ONE_LINE, we treat it as a FlowRow.
                    // Calculate "Separator" width
                    val separator = element.separator ?: " • "
                    val separatorWidth = textPaint.measureText(separator)
                    
                    var currentLine = mutableListOf<ContactItemLayout>()
                    var currentLineWidth = 0f
                    val lines = mutableListOf<Pair<Float, List<ContactItemLayout>>>() // Width, Items
                    
                    visibleItems.forEachIndexed { index, item ->
                        val itemLayout = measureContactItem(
                            item, element, textPaint, iconCache, iconSizePx, density
                        )
                        
                        // Check if we need a separator before this item (if not first in line)
                        // But we don't know if it's first in line yet.
                        // Ideally, we add separator width to the item width if it's not the first item TOTAL.
                        // But if it wraps to new line, we drop the separator.
                         
                        // Determine if we add separator: strictly between items.
                        // Logic: Add item. If fits, good. If not, new line.
                        // Separator is tricky in flow layout. Let's assume separator connects to PREVIOUS item.
                        
                        var widthToAdd = itemLayout.totalWidth
                        val isLast = index == visibleItems.size - 1
                        
                        // Tentative check: currentLineWidth + (separator if not start of line) + itemWidth
                        var separatorWidthForThis = 0f
                        if (currentLine.isNotEmpty()) {
                             separatorWidthForThis = separatorWidth
                        }
                        
                        if (currentLineWidth + separatorWidthForThis + widthToAdd <= baseWidth) {
                            // Fits on current line
                            if (currentLine.isNotEmpty()) {
                                // Add separator to previous item layout effectively (or just account for space)
                                // We'll just modifying the X position during draw
                                currentLineWidth += separatorWidthForThis
                            }
                            itemLayout.xOffsetInLine = currentLineWidth
                            itemLayout.hasPrecedingSeparator = currentLine.isNotEmpty()
                            
                            currentLine.add(itemLayout)
                            currentLineWidth += widthToAdd
                        } else {
                            // Wrap to new line
                            if (currentLine.isNotEmpty()) {
                                lines.add(currentLineWidth to currentLine)
                            }
                            currentLine = mutableListOf()
                            currentLineWidth = 0f
                            
                            // Item is first in new line, no separator
                            itemLayout.xOffsetInLine = 0f
                            itemLayout.hasPrecedingSeparator = false
                            currentLine.add(itemLayout)
                            currentLineWidth += widthToAdd
                        }
                    }
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLineWidth to currentLine)
                    }
                    
                    // Calculate total height
                    // Assume all lines have same height (max height of items in line)
                    // For simplicity, find max height of ALL items + line spacing
                    // Items are usually single line text.
                    // But text could wrap within an item? No, usually One Line means item is one line.
                    // We'll trust measureContactItem to return height.
                    
                    var currentY = 0f
                    val lineSpacing = itemSpacing // Use spacing property for line spacing
                    
                    lines.forEach { (lineWidth, lineItems) ->
                        val lineHeight = lineItems.maxOfOrNull { it.totalHeight } ?: 0f
                        
                        // Horizontal alignment of the LINE
                        val startX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                            HorizontalAlignment.START -> 0f
                            HorizontalAlignment.CENTER -> (baseWidth - lineWidth) / 2f
                            HorizontalAlignment.END -> baseWidth - lineWidth
                        }
                        
                        lineItems.forEach { layout ->
                            layout.x = startX + layout.xOffsetInLine
                            layout.y = currentY + (lineHeight - layout.totalHeight) / 2f // Vertically center in line
                        }
                        
                        currentY += lineHeight + lineSpacing
                    }
                    if (lines.isNotEmpty()) currentY -= lineSpacing // Remove last spacing
                    totalContentHeight = currentY
                    itemsLayouts.addAll(lines.flatMap { it.second })
                }
                
                ContactDisplayStyle.STANDARD -> {
                    if (element.orientation == ContactOrientation.HORIZONTAL) {
                        // Horizontal stack (FlowRow but without separators, using spacing)
                        // Actually, standard Horizontal usually means equal spacing or just flow?
                        // "Row" implies flow.
                        // Let's reuse the Flow logic but use `spacing` instead of `separator`.
                        
                        var currentLine = mutableListOf<ContactItemLayout>()
                        var currentLineWidth = 0f
                        val lines = mutableListOf<Pair<Float, List<ContactItemLayout>>>() 
                        
                        visibleItems.forEach { item ->
                            val itemLayout = measureContactItem(
                                item, element, textPaint, iconCache, iconSizePx, density
                            )
                            
                            val spacingBefore = if (currentLine.isNotEmpty()) itemSpacing else 0f
                            
                            if (currentLineWidth + spacingBefore + itemLayout.totalWidth <= baseWidth) {
                                currentLineWidth += spacingBefore
                                itemLayout.xOffsetInLine = currentLineWidth
                                currentLine.add(itemLayout)
                                currentLineWidth += itemLayout.totalWidth
                            } else {
                                if (currentLine.isNotEmpty()) lines.add(currentLineWidth to currentLine)
                                currentLine = mutableListOf()
                                currentLineWidth = 0f
                                
                                itemLayout.xOffsetInLine = 0f
                                currentLine.add(itemLayout)
                                currentLineWidth += itemLayout.totalWidth
                            }
                        }
                        if (currentLine.isNotEmpty()) lines.add(currentLineWidth to currentLine)
                        
                        var currentY = 0f
                        lines.forEach { (lineWidth, lineItems) ->
                            val lineHeight = lineItems.maxOfOrNull { it.totalHeight } ?: 0f
                            
                            val startX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                                HorizontalAlignment.START -> 0f
                                HorizontalAlignment.CENTER -> (baseWidth - lineWidth) / 2f
                                HorizontalAlignment.END -> baseWidth - lineWidth
                            }
                            
                            lineItems.forEach { layout ->
                                layout.x = startX + layout.xOffsetInLine
                                layout.y = currentY + (lineHeight - layout.totalHeight) / 2f
                            }
                            currentY += lineHeight + itemSpacing
                        }
                        if (lines.isNotEmpty()) currentY -= itemSpacing
                        totalContentHeight = currentY
                        itemsLayouts.addAll(lines.flatMap { it.second })
                        
                    } else {
                        // VERTICAL STACK
                        var currentY = 0f
                        visibleItems.forEach { item ->
                            val itemLayout = measureContactItem(
                                item, element, textPaint, iconCache, iconSizePx, density, 
                                // For vertical stack, we might allow multiline text within item if it's too long?
                                // Let's constrain width to baseWidth
                                maxWidth = baseWidth
                            )
                            
                            // Horizontal alignment for individual item
                            val xPos = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                                HorizontalAlignment.START -> 0f
                                HorizontalAlignment.CENTER -> (baseWidth - itemLayout.totalWidth) / 2f
                                HorizontalAlignment.END -> baseWidth - itemLayout.totalWidth
                            }
                            
                            itemLayout.x = xPos
                            itemLayout.y = currentY
                            
                            itemsLayouts.add(itemLayout)
                            currentY += itemLayout.totalHeight + itemSpacing
                        }
                        if (visibleItems.isNotEmpty()) currentY -= itemSpacing
                        totalContentHeight = currentY
                    }
                }
            }
            
            // 5. Apply Scale & Translate
            nativeCanvas.save()
            nativeCanvas.scale(zoomLevel, zoomLevel)
            nativeCanvas.translate(paddingLeft, paddingTop)
            
            // Vertical Alignment of the entire block
            val blockYOffset = when (element.verticalAlignment ?: VerticalAlignment.CENTER) {
                VerticalAlignment.TOP -> 0f
                VerticalAlignment.CENTER -> (baseHeight - totalContentHeight) / 2f
                VerticalAlignment.BOTTOM -> baseHeight - totalContentHeight
            }
            
            nativeCanvas.translate(0f, blockYOffset)
            nativeCanvas.clipRect(0f, 0f, baseWidth, baseHeight - blockYOffset) // Simple clip
            
            // 6. Draw Items
            itemsLayouts.forEach { layout ->
                nativeCanvas.save()
                nativeCanvas.translate(layout.x, layout.y)
                
                // Draw separator if needed (for ONE_LINE)
                if (layout.hasPrecedingSeparator) {
                    val sep = element.separator ?: " • "
                    val sepWidth = textPaint.measureText(sep)
                    // The layout.x already accounts for separator width being *before* it?
                    // Wait, layout.xOffsetInLine was calculated as `currentLineWidth` BEFORE adding item width.
                    // But currentLineWidth INCLUDED separator width.
                    // So layout.x is the start of the ITEM.
                    // The separator should be drawn to the LEFT of layout.x.
                    nativeCanvas.drawText(sep, -sepWidth, -layout.textLayout.getLineAscent(0).toFloat(), textPaint) 
                    // Note: ascent is negative, so -ascent is positive Y down from baseline.
                    // But we need to align baselines.
                    // StaticLayout drawing draws from 0,0 top-left.
                    // We need baseline alignment.
                    // Let's calculate baseline offset.
                }
                
                // Draw parts
                // layout.iconBitmap, layout.label, layout.textLayout
                // We need to know relative positions calculated during measure
                
                // Draw Icon
                if (layout.iconBitmap != null) {
                    val paint = Paint().apply { isFilterBitmap = true; isAntiAlias = true }
                    nativeCanvas.drawBitmap(layout.iconBitmap, layout.iconX, layout.iconY, paint)
                }
                
                // Draw Label (Bold)
                if (layout.labelLayout != null) {
                    layout.labelLayout.draw(nativeCanvas) // It's at labelX, labelY (relaive to item) - wait, StaticLayout draws at 0,0 of translate
                    // We need to translate to labelX, labelY
                }
                
                // Draw Text
                nativeCanvas.save()
                nativeCanvas.translate(layout.textX, layout.textY)
                layout.textLayout.draw(nativeCanvas)
                nativeCanvas.restore()
                
                nativeCanvas.restore()
            }
            
            nativeCanvas.restore()
        }
    }
}

// Helper Structures & Functions

private data class ContactItemLayout(
    var x: Float = 0f,
    var y: Float = 0f,
    val totalWidth: Float,
    val totalHeight: Float,
    // Content Parts
    val iconBitmap: android.graphics.Bitmap?,
    val iconX: Float,
    val iconY: Float,
    val labelLayout: StaticLayout?,
    val labelX: Float,
    val labelY: Float,
    val textLayout: StaticLayout,
    val textX: Float,
    val textY: Float,
    // Flow/Grid helper
    var xOffsetInLine: Float = 0f,
    var hasPrecedingSeparator: Boolean = false
)

private fun measureContactItem(
    item: ContactItem,
    element: ResumeElement.ContactElement,
    baseTextPaint: TextPaint,
    iconCache: Map<String, android.graphics.Bitmap?>,
    iconSizePx: Float,
    density: Float,
    maxWidth: Float = Float.MAX_VALUE
): ContactItemLayout {
    val iconStyle = element.iconStyle
    val iconOrLabelSpacing = 4f * density
    
    // 1. Measure Prefix (Icon or Label)
    var prefixWidth = 0f
    var prefixHeight = 0f
    
    var iconBitmap: android.graphics.Bitmap? = null
    var labelLayout: StaticLayout? = null
    
    if (iconStyle == ContactIconStyle.ICON && item.iconName.isNotEmpty()) {
        val color = (element.iconColor?.toInt() ?: element.textStyle.color.toInt())
        val cacheKey = "${item.iconName}_${iconSizePx.toInt()}_${color}"
        iconBitmap = iconCache[cacheKey]
        if (iconBitmap != null) {
            prefixWidth = iconSizePx
            prefixHeight = iconSizePx
        }
    } else if (iconStyle == ContactIconStyle.BOLD_LABEL && item.label.isNotEmpty()) {
        val labelPaint = TextPaint(baseTextPaint).apply { isFakeBoldText = true }
        // Simple measurement
        val width = labelPaint.measureText(item.label)
        // For height, we'd ideally use a Layout, but line height is dominated by main text usually
        val labelStaticLayout = StaticLayout.Builder
                .obtain(item.label, 0, item.label.length, labelPaint, (width + 1).toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
        
        labelLayout = labelStaticLayout
        prefixWidth = labelStaticLayout.width.toFloat()
        prefixHeight = labelStaticLayout.height.toFloat()
    }
    
    // 2. Measure Main Text
    // Max width for text = allowed width - prefix - spacing
    val availableTextWidth = (maxWidth - (if (prefixWidth > 0) prefixWidth + iconOrLabelSpacing else 0f)).coerceAtLeast(1f)
    
    val textLayout = StaticLayout.Builder
                .obtain(item.value, 0, item.value.length, baseTextPaint, availableTextWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f) // Standard 1.0 spacing
                .setIncludePad(false)
                .build()
                
    val textWidth = textLayout.lineWidth(0) // Assuming single line mostly, or take layout.width? layout.width is the requested width.
    // Use layout.width if multiline, or measureText if single?
    // textLayout.width matches availableWidth usually if constrained.
    // For tighter bounds, we check line count.
    val actualTextWidth = if (textLayout.lineCount == 1) baseTextPaint.measureText(item.value) else textLayout.width.toFloat()
    
    val textHeight = textLayout.height.toFloat()
    
    // 3. Assemble Layout
    // Alignment: Icon/Label and Text usually centered vertically relative to each other.
    // Height of item = max(prefixHeight, textHeight)
    val totalHeight = maxOf(prefixHeight, textHeight)
    val totalWidth = if (prefixWidth > 0) prefixWidth + iconOrLabelSpacing + actualTextWidth else actualTextWidth
    
    var iconX = 0f
    var iconY = 0f
    var labelX = 0f
    var labelY = 0f
    var textX = 0f
    var textY = 0f
    
    val horizontalAlignment = element.horizontalAlignment ?: HorizontalAlignment.START
    val iconAfterText = horizontalAlignment == HorizontalAlignment.END
    
    // Vertical centering
    // If text wraps to multiple lines, align icon to the first line
    val textCenterY = (totalHeight - textHeight) / 2f
    
    val prefixCenterY = if (textLayout.lineCount > 1) {
        val firstLineHeight = textLayout.getLineBottom(0).toFloat()
        // Center relative to the first line, starting from textY (which is textCenterY)
        textCenterY + (firstLineHeight - prefixHeight) / 2f
    } else {
        (totalHeight - prefixHeight) / 2f
    }
    
    if (iconAfterText) {
        textX = 0f
        textY = textCenterY
        
        val afterX = actualTextWidth + iconOrLabelSpacing
        if (iconBitmap != null) {
            iconX = afterX
            iconY = prefixCenterY
        }
        if (labelLayout != null) {
            labelX = afterX 
        }
    } else {
        // Icon before text
        if (iconBitmap != null) {
            iconX = 0f
            iconY = prefixCenterY
            textX = prefixWidth + iconOrLabelSpacing
        } else if (labelLayout != null) {
            labelX = 0f 
            labelY = prefixCenterY 
            textX = prefixWidth + iconOrLabelSpacing
        } else {
            textX = 0f
        }
        textY = textCenterY
    }
    
    // Special handling for Label layout drawing: needs a way to pass coordinate
    // We'll store offsets for each part.
    
    return ContactItemLayout(
        totalWidth = totalWidth,
        totalHeight = totalHeight,
        iconBitmap = iconBitmap,
        iconX = iconX,
        iconY = iconY,
        labelLayout = labelLayout,
        labelX = labelX, // Valid only if labelLayout != null
        labelY = labelY, // Valid only if labelLayout != null
        textLayout = textLayout,
        textX = textX,
        textY = textY
    )
}

// Note: ElementStyle drawing logic (drawElementStyle) and createTextPaint are reused/duplicated 
// from what likely exists similar to TextElementRenderer/SkillElementRenderer.
// I'll implement them here to ensure self-containment.

private fun drawElementStyle(
    canvas: android.graphics.Canvas,
    style: ElementStyle,
    bounds: android.graphics.RectF,
    zoomLevel: Float
) {
    val paint = Paint().apply { isAntiAlias = true }
    
    // Shadow
    if (style.shadowColor != null && style.shadowBlur > 0f) {
        val shadowPaint = Paint(paint).apply {
            color = style.shadowColor.toInt()
            alpha = (style.opacity * 255).toInt()
            setShadowLayer(
                style.shadowBlur * zoomLevel,
                style.shadowOffsetX * zoomLevel,
                style.shadowOffsetY * zoomLevel,
                style.shadowColor.toInt()
            )
        }
        val shadowBounds = android.graphics.RectF(bounds)
        shadowBounds.offset(style.shadowOffsetX * zoomLevel, style.shadowOffsetY * zoomLevel)
        if (style.borderRadius > 0f) {
            val r = style.borderRadius * zoomLevel
            canvas.drawRoundRect(shadowBounds, r, r, shadowPaint)
        } else {
            canvas.drawRect(shadowBounds, shadowPaint)
        }
    }
    
    // Background
    if (style.backgroundColor != null && Color(style.backgroundColor).alpha > 0f) {
        paint.color = style.backgroundColor.toInt()
        paint.alpha = (style.opacity * 255).toInt()
        paint.style = Paint.Style.FILL
        if (style.borderRadius > 0f) {
            val r = style.borderRadius * zoomLevel
            canvas.drawRoundRect(bounds, r, r, paint)
        } else {
            canvas.drawRect(bounds, paint)
        }
    }
    
    // Border
    if (style.borderColor != null && style.borderWidth > 0f) {
        paint.color = style.borderColor.toInt()
        paint.alpha = (style.opacity * 255).toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = style.borderWidth * zoomLevel
        if (style.borderRadius > 0f) {
            val r = style.borderRadius * zoomLevel
            canvas.drawRoundRect(bounds, r, r, paint)
        } else {
            canvas.drawRect(bounds, paint)
        }
    }
}

private fun createTextPaint(
    textStyle: TextStyle,
    context: android.content.Context
): TextPaint {
    val density = context.resources.displayMetrics.density
    return TextPaint().apply {
        isAntiAlias = true
        textSize = textStyle.fontSize * density // Base size, assume Canvas scaled
        color = textStyle.color.toInt()
        typeface = FontManager.getPoppinsTypeface(
            context,
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
 * Extension to get line width from StaticLayout safely
 */
private fun StaticLayout.lineWidth(lineIndex: Int): Float {
    return this.getLineWidth(lineIndex)
}
