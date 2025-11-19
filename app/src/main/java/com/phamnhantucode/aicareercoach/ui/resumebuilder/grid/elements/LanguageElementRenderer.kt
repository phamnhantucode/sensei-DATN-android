package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.*

/**
 * Renders a language element on the resume
 * Displays a list of languages with proficiency levels in various visual styles
 * Uses Canvas rendering for proper zoom handling without clipping issues
 */
@Composable
fun LanguageElementRenderer(
    element: ResumeElement.LanguageElement,
    zoomLevel: Float = 1f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val density = context.resources.displayMetrics.density
            
            // Calculate base dimensions (unscaled)
            val basePadding = 8f * density
            val baseWidth = (size.width / zoomLevel) - (basePadding * 2)
            val baseHeight = (size.height / zoomLevel) - (basePadding * 2)
            
            // Apply zoom via Canvas scaling
            nativeCanvas.save()
            nativeCanvas.scale(zoomLevel, zoomLevel)
            nativeCanvas.translate(basePadding, basePadding)
            
            // Clip to content bounds
            nativeCanvas.clipRect(0f, 0f, baseWidth, baseHeight)
            
            // Render based on display style
            when (element.displayStyle) {
                LanguageDisplayStyle.TEXT_LABELS -> {
                    drawTextLabelsLanguageLayout(nativeCanvas, element, context, baseWidth, baseHeight, density)
                }
                LanguageDisplayStyle.PROGRESS_BARS -> {
                    drawProgressBarsLanguageLayout(nativeCanvas, element, context, baseWidth, baseHeight, density)
                }
                LanguageDisplayStyle.DOTS -> {
                    drawDotsLanguageLayout(nativeCanvas, element, context, baseWidth, baseHeight, density)
                }
                LanguageDisplayStyle.TAGS -> {
                    drawTagsLanguageLayout(nativeCanvas, element, context, baseWidth, baseHeight, density)
                }
            }
            
            nativeCanvas.restore()
        }
    }
}

/**
 * Text labels layout: Simple text layout (Language - Proficiency)
 */
private fun drawTextLabelsLanguageLayout(
    canvas: android.graphics.Canvas,
    element: ResumeElement.LanguageElement,
    context: android.content.Context,
    width: Float,
    height: Float,
    density: Float
) {
    val languageTextPaint = createTextPaint(element.languageStyle, context)
    val proficiencyTextPaint = createTextPaint(element.proficiencyLabelStyle, context)
    val spacing = element.spacing * density
    
    // Calculate layouts for all items
    val layouts = mutableListOf<Triple<String, String, Float>>()
    var totalHeight = 0f
    
    element.items.forEach { item ->
        if (item.name.isNotEmpty()) {
            val proficiencyText = getProficiencyText(item, element.proficiencyType)
            val itemHeight = languageTextPaint.textSize.coerceAtLeast(proficiencyTextPaint.textSize)
            layouts.add(Triple(item.name, proficiencyText, itemHeight))
            totalHeight += itemHeight
        }
    }
    
    if (layouts.size > 1) {
        totalHeight += spacing * (layouts.size - 1)
    }
    
    // Apply vertical alignment
    var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
        VerticalAlignment.TOP -> 0f
        VerticalAlignment.CENTER -> (height - totalHeight) / 2f
        VerticalAlignment.BOTTOM -> height - totalHeight
    }
    
    // Draw each item
    var isFirstItem = true
    layouts.forEach { (name, proficiencyText, itemHeight) ->
        if (!isFirstItem) {
            currentY += spacing
        }
        isFirstItem = false
        
        val nameWidth = languageTextPaint.measureText(name)
        val proficiencyWidth = if (proficiencyText.isNotEmpty()) proficiencyTextPaint.measureText(proficiencyText) else 0f
        
        // Draw language name - aligned according to horizontalAlignment
        val nameX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (width - nameWidth) / 2f
            HorizontalAlignment.END -> width - nameWidth
        }
        
        canvas.drawText(name, nameX, currentY + languageTextPaint.textSize, languageTextPaint)
        
        // Draw proficiency label - always at the right edge
        if (proficiencyText.isNotEmpty()) {
            canvas.drawText(
                proficiencyText,
                width - proficiencyWidth,
                currentY + proficiencyTextPaint.textSize,
                proficiencyTextPaint
            )
        }
        
        currentY += itemHeight
    }
}

/**
 * Progress bars layout: Visual bars showing proficiency
 */
