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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Renders a project element on the resume
 * Displays a list of projects with name, description, technologies, highlights, and links
 */
@Composable
fun ProjectElementRenderer(
    element: ResumeElement.ProjectElement,
    zoomLevel: Float = 1f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val density = context.resources.displayMetrics.density
            
            // Calculate base dimensions (unscaled) to ensure consistent line breaks
            val basePadding = 8f * density
            val baseWidth = (size.width / zoomLevel) - (basePadding * 2)
            val baseHeight = (size.height / zoomLevel) - (basePadding * 2)
            
            // Apply zoom via Canvas scaling
            nativeCanvas.save()
            nativeCanvas.scale(zoomLevel, zoomLevel)
            nativeCanvas.translate(basePadding, basePadding)
            
            // Clip to content bounds
            nativeCanvas.clipRect(0f, 0f, baseWidth, baseHeight)
            
            // Draw all project items with base dimensions
            var currentY = 0f
            element.items.forEachIndexed { index, item ->
                val itemHeight = drawProjectItem(
                    canvas = nativeCanvas,
                    item = item,
                    element = element,
                    context = context,
                    width = baseWidth,
                    yOffset = currentY,
                    density = density
                )
                currentY += itemHeight
                
                // Add spacing between items
                if (index < element.items.size - 1) {
                    currentY += element.spacing * density
                }
            }
            
            nativeCanvas.restore()
        }
    }
}

/**
 * Draw a single project item using Canvas
 */
private fun drawProjectItem(
    canvas: android.graphics.Canvas,
    item: ProjectItem,
    element: ResumeElement.ProjectElement,
    context: android.content.Context,
    width: Float,
    yOffset: Float,
    density: Float
): Float {
    var currentY = yOffset
    val itemSpacing = element.itemSpacing * density
    
    when (element.displayStyle) {
        ProjectDisplayStyle.STANDARD -> {
            // Project Name and Date Row
            if (item.name.isNotEmpty()) {
                val nameHeight = drawNameAndDateRow(
                    canvas, item, element, context, width, currentY, density
                )
                currentY += nameHeight + itemSpacing
            }
            
            // Description
            if (element.showDescription && item.description.isNotEmpty()) {
                val descHeight = drawText(
                    canvas, item.description, element.descriptionStyle, 
                    context, width, currentY, TextAlignment.LEFT
                )
                currentY += descHeight + itemSpacing
            }
            
            // Technologies
            if (element.showTechnologies && item.technologies.isNotEmpty()) {
                val techHeight = drawTechnologyTags(
                    canvas, item.technologies, element, context, width, currentY, density
                )
                currentY += techHeight + itemSpacing
            }
            
            // Highlights
            if (item.highlights.isNotEmpty()) {
                val highlightHeight = drawHighlights(
                    canvas, item.highlights, element, context, width, currentY, density
                )
                currentY += highlightHeight + itemSpacing
            }
            
            // Link
            if (element.showLink && item.link.isNotEmpty()) {
                val linkHeight = drawText(
                    canvas, item.link, element.linkStyle,
                    context, width, currentY, TextAlignment.LEFT
                )
                currentY += linkHeight + itemSpacing
            }
        }
        ProjectDisplayStyle.COMPACT -> {
            // Name and Date
            if (item.name.isNotEmpty()) {
                val nameHeight = drawNameAndDateRow(
                    canvas, item, element, context, width, currentY, density
                )
                currentY += nameHeight + (itemSpacing / 2)
            }
            
            // Technologies (inline)
            if (element.showTechnologies && item.technologies.isNotEmpty()) {
                val techHeight = drawText(
                    canvas, item.technologies, element.technologyStyle,
                    context, width, currentY, TextAlignment.LEFT
                )
                currentY += techHeight + (itemSpacing / 2)
            }
            
            // Description (max 2 lines)
            if (element.showDescription && item.description.isNotEmpty()) {
                val descHeight = drawText(
                    canvas, item.description, element.descriptionStyle,
                    context, width, currentY, TextAlignment.LEFT, maxLines = 2
                )
                currentY += descHeight + (itemSpacing / 2)
            }
            
            // Highlights
            if (item.highlights.isNotEmpty()) {
                val highlightHeight = drawHighlights(
                    canvas, item.highlights, element, context, width, currentY, density
                )
                currentY += highlightHeight + (itemSpacing / 2)
            }
        }
        ProjectDisplayStyle.DETAILED -> {
            // Project Name
            if (item.name.isNotEmpty()) {
                val nameHeight = drawText(
                    canvas, item.name, element.nameStyle,
                    context, width, currentY, TextAlignment.LEFT
                )
                currentY += nameHeight + itemSpacing
            }
            
            // Date
            if (element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isOngoing)) {
                val dateText = formatDateRange(item, element)
                val dateHeight = drawText(
                    canvas, dateText, element.dateStyle,
                    context, width, currentY, TextAlignment.LEFT
                )
                currentY += dateHeight + itemSpacing
            }
            
            // Link
            if (element.showLink && item.link.isNotEmpty()) {
                val linkHeight = drawText(
                    canvas, item.link, element.linkStyle,
                    context, width, currentY, TextAlignment.LEFT
                )
                currentY += linkHeight + itemSpacing
            }
            
            // Technologies
            if (element.showTechnologies && item.technologies.isNotEmpty()) {
                val techHeight = drawTechnologyTags(
                    canvas, item.technologies, element, context, width, currentY, density
                )
                currentY += techHeight + itemSpacing
            }
            
            // Description
            if (element.showDescription && item.description.isNotEmpty()) {
                val descHeight = drawText(
                    canvas, item.description, element.descriptionStyle,
                    context, width, currentY, TextAlignment.LEFT
                )
                currentY += descHeight + itemSpacing
            }
            
            // Highlights
            if (item.highlights.isNotEmpty()) {
                val highlightHeight = drawHighlights(
                    canvas, item.highlights, element, context, width, currentY, density
                )
                currentY += highlightHeight + itemSpacing
            }
        }
    }
    
    return currentY - yOffset
}

