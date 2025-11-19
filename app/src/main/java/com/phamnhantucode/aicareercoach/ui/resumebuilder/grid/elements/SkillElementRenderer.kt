package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.*
import kotlin.math.roundToInt

/**
 * Renders a skill element on the resume
 * Displays a list of skills with various visual styles (tags, bars, list, etc.)
 * Uses Canvas rendering for proper zoom handling without clipping issues
 */
@Composable
fun SkillElementRenderer(
    element: ResumeElement.SkillElement,
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
                SkillDisplayStyle.LIST -> {
                    drawSkillList(nativeCanvas, element, context, baseWidth, baseHeight, density)
                }
                SkillDisplayStyle.TAGS -> {
                    drawSkillTags(nativeCanvas, element, context, baseWidth, baseHeight, density)
                }
                SkillDisplayStyle.PROGRESS_BARS -> {
                    drawSkillProgressBars(nativeCanvas, element, context, baseWidth, baseHeight, density)
                }
                SkillDisplayStyle.DOTS -> {
                    drawSkillDots(nativeCanvas, element, context, baseWidth, baseHeight, density)
                }
                SkillDisplayStyle.GROUPED -> {
                    drawSkillGrouped(nativeCanvas, element, context, baseWidth, baseHeight, density)
                }
            }
            
            nativeCanvas.restore()
        }
    }
}

/**
 * Draw skills as a simple list
 */