private fun drawProgressBarsLanguageLayout(
    canvas: android.graphics.Canvas,
    element: ResumeElement.LanguageElement,
    context: android.content.Context,
    width: Float,
    height: Float,
    density: Float
) {
    val languageTextPaint = createTextPaint(element.languageStyle, context)
    val proficiencyTextPaint = createTextPaint(element.proficiencyLabelStyle, context)
    val spacing = element.spacing * density
    val barHeight = element.progressBarHeight * density
    val barRadius = element.progressBarCornerRadius * density
    
    val backgroundPaint = Paint().apply {
        color = (element.progressBarBackgroundColor ?: 0xFFE0E0E0.toInt()).toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val progressPaint = Paint().apply {
        color = (element.progressBarColor ?: 0xFF2196F3.toInt()).toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    // Calculate total height
    var itemCount = 0
    var totalHeight = 0f
    element.items.forEach { item ->
        if (item.name.isNotEmpty()) {
            totalHeight += languageTextPaint.textSize + 2f * density + barHeight
            itemCount++
        }
    }
    if (itemCount > 1) {
        totalHeight += spacing * (itemCount - 1)
    }
    
    // Apply vertical alignment
    var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
        VerticalAlignment.TOP -> 0f
        VerticalAlignment.CENTER -> (height - totalHeight) / 2f
        VerticalAlignment.BOTTOM -> height - totalHeight
    }
    
    var isFirstItem = true
    element.items.forEach { item ->
        if (item.name.isEmpty()) return@forEach
        
        if (!isFirstItem) {
            currentY += spacing
        }
        isFirstItem = false
        
        // Draw language name and proficiency label
        val proficiencyText = getProficiencyText(item, element.proficiencyType)
        val nameWidth = languageTextPaint.measureText(item.name)
        val proficiencyWidth = if (proficiencyText.isNotEmpty()) proficiencyTextPaint.measureText(proficiencyText) else 0f
        
        // Draw language name - aligned according to horizontalAlignment
        val nameX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (width - nameWidth) / 2f
            HorizontalAlignment.END -> width - nameWidth
        }
        
        canvas.drawText(item.name, nameX, currentY + languageTextPaint.textSize, languageTextPaint)
        
        // Draw proficiency label - always at the right edge
        if (proficiencyText.isNotEmpty()) {
            canvas.drawText(
                proficiencyText,
                width - proficiencyWidth,
                currentY + proficiencyTextPaint.textSize,
                proficiencyTextPaint
            )
        }
        
        currentY += languageTextPaint.textSize + 2f * density
        
        // Draw progress bar background
        val barStartX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> 0f
            HorizontalAlignment.END -> 0f
        }
        val barRect = RectF(barStartX, currentY, barStartX + width, currentY + barHeight)
        canvas.drawRoundRect(barRect, barRadius, barRadius, backgroundPaint)
        
        // Draw progress fill
        val proficiency = item.proficiency.coerceIn(0f, 1f)
        val fillWidth = width * proficiency
        if (fillWidth > 0) {
            val fillRect = RectF(barStartX, currentY, barStartX + fillWidth, currentY + barHeight)
            canvas.drawRoundRect(fillRect, barRadius, barRadius, progressPaint)
        }
        
        currentY += barHeight
    }
}

/**
 * Dots layout: Dot indicators for proficiency
 */
private fun drawDotsLanguageLayout(
    canvas: android.graphics.Canvas,
    element: ResumeElement.LanguageElement,
    context: android.content.Context,
    width: Float,
    height: Float,
    density: Float
) {
    val languageTextPaint = createTextPaint(element.languageStyle, context)
    val spacing = element.spacing * density
    val dotSize = element.dotSize * density
    val dotSpacing = 4f * density
    
    val filledPaint = Paint().apply {
        color = (element.progressBarColor ?: 0xFF2196F3.toInt()).toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val emptyPaint = Paint().apply {
        color = (element.progressBarBackgroundColor ?: 0xFFE0E0E0.toInt()).toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    // Calculate layouts for all items
    val layouts = mutableListOf<Pair<LanguageItem, Float>>()
    var totalHeight = 0f
    
    element.items.forEach { item ->
        if (item.name.isNotEmpty()) {
            val itemHeight = languageTextPaint.textSize
            layouts.add(item to itemHeight)
            totalHeight += itemHeight
        }
    }
    
    if (layouts.size > 1) {
        totalHeight += spacing * (layouts.size - 1)
    }
    
    // Apply vertical alignment
    var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
        VerticalAlignment.TOP -> 0f
        VerticalAlignment.CENTER -> (height - totalHeight) / 2f
        VerticalAlignment.BOTTOM -> height - totalHeight
    }
    
    var isFirstItem = true
    layouts.forEach { (item, itemHeight) ->
        if (!isFirstItem) {
            currentY += spacing
        }
        isFirstItem = false
        
        val textWidth = languageTextPaint.measureText(item.name)
        val dotsWidth = (dotSize * element.maxDots) + (dotSpacing * (element.maxDots - 1))
        val totalItemWidth = textWidth + 8f * density + dotsWidth
        
        val itemStartX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (width - totalItemWidth) / 2f
            HorizontalAlignment.END -> width - totalItemWidth
        }
        
        // Draw language name
        canvas.drawText(item.name, itemStartX, currentY + languageTextPaint.textSize, languageTextPaint)
        
        // Draw dots
        var dotX = itemStartX + textWidth + 8f * density
        val proficiency = item.proficiency.coerceIn(0f, 1f)
        val filledDots = (proficiency * element.maxDots).toInt().coerceIn(0, element.maxDots)
        
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
        
        currentY += itemHeight
    }
}