/**
 * Draw name and date on the same row
 */
private fun drawNameAndDateRow(
    canvas: android.graphics.Canvas,
    item: ProjectItem,
    element: ResumeElement.ProjectElement,
    context: android.content.Context,
    width: Float,
    yOffset: Float,
    density: Float
): Float {
    val dateText = if (element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isOngoing)) {
        formatDateRange(item, element)
    } else ""
    
    if (dateText.isEmpty()) {
        // Only name, full width
        return drawText(canvas, item.name, element.nameStyle, context, width, yOffset, TextAlignment.LEFT)
    }
    
    // Calculate date width
    val datePaint = createTextPaint(element.dateStyle, context)
    val dateWidth = datePaint.measureText(dateText)
    val gap = 16f * density
    
    // Draw name on left
    val nameWidth = width - dateWidth - gap
    val nameHeight = drawText(canvas, item.name, element.nameStyle, context, nameWidth, yOffset, TextAlignment.LEFT)
    
    // Draw date on right
    canvas.save()
    canvas.translate(width - dateWidth, yOffset)
    drawText(canvas, dateText, element.dateStyle, context, dateWidth, 0f, TextAlignment.RIGHT)
    canvas.restore()
    
    return nameHeight
}

/**
 * Draw text with proper layout (similar to TextElementRenderer)
 */
private fun drawText(
    canvas: android.graphics.Canvas,
    text: String,
    textStyle: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextStyle,
    context: android.content.Context,
    width: Float,
    yOffset: Float,
    alignment: TextAlignment,
    maxLines: Int? = null
): Float {
    val paint = createTextPaint(textStyle, context)
    val layout = createTextLayout(text, paint, width.toInt().coerceAtLeast(1), alignment, maxLines)
    
    canvas.save()
    canvas.translate(0f, yOffset)
    layout.draw(canvas)
    canvas.restore()
    
    return layout.height.toFloat()
}