private fun drawSkillList(
    canvas: android.graphics.Canvas,
    element: ResumeElement.SkillElement,
    context: android.content.Context,
    width: Float,
    height: Float,
    density: Float
) {
    val textPaint = createTextPaint(element.skillStyle, context)
    val spacing = element.spacing * density
    
    // Calculate layouts for all items
    val layouts = mutableListOf<Pair<String, StaticLayout>>()
    var totalHeight = 0f
    
    element.items.forEach { item ->
        if (item.name.isNotEmpty()) {
            val bullet = if (element.showBullets) getBulletCharacter(element.bulletStyle) + " " else ""
            val text = bullet + item.name
            
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, width.toInt().coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            layouts.add(text to layout)
            totalHeight += layout.height.toFloat()
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
    layouts.forEach { (text, layout) ->
        val textWidth = textPaint.measureText(text)
        
        val xPosition = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (width - textWidth) / 2f
            HorizontalAlignment.END -> width - textWidth
        }
        
        canvas.save()
        canvas.translate(xPosition, currentY)
        layout.draw(canvas)
        canvas.restore()
        
        currentY += layout.height.toFloat() + spacing
    }
}

/**
 * Draw skills as tags/chips
 */
private fun drawSkillTags(
    canvas: android.graphics.Canvas,
    element: ResumeElement.SkillElement,
    context: android.content.Context,
    width: Float,
    height: Float,
    density: Float
) {
    val textPaint = createTextPaint(element.skillStyle, context)
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
    data class TagInfo(val name: String, val width: Float)
    val rows = mutableListOf<MutableList<TagInfo>>()
    var currentRow = mutableListOf<TagInfo>()
    var currentRowWidth = 0f
    
    element.items.forEach { item ->
        if (item.name.isEmpty()) return@forEach
        val textWidth = textPaint.measureText(item.name)
        val tagWidth = textWidth + tagPadding * 2
        
        if (currentRowWidth + tagWidth > width && currentRow.isNotEmpty()) {
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
    
    val rowHeight = textPaint.textSize + tagPadding * 2
    val totalHeight = (rowHeight * rows.size) + (spacing * (rows.size - 1))
    
    // Apply vertical alignment
    val startY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
        VerticalAlignment.TOP -> 0f
        VerticalAlignment.CENTER -> (height - totalHeight) / 2f
        VerticalAlignment.BOTTOM -> height - totalHeight
    }
    
    var currentY = startY
    rows.forEach { row ->
        val rowWidth = row.sumOf { it.width.toDouble() }.toFloat() + (spacing * (row.size - 1))
        
        var currentX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (width - rowWidth) / 2f
            HorizontalAlignment.END -> width - rowWidth
        }
        
        row.forEach { tag ->
            val rect = android.graphics.RectF(currentX, currentY, currentX + tag.width, currentY + rowHeight)
            canvas.drawRoundRect(rect, tagRadius, tagRadius, tagBackgroundPaint)
            
            tagBorderPaint?.let {
                canvas.drawRoundRect(rect, tagRadius, tagRadius, it)
            }
            
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
 * Draw skills with progress bars
 */
private fun drawSkillProgressBars(
    canvas: android.graphics.Canvas,
    element: ResumeElement.SkillElement,
    context: android.content.Context,
    width: Float,
    height: Float,
    density: Float
) {
    val textPaint = createTextPaint(element.skillStyle, context)
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
    var totalHeight = 0f
    element.items.forEach { item ->
        if (item.name.isNotEmpty()) {
            totalHeight += textPaint.textSize + 4f * density + barHeight + spacing
        }
    }
    totalHeight -= spacing
    
    // Apply vertical alignment
    var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
        VerticalAlignment.TOP -> 0f
        VerticalAlignment.CENTER -> (height - totalHeight) / 2f
        VerticalAlignment.BOTTOM -> height - totalHeight
    }
    
    // Apply horizontal alignment
    val columnStartX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
        HorizontalAlignment.START -> 0f
        HorizontalAlignment.CENTER -> (width - width) / 2f
        HorizontalAlignment.END -> 0f
    }
    
    element.items.forEach { item ->
        if (item.name.isEmpty()) return@forEach
        
        // Draw skill name
        canvas.drawText(item.name, columnStartX, currentY + textPaint.textSize, textPaint)
        currentY += textPaint.textSize + 4f * density
        
        // Draw progress bar background
        val barRect = android.graphics.RectF(columnStartX, currentY, columnStartX + width, currentY + barHeight)
        canvas.drawRoundRect(barRect, barRadius, barRadius, backgroundPaint)
        
        // Draw progress fill
        val proficiency = item.proficiency?.coerceIn(0f, 1f) ?: 0.5f
        val fillWidth = width * proficiency
        if (fillWidth > 0) {
            val fillRect = android.graphics.RectF(columnStartX, currentY, columnStartX + fillWidth, currentY + barHeight)
            canvas.drawRoundRect(fillRect, barRadius, barRadius, progressPaint)
        }
        
        currentY += barHeight + spacing
    }
}

/**
 * Draw skills with dots proficiency
 */
private fun drawSkillDots(
    canvas: android.graphics.Canvas,
    element: ResumeElement.SkillElement,
    context: android.content.Context,
    width: Float,
    height: Float,
    density: Float
) {
    val textPaint = createTextPaint(element.skillStyle, context)
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
    
    // Calculate layouts
    val layouts = mutableListOf<Pair<SkillItem, StaticLayout>>()
    var totalHeight = 0f
    
    element.items.forEach { item ->
        if (item.name.isNotEmpty()) {
            val layout = StaticLayout.Builder
                .obtain(item.name, 0, item.name.length, textPaint, width.toInt().coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            layouts.add(item to layout)
            totalHeight += layout.height.toFloat()
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
    
    layouts.forEach { (item, layout) ->
        val textWidth = textPaint.measureText(item.name)
        val dotsWidth = (dotSize * element.maxDots) + (dotSpacing * (element.maxDots - 1))
        val textDotSpacing = 8f * density
        val totalItemWidth = textWidth + textDotSpacing + dotsWidth
        val itemHeight = layout.height.toFloat()
        
        val itemStartX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (width - totalItemWidth) / 2f
            HorizontalAlignment.END -> width - totalItemWidth
        }
        
        // Draw skill name
        canvas.save()
        canvas.translate(itemStartX, currentY)
        layout.draw(canvas)
        canvas.restore()
        
        // Draw dots
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
        
        currentY += itemHeight + spacing
    }
}

/**
 * Draw skills grouped by category
 */
private fun drawSkillGrouped(
    canvas: android.graphics.Canvas,
    element: ResumeElement.SkillElement,
    context: android.content.Context,
    width: Float,
    height: Float,
    density: Float
) {
    val skillTextPaint = createTextPaint(element.skillStyle, context)
    val categoryTextPaint = createTextPaint(element.categoryStyle, context)
    val spacingPerItem = element.spacing * density
    val groupSpacing = element.groupSpacing * density
    
    // Calculate layouts for all groups
    val groups = element.items.groupBy { it.category }
    val groupLayouts = mutableListOf<Triple<String, StaticLayout?, List<Pair<String, StaticLayout>>>>()
    var totalHeight = 0f
    
    groups.entries.forEachIndexed { groupIndex, entry ->
        val category = entry.key
        val skills = entry.value
        
        var categoryLayout: StaticLayout? = null
        if (category.isNotEmpty()) {
            categoryLayout = StaticLayout.Builder
                .obtain(category, 0, category.length, categoryTextPaint, width.toInt().coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            totalHeight += categoryLayout.height.toFloat() + 4f * density
        }
        
        val skillLayouts = mutableListOf<Pair<String, StaticLayout>>()
        skills.forEach { item ->
            if (item.name.isNotEmpty()) {
                val bullet = if (element.showBullets) getBulletCharacter(element.bulletStyle) + " " else ""
                val text = bullet + item.name
                
                val layout = StaticLayout.Builder
                    .obtain(text, 0, text.length, skillTextPaint, width.toInt().coerceAtLeast(1))
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
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
        VerticalAlignment.CENTER -> (height - totalHeight) / 2f
        VerticalAlignment.BOTTOM -> height - totalHeight
    }
    
    groupLayouts.forEach { (category, categoryLayout, skillLayouts) ->
        // Draw category header
        if (categoryLayout != null) {
            val categoryWidth = categoryTextPaint.measureText(category)
            val xPosition = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                HorizontalAlignment.START -> 0f
                HorizontalAlignment.CENTER -> (width - categoryWidth) / 2f
                HorizontalAlignment.END -> width - categoryWidth
            }
            
            canvas.save()
            canvas.translate(xPosition, currentY)
            categoryLayout.draw(canvas)
            canvas.restore()
            
            currentY += categoryLayout.height.toFloat() + 4f * density
        }
        
        // Draw skills
        skillLayouts.forEach { (text, layout) ->
            val textWidth = skillTextPaint.measureText(text)
            
            val xPosition = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
                HorizontalAlignment.START -> 0f
                HorizontalAlignment.CENTER -> (width - textWidth) / 2f
                HorizontalAlignment.END -> width - textWidth
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