/**
 * Tags layout: Chip-style tags with proficiency
 */
private fun drawTagsLanguageLayout(
    canvas: android.graphics.Canvas,
    element: ResumeElement.LanguageElement,
    context: android.content.Context,
    width: Float,
    height: Float,
    density: Float
) {
    val languageTextPaint = createTextPaint(element.languageStyle, context)
    val proficiencyTextPaint = createTextPaint(element.proficiencyLabelStyle, context)
    val spacing = element.spacing * density
    val tagPadding = 12f * density
    val tagRadius = element.tagCornerRadius * density
    
    val tagBackgroundPaint = Paint().apply {
        color = (element.tagBackgroundColor ?: 0xFFE3F2FD.toInt()).toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val tagBorderPaint = if (element.tagBorderColor != null && element.tagBorderWidth > 0) {
        Paint().apply {
            color = element.tagBorderColor.toInt()
            style = Paint.Style.STROKE
            strokeWidth = element.tagBorderWidth * density
            isAntiAlias = true
        }
    } else null
    
    // Group tags into rows
    data class TagInfo(val name: String, val proficiencyText: String, val width: Float, val height: Float)
    val rows = mutableListOf<MutableList<TagInfo>>()
    var currentRow = mutableListOf<TagInfo>()
    var currentRowWidth = 0f
    
    element.items.forEach { item ->
        if (item.name.isEmpty()) return@forEach
        
        val proficiencyText = getProficiencyText(item, element.proficiencyType)
        val nameWidth = languageTextPaint.measureText(item.name)
        val proficiencyWidth = if (proficiencyText.isNotEmpty()) proficiencyTextPaint.measureText(proficiencyText) else 0f
        val textWidth = nameWidth.coerceAtLeast(proficiencyWidth)
        
        val tagWidth = textWidth + tagPadding * 2
        val tagHeight = languageTextPaint.textSize + 
            (if (proficiencyText.isNotEmpty()) proficiencyTextPaint.textSize + 2f * density else 0f) + 
            tagPadding * 2
        
        if (currentRowWidth + tagWidth > width && currentRow.isNotEmpty()) {
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
    
    val maxRowHeight = rows.flatMap { it.map { tag -> tag.height } }.maxOrNull() ?: 0f
    val totalHeight = (maxRowHeight * rows.size) + if (rows.size > 1) (spacing * (rows.size - 1)) else 0f
    
    // Apply vertical alignment
    val startY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
        VerticalAlignment.TOP -> 0f
        VerticalAlignment.CENTER -> (height - totalHeight) / 2f
        VerticalAlignment.BOTTOM -> height - totalHeight
    }
    
    var currentY = startY
    var isFirstRow = true
    rows.forEach { row ->
        if (!isFirstRow) {
            currentY += spacing
        }
        isFirstRow = false
        
        val rowWidth = row.sumOf { it.width.toDouble() }.toFloat() + 
                      if (row.size > 1) (spacing * (row.size - 1)) else 0f
        
        var currentX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (width - rowWidth) / 2f
            HorizontalAlignment.END -> width - rowWidth
        }
        
        var isFirstTag = true
        row.forEach { tag ->
            if (!isFirstTag) {
                currentX += spacing
            }
            isFirstTag = false
            
            val rect = RectF(currentX, currentY, currentX + tag.width, currentY + tag.height)
            canvas.drawRoundRect(rect, tagRadius, tagRadius, tagBackgroundPaint)
            
            tagBorderPaint?.let {
                canvas.drawRoundRect(rect, tagRadius, tagRadius, it)
            }
            
            val textX = currentX + tagPadding
            var textY = currentY + tagPadding + languageTextPaint.textSize
            
            canvas.drawText(tag.name, textX, textY, languageTextPaint)
            
            if (tag.proficiencyText.isNotEmpty()) {
                textY += 2f * density + proficiencyTextPaint.textSize
                canvas.drawText(tag.proficiencyText, textX, textY, proficiencyTextPaint)
            }
            
            currentX += tag.width
        }
        
        currentY += maxRowHeight
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
 * Create text paint for drawing text on canvas
 */
private fun createTextPaint(
    textStyle: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextStyle,
    context: android.content.Context
): TextPaint {
    return TextPaint().apply {
        isAntiAlias = true
        val density = context.resources.displayMetrics.density
        textSize = textStyle.fontSize * density
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