/**
 * Draw technology tags
 */
private fun drawTechnologyTags(
    canvas: android.graphics.Canvas,
    technologies: String,
    element: ResumeElement.ProjectElement,
    context: android.content.Context,
    width: Float,
    yOffset: Float,
    density: Float
): Float {
    val techList = technologies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val paint = createTextPaint(element.technologyStyle, context)
    
    val tagPadding = 8f * density
    val tagSpacing = element.technologySpacing * density
    val lineSpacing = (element.technologySpacing / 2) * density
    
    var currentX = 0f
    var currentY = yOffset
    var lineHeight = 0f
    
    techList.forEach { tech ->
        val textWidth = paint.measureText(tech)
        val tagWidth = textWidth + (tagPadding * 2)
        val tagHeight = paint.textSize + (tagPadding * 2)
        
        // Wrap to next line if needed
        if (currentX + tagWidth > width && currentX > 0) {
            currentX = 0f
            currentY += lineHeight + lineSpacing
            lineHeight = 0f
        }
        
        // Draw tag background
        val bgColor = element.technologyTagBackgroundColor
        if (bgColor != null) {
            val bgPaint = Paint().apply {
                color = bgColor.toInt()
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val cornerRadius = element.technologyTagCornerRadius * density
            canvas.drawRoundRect(
                currentX, currentY, currentX + tagWidth, currentY + tagHeight,
                cornerRadius, cornerRadius, bgPaint
            )
        }
        
        // Draw tag border
        val borderColor = element.technologyTagBorderColor
        if (borderColor != null && element.technologyTagBorderWidth > 0) {
            val borderPaint = Paint().apply {
                color = borderColor.toInt()
                style = Paint.Style.STROKE
                strokeWidth = element.technologyTagBorderWidth * density
                isAntiAlias = true
            }
            val cornerRadius = element.technologyTagCornerRadius * density
            canvas.drawRoundRect(
                currentX, currentY, currentX + tagWidth, currentY + tagHeight,
                cornerRadius, cornerRadius, borderPaint
            )
        }
        
        // Draw text
        canvas.drawText(tech, currentX + tagPadding, currentY + tagPadding - paint.ascent(), paint)
        
        currentX += tagWidth + tagSpacing
        lineHeight = lineHeight.coerceAtLeast(tagHeight)
    }
    
    return currentY + lineHeight - yOffset
}

/**
 * Draw highlights (bullet points)
 */
private fun drawHighlights(
    canvas: android.graphics.Canvas,
    highlights: List<ProjectHighlight>,
    element: ResumeElement.ProjectElement,
    context: android.content.Context,
    width: Float,
    yOffset: Float,
    density: Float
): Float {
    val paint = createTextPaint(element.highlightStyle, context)
    val bulletGap = 4f * density
    val highlightSpacing = element.highlightSpacing * density
    
    var currentY = yOffset
    
    highlights.forEachIndexed { index, highlight ->
        if (highlight.text.isNotEmpty()) {
            val bullet = getBulletCharacter(element.bulletStyle, highlight, index)
            val bulletWidth = if (bullet.isEmpty()) 0f else paint.measureText(bullet) + bulletGap
            
            // Draw bullet
            if (bullet.isNotEmpty()) {
                canvas.drawText(bullet, 0f, currentY - paint.ascent(), paint)
            }
            
            // Draw highlight text
            val textWidth = width - bulletWidth
            val layout = createTextLayout(
                highlight.text, paint, textWidth.toInt().coerceAtLeast(1),
                TextAlignment.LEFT, null
            )
            
            canvas.save()
            canvas.translate(bulletWidth, currentY)
            layout.draw(canvas)
            canvas.restore()
            
            currentY += layout.height + highlightSpacing
        }
    }
    
    return currentY - yOffset
}

/**
 * Create text paint with styling (no zoom applied, zoom handled by canvas scale)
 */
private fun createTextPaint(
    textStyle: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextStyle,
    context: android.content.Context
): TextPaint {
    val density = context.resources.displayMetrics.density
    return TextPaint().apply {
        isAntiAlias = true
        textSize = textStyle.fontSize * density
        color = textStyle.color.toInt()
        
        typeface = com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager.getPoppinsTypeface(
            context,
            textStyle.fontWeight,
            textStyle.isItalic
        )
        
        isUnderlineText = textStyle.isUnderlined
        letterSpacing = textStyle.letterSpacing
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
    maxLines: Int?
): StaticLayout {
    val layoutAlignment = when (alignment) {
        TextAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
        TextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
        TextAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        TextAlignment.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL
    }
    
    val builder = StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
        .setAlignment(layoutAlignment)
        .setLineSpacing(0f, 1.0f)
        .setIncludePad(false)
        .setMaxLines(maxLines ?: Int.MAX_VALUE)
    
    if (alignment == TextAlignment.JUSTIFY && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        builder.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD)
    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        builder.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_NONE)
    }
    
    return builder.build()
}

/**
 * Get bullet character based on bullet style
 */
private fun getBulletCharacter(
    bulletStyle: BulletStyle,
    highlight: ProjectHighlight,
    index: Int
): String {
    return when (bulletStyle) {
        BulletStyle.DISC -> "•"
        BulletStyle.DASH -> "-"
        BulletStyle.ARROW -> "→"
        BulletStyle.CHEVRON -> "›"
        BulletStyle.NUMBERED -> "${index + 1}."
        BulletStyle.CUSTOM_ICON -> highlight.customBullet ?: "•"
        BulletStyle.NONE -> ""
    }
}

/**
 * Format date range for a project
 */
private fun formatDateRange(
    item: ProjectItem,
    element: ResumeElement.ProjectElement
): String {
    val startDate = if (item.startDate.isNotEmpty()) {
        formatDate(item.startDate, element.dateFormat)
    } else ""

    val endDate = when {
        item.isOngoing -> "Present"
        item.endDate.isNotEmpty() -> formatDate(item.endDate, element.dateFormat)
        else -> ""
    }

    return when {
        startDate.isNotEmpty() && endDate.isNotEmpty() -> "$startDate${element.dateSeparator}$endDate"
        startDate.isNotEmpty() -> startDate
        endDate.isNotEmpty() -> endDate
        else -> ""
    }
}

/**
 * Format a single date string
 */
private fun formatDate(dateString: String, dateFormat: DateFormat): String {
    if (dateString.isEmpty()) return ""

    return try {
        val date = LocalDate.parse(dateString)
        when (dateFormat) {
            DateFormat.MMM_YYYY -> date.format(DateTimeFormatter.ofPattern("MMM yyyy"))
            DateFormat.MM_YYYY -> date.format(DateTimeFormatter.ofPattern("MM/yyyy"))
            DateFormat.FULL -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            DateFormat.SHORT -> date.format(DateTimeFormatter.ofPattern("M/yy"))
            DateFormat.YYYY -> date.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    } catch (e: Exception) {
        dateString
    }
}

/**
 * Convert custom TextStyle to Compose TextStyle (kept for compatibility)
 */
private fun com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextStyle.toComposeTextStyle(
    zoomLevel: Float = 1f
): TextStyle {
    return TextStyle(
        fontSize = (fontSize * zoomLevel).sp,
        fontWeight = fontWeight,
        fontFamily = com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager.poppinsFontFamily,
        color = Color(color),
        lineHeight = lineHeight?.let { (it * zoomLevel).sp } ?: TextUnit.Unspecified,
        letterSpacing = (letterSpacing * zoomLevel).sp,
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None
    )
}
