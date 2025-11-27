package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import android.graphics.Paint
import android.graphics.RectF
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
// TextStyle conflict resolved - using fully qualified names
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Renders a work experience element on the resume
 * Displays a list of work experiences with job titles, companies, dates, and responsibilities
 * Uses Canvas rendering for proper zoom handling without text line wrap issues
 */
@Composable
fun WorkExperienceElementRenderer(
    element: ResumeElement.WorkExperienceElement,
    zoomLevel: Float = 1f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Canvas(modifier = modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val bounds = RectF(0f, 0f, size.width, size.height)

            // Draw background, border, shadow
            drawElementStyle(nativeCanvas, element.style, bounds, zoomLevel)

            // Calculate base dimensions (unscaled) for consistent text layout
            val density = context.resources.displayMetrics.density
            val safePadding = element.padding ?: Padding(8f, 8f, 8f, 8f)
            val paddingLeft = safePadding.left * density
            val paddingTop = safePadding.top * density
            val paddingRight = safePadding.right * density
            val paddingBottom = safePadding.bottom * density

            val baseWidth = (size.width / zoomLevel) - (paddingLeft + paddingRight)
            val baseHeight = (size.height / zoomLevel) - (paddingTop + paddingBottom)

            // Apply zoom via Canvas scaling
            nativeCanvas.save()
            nativeCanvas.scale(zoomLevel, zoomLevel)
            nativeCanvas.translate(paddingLeft, paddingTop)

            // Clip to content bounds
            nativeCanvas.clipRect(0f, 0f, baseWidth, baseHeight)

            // Render based on orientation
            when (element.orientation) {
                WorkExperienceOrientation.VERTICAL -> {
                    renderVerticalLayout(
                        nativeCanvas,
                        element,
                        context,
                        baseWidth,
                        baseHeight,
                        density
                    )
                }
                WorkExperienceOrientation.HORIZONTAL -> {
                    renderHorizontalLayout(
                        nativeCanvas,
                        element,
                        context,
                        baseWidth,
                        baseHeight,
                        density
                    )
                }
            }

            nativeCanvas.restore()
        }
    }
}

/**
 * Get bullet character based on bullet style
 */
private fun getBulletCharacter(
    bulletStyle: BulletStyle,
    index: Int,
    responsibility: ResponsibilityItem
): String {
    return when (bulletStyle) {
        BulletStyle.DISC -> "•"
        BulletStyle.DASH -> "-"
        BulletStyle.ARROW -> "→"
        BulletStyle.CHEVRON -> "›"
        BulletStyle.NUMBERED -> "${index + 1}."
        BulletStyle.CUSTOM_ICON -> responsibility.customBullet ?: "•"
        BulletStyle.NONE -> ""
    }
}

/**
 * Format date range for display
 */
private fun formatDateRange(
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement
): String {
    val start = formatDate(item.startDate, element.dateFormat)
    val end = if (item.isCurrentRole) "Present" else formatDate(item.endDate, element.dateFormat)

    return when {
        start.isNotEmpty() && end.isNotEmpty() -> "$start${element.dateSeparator}$end"
        start.isNotEmpty() -> start
        end.isNotEmpty() -> end
        else -> ""
    }
}

/**
 * Format a single date string based on date format
 */
private fun formatDate(dateString: String, dateFormat: DateFormat): String {
    if (dateString.isEmpty()) return ""

    return try {
        // Try to parse as LocalDate (ISO format: yyyy-MM-dd)
        val date = LocalDate.parse(dateString)

        when (dateFormat) {
            DateFormat.MMM_YYYY -> date.format(DateTimeFormatter.ofPattern("MMM yyyy"))
            DateFormat.MM_YYYY -> date.format(DateTimeFormatter.ofPattern("MM/yyyy"))
            DateFormat.FULL -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            DateFormat.SHORT -> date.format(DateTimeFormatter.ofPattern("M/yy"))
            DateFormat.YYYY -> date.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    } catch (e: Exception) {
        // If parsing fails, return the original string (might be pre-formatted)
        dateString
    }
}

/**
 * Converts custom TextStyle to Compose TextStyle
 * DEPRECATED: This function causes zoom text wrap issues. Use Canvas rendering instead.
 */
@Deprecated("Use Canvas rendering with createTextPaint instead")
private fun com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.TextStyle.toComposeTextStyle(zoomLevel: Float = 1f): androidx.compose.ui.text.TextStyle {
    return androidx.compose.ui.text.TextStyle(
        fontSize = (fontSize * zoomLevel).sp,
        fontWeight = fontWeight,
        fontFamily = FontManager.poppinsFontFamily,
        color = Color(color),
        lineHeight = lineHeight?.let { (it * zoomLevel).sp } ?: androidx.compose.ui.unit.TextUnit.Unspecified,
        letterSpacing = (letterSpacing * zoomLevel).sp,
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None
    )
}

/**
 * Draw element background, border, and shadow
 */
private fun drawElementStyle(
    canvas: android.graphics.Canvas,
    style: ElementStyle,
    bounds: RectF,
    zoomLevel: Float
) {
    val paint = Paint().apply {
        isAntiAlias = true
    }

    // Draw shadow
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

        val shadowBounds = RectF(bounds)
        shadowBounds.offset(style.shadowOffsetX * zoomLevel, style.shadowOffsetY * zoomLevel)

        if (style.borderRadius > 0f) {
            val radius = style.borderRadius * zoomLevel
            canvas.drawRoundRect(shadowBounds, radius, radius, shadowPaint)
        } else {
            canvas.drawRect(shadowBounds, shadowPaint)
        }
    }

    // Draw background
    if (style.backgroundColor != null && Color(style.backgroundColor).alpha > 0f) {
        paint.color = style.backgroundColor.toInt()
        paint.alpha = (style.opacity * 255).toInt()
        paint.style = Paint.Style.FILL

        if (style.borderRadius > 0f) {
            val radius = style.borderRadius * zoomLevel
            canvas.drawRoundRect(bounds, radius, radius, paint)
        } else {
            canvas.drawRect(bounds, paint)
        }
    }

    // Draw border
    if (style.borderColor != null && style.borderWidth > 0f) {
        paint.color = style.borderColor.toInt()
        paint.alpha = (style.opacity * 255).toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = style.borderWidth * zoomLevel

        if (style.borderRadius > 0f) {
            val radius = style.borderRadius * zoomLevel
            canvas.drawRoundRect(bounds, radius, radius, paint)
        } else {
            canvas.drawRect(bounds, paint)
        }
    }
}

/**
 * Create text paint with styling
 */
private fun createTextPaint(
    textStyle: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.TextStyle,
    context: android.content.Context
): TextPaint {
    val density = context.resources.displayMetrics.density
    return TextPaint().apply {
        isAntiAlias = true
        // fontSize is stored as SP value, convert to pixels
        textSize = textStyle.fontSize * density
        color = textStyle.color.toInt()

        // Use Poppins font with proper weight mapping
        typeface = FontManager.getPoppinsTypeface(context, textStyle)

        // Underline
        isUnderlineText = textStyle.isUnderlined

        // Letter spacing
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
    alignment: HorizontalAlignment
): StaticLayout {
    val layoutAlignment = when (alignment) {
        HorizontalAlignment.START -> Layout.Alignment.ALIGN_NORMAL
        HorizontalAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
        HorizontalAlignment.END -> Layout.Alignment.ALIGN_OPPOSITE
    }

    return StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
        .setAlignment(layoutAlignment)
        .setLineSpacing(0f, 1f)
        .setIncludePad(false)
        .build()
}

/**
 * Measure actual text width using Paint
 * This is more accurate than StaticLayout.width for single-line text
 */
private fun measureTextWidth(
    text: String,
    paint: TextPaint
): Float {
    return paint.measureText(text)
}

/**
 * Render items in vertical layout using Canvas
 */
private fun renderVerticalLayout(
    canvas: android.graphics.Canvas,
    element: ResumeElement.WorkExperienceElement,
    context: android.content.Context,
    width: Float,
    height: Float,
    density: Float
) {
    var currentY = 0f
    val spacing = element.spacing * density

    // Calculate vertical alignment offset
    val totalHeight = calculateTotalVerticalHeight(element, context, width, density)
    val startY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
        VerticalAlignment.TOP -> 0f
        VerticalAlignment.CENTER -> (height - totalHeight) / 2f
        VerticalAlignment.BOTTOM -> height - totalHeight
    }

    currentY = startY

    element.items.forEachIndexed { index, item ->
        val itemHeight = renderWorkExperienceItem(
            canvas,
            item,
            element,
            context,
            width,
            currentY,
            density
        )

        currentY += itemHeight

        // Add spacing between items (but not after last)
        if (index < element.items.size - 1) {
            currentY += spacing
        }
    }
}

/**
 * Render items in horizontal layout using Canvas
 */
private fun renderHorizontalLayout(
    canvas: android.graphics.Canvas,
    element: ResumeElement.WorkExperienceElement,
    context: android.content.Context,
    width: Float,
    height: Float,
    density: Float
) {
    val spacing = element.spacing * density
    val itemCount = element.items.size

    if (itemCount == 0) return

    // Calculate width per item (equal distribution)
    val totalSpacing = spacing * (itemCount - 1)
    val itemWidth = (width - totalSpacing) / itemCount

    var currentX = 0f

    // Calculate horizontal alignment offset
    val contentWidth = itemWidth * itemCount + totalSpacing
    val horizontalOffset = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
        HorizontalAlignment.START -> 0f
        HorizontalAlignment.CENTER -> (width - contentWidth) / 2f
        HorizontalAlignment.END -> width - contentWidth
    }

    currentX = horizontalOffset

    element.items.forEachIndexed { index, item ->
        canvas.save()
        canvas.translate(currentX, 0f)
        canvas.clipRect(0f, 0f, itemWidth, height)

        renderWorkExperienceItem(
            canvas,
            item,
            element,
            context,
            itemWidth,
            0f,
            density
        )

        canvas.restore()

        currentX += itemWidth

        // Add spacing between items (but not after last)
        if (index < element.items.size - 1) {
            currentX += spacing
        }
    }
}

/**
 * Render a single work experience item
 * Returns the height consumed by this item
 */
private fun renderWorkExperienceItem(
    canvas: android.graphics.Canvas,
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement,
    context: android.content.Context,
    width: Float,
    y: Float,
    density: Float
): Float {
    canvas.save()
    canvas.translate(0f, y)

    val height = when (element.displayStyle) {
        WorkExperienceDisplayStyle.STANDARD -> {
            renderStandardLayout(canvas, item, element, context, width, density)
        }
        WorkExperienceDisplayStyle.COMPACT -> {
            renderCompactLayout(canvas, item, element, context, width, density)
        }
        WorkExperienceDisplayStyle.DETAILED -> {
            renderDetailedLayout(canvas, item, element, context, width, density)
        }
    }

    canvas.restore()
    return height
}

/**
 * Render STANDARD layout: Title and company on separate lines
 * Returns total height
 */
private fun renderStandardLayout(
    canvas: android.graphics.Canvas,
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement,
    context: android.content.Context,
    width: Float,
    density: Float
): Float {
    var currentY = 0f
    val itemSpacing = element.itemSpacing * density

    // 1. Job Title
    if (item.jobTitle.isNotEmpty()) {
        val titlePaint = createTextPaint(element.titleStyle, context)
        val titleLayout = createTextLayout(
            item.jobTitle,
            titlePaint,
            width.toInt(),
            element.horizontalAlignment ?: HorizontalAlignment.START
        )

        canvas.save()
        canvas.translate(0f, currentY)
        titleLayout.draw(canvas)
        canvas.restore()

        currentY += titleLayout.height + itemSpacing
    }

    // 2. Company and Date Row
    val hasCompany = item.company.isNotEmpty()
    val hasDate = element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isCurrentRole)

    if (hasCompany || hasDate) {
        val rowHeight = renderCompanyDateRow(
            canvas,
            item,
            element,
            context,
            width,
            currentY,
            density
        )
        currentY += rowHeight + itemSpacing
    }

    // 3. Location
    if (element.showLocation && item.location.isNotEmpty()) {
        val locationPaint = createTextPaint(element.locationStyle, context)
        val locationLayout = createTextLayout(
            item.location,
            locationPaint,
            width.toInt(),
            element.horizontalAlignment ?: HorizontalAlignment.START
        )

        canvas.save()
        canvas.translate(0f, currentY)
        locationLayout.draw(canvas)
        canvas.restore()

        currentY += locationLayout.height + itemSpacing
    }

    // 4. Responsibilities
    if (item.responsibilities.isNotEmpty()) {
        val responsibilitiesHeight = renderResponsibilityList(
            canvas,
            item.responsibilities,
            element,
            context,
            width,
            currentY,
            density
        )
        currentY += responsibilitiesHeight
    }

    return currentY
}

/**
 * Render COMPACT layout: Title+Company on same line
 * Returns total height
 */
private fun renderCompactLayout(
    canvas: android.graphics.Canvas,
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement,
    context: android.content.Context,
    width: Float,
    density: Float
): Float {
    var currentY = 0f
    val itemSpacing = element.itemSpacing * density

    // 1. Title + Company Row with Date
    val hasTitleOrCompany = item.jobTitle.isNotEmpty() || item.company.isNotEmpty()
    val hasDate = element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isCurrentRole)

    if (hasTitleOrCompany || hasDate) {
        val titleCompany = buildString {
            if (item.jobTitle.isNotEmpty()) append(item.jobTitle)
            if (item.jobTitle.isNotEmpty() && item.company.isNotEmpty()) append(" at ")
            if (item.company.isNotEmpty()) append(item.company)
        }

        val rowHeight = renderTitleCompanyDateRow(
            canvas,
            titleCompany,
            item,
            element,
            context,
            width,
            currentY,
            density
        )
        currentY += rowHeight + itemSpacing
    }

    // 2. Location
    if (element.showLocation && item.location.isNotEmpty()) {
        val locationPaint = createTextPaint(element.locationStyle, context)
        val locationLayout = createTextLayout(
            item.location,
            locationPaint,
            width.toInt(),
            element.horizontalAlignment ?: HorizontalAlignment.START
        )

        canvas.save()
        canvas.translate(0f, currentY)
        locationLayout.draw(canvas)
        canvas.restore()

        currentY += locationLayout.height + itemSpacing
    }

    // 3. Responsibilities
    if (item.responsibilities.isNotEmpty()) {
        val responsibilitiesHeight = renderResponsibilityList(
            canvas,
            item.responsibilities,
            element,
            context,
            width,
            currentY,
            density
        )
        currentY += responsibilitiesHeight
    }

    return currentY
}

/**
 * Render DETAILED layout: All fields prominently displayed
 * Returns total height
 */
private fun renderDetailedLayout(
    canvas: android.graphics.Canvas,
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement,
    context: android.content.Context,
    width: Float,
    density: Float
): Float {
    var currentY = 0f
    val itemSpacing = element.itemSpacing * density

    // 1. Job Title
    if (item.jobTitle.isNotEmpty()) {
        val titlePaint = createTextPaint(element.titleStyle, context)
        val titleLayout = createTextLayout(
            item.jobTitle,
            titlePaint,
            width.toInt(),
            element.horizontalAlignment ?: HorizontalAlignment.START
        )

        canvas.save()
        canvas.translate(0f, currentY)
        titleLayout.draw(canvas)
        canvas.restore()

        currentY += titleLayout.height + itemSpacing
    }

    // 2. Company
    if (item.company.isNotEmpty()) {
        val companyPaint = createTextPaint(element.companyStyle, context)
        val companyLayout = createTextLayout(
            item.company,
            companyPaint,
            width.toInt(),
            element.horizontalAlignment ?: HorizontalAlignment.START
        )

        canvas.save()
        canvas.translate(0f, currentY)
        companyLayout.draw(canvas)
        canvas.restore()

        currentY += companyLayout.height + itemSpacing
    }

    // 3. Location and Date Row
    val hasLocation = element.showLocation && item.location.isNotEmpty()
    val hasDate = element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isCurrentRole)

    if (hasLocation || hasDate) {
        val rowHeight = renderLocationDateRow(
            canvas,
            item,
            element,
            context,
            width,
            currentY,
            density
        )
        currentY += rowHeight + itemSpacing
    }

    // 4. Responsibilities
    if (item.responsibilities.isNotEmpty()) {
        val responsibilitiesHeight = renderResponsibilityList(
            canvas,
            item.responsibilities,
            element,
            context,
            width,
            currentY,
            density
        )
        currentY += responsibilitiesHeight
    }

    return currentY
}

/**
 * Render company and date in a row (SpaceBetween arrangement)
 * Returns row height
 */
private fun renderCompanyDateRow(
    canvas: android.graphics.Canvas,
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement,
    context: android.content.Context,
    width: Float,
    y: Float,
    density: Float
): Float {
    val hasCompany = item.company.isNotEmpty()
    val hasDate = element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isCurrentRole)

    if (!hasCompany && !hasDate) return 0f

    val companyPaint = createTextPaint(element.companyStyle, context)
    val datePaint = createTextPaint(element.dateStyle, context)

    // Measure date first to know how much space company has
    var dateWidth = 0f
    var dateLayout: StaticLayout? = null
    if (hasDate) {
        val dateText = formatDateRange(item, element)

        // CRITICAL FIX: Measure actual text width, not layout width
        dateWidth = datePaint.measureText(dateText) + (4f * density) // Add buffer for padding/rounding

        // Create layout with measured width (not full container width)
        dateLayout = createTextLayout(
            dateText,
            datePaint,
            dateWidth.toInt().coerceAtLeast(1),
            HorizontalAlignment.END
        )
    }

    // Company gets remaining space (with 8dp gap)
    val gap = 8f * density
    val companyMaxWidth = if (hasDate) {
        (width - dateWidth - gap).coerceAtLeast(1f)
    } else {
        width
    }

    var maxHeight = 0f

    // Draw company (left aligned or aligned per element setting)
    if (hasCompany) {
        val companyLayout = createTextLayout(
            item.company,
            companyPaint,
            companyMaxWidth.toInt(),
            element.horizontalAlignment ?: HorizontalAlignment.START
        )

        canvas.save()
        canvas.translate(0f, y)
        companyLayout.draw(canvas)
        canvas.restore()

        maxHeight = companyLayout.height.toFloat()
    }

    // Draw date (right aligned)
    if (hasDate && dateLayout != null) {
        canvas.save()
        canvas.translate(width - dateWidth, y)
        dateLayout.draw(canvas)
        canvas.restore()

        maxHeight = maxOf(maxHeight, dateLayout.height.toFloat())
    }

    return maxHeight
}

/**
 * Render title+company and date in a row (for COMPACT style)
 * Returns row height
 */
private fun renderTitleCompanyDateRow(
    canvas: android.graphics.Canvas,
    titleCompany: String,
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement,
    context: android.content.Context,
    width: Float,
    y: Float,
    density: Float
): Float {
    val hasDate = element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isCurrentRole)

    val titlePaint = createTextPaint(element.titleStyle, context)
    val datePaint = createTextPaint(element.dateStyle, context)

    // Measure date first
    var dateWidth = 0f
    var dateLayout: StaticLayout? = null
    if (hasDate) {
        val dateText = formatDateRange(item, element)

        // CRITICAL FIX: Measure actual text width, not layout width
        dateWidth = datePaint.measureText(dateText) + (4f * density) // Add buffer for padding/rounding

        // Create layout with measured width (not full container width)
        dateLayout = createTextLayout(
            dateText,
            datePaint,
            dateWidth.toInt().coerceAtLeast(1),
            HorizontalAlignment.END
        )
    }

    // Title+Company gets remaining space
    val gap = 8f * density
    val titleMaxWidth = if (hasDate) {
        (width - dateWidth - gap).coerceAtLeast(1f)
    } else {
        width
    }

    var maxHeight = 0f

    // Draw title+company
    if (titleCompany.isNotEmpty()) {
        val titleLayout = createTextLayout(
            titleCompany,
            titlePaint,
            titleMaxWidth.toInt(),
            element.horizontalAlignment ?: HorizontalAlignment.START
        )

        canvas.save()
        canvas.translate(0f, y)
        titleLayout.draw(canvas)
        canvas.restore()

        maxHeight = titleLayout.height.toFloat()
    }

    // Draw date
    if (hasDate && dateLayout != null) {
        canvas.save()
        canvas.translate(width - dateWidth, y)
        dateLayout.draw(canvas)
        canvas.restore()

        maxHeight = maxOf(maxHeight, dateLayout.height.toFloat())
    }

    return maxHeight
}

/**
 * Render location and date in a row (for DETAILED style)
 * Returns row height
 */
private fun renderLocationDateRow(
    canvas: android.graphics.Canvas,
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement,
    context: android.content.Context,
    width: Float,
    y: Float,
    density: Float
): Float {
    val hasLocation = element.showLocation && item.location.isNotEmpty()
    val hasDate = element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isCurrentRole)

    if (!hasLocation && !hasDate) return 0f

    val locationPaint = createTextPaint(element.locationStyle, context)
    val datePaint = createTextPaint(element.dateStyle, context)

    // Measure date first
    var dateWidth = 0f
    var dateLayout: StaticLayout? = null
    if (hasDate) {
        val dateText = formatDateRange(item, element)

        // CRITICAL FIX: Measure actual text width, not layout width
        dateWidth = datePaint.measureText(dateText) + (4f * density) // Add buffer for padding/rounding

        // Create layout with measured width (not full container width)
        dateLayout = createTextLayout(
            dateText,
            datePaint,
            dateWidth.toInt().coerceAtLeast(1),
            HorizontalAlignment.END
        )
    }

    val gap = 8f * density
    val locationMaxWidth = if (hasDate) {
        (width - dateWidth - gap).coerceAtLeast(1f)
    } else {
        width
    }

    var maxHeight = 0f

    if (hasLocation) {
        val locationLayout = createTextLayout(
            item.location,
            locationPaint,
            locationMaxWidth.toInt(),
            element.horizontalAlignment ?: HorizontalAlignment.START
        )

        canvas.save()
        canvas.translate(0f, y)
        locationLayout.draw(canvas)
        canvas.restore()

        maxHeight = locationLayout.height.toFloat()
    }

    if (hasDate && dateLayout != null) {
        canvas.save()
        canvas.translate(width - dateWidth, y)
        dateLayout.draw(canvas)
        canvas.restore()

        maxHeight = maxOf(maxHeight, dateLayout.height.toFloat())
    }

    return maxHeight
}

/**
 * Render list of responsibilities with bullets
 * Returns total height
 */
private fun renderResponsibilityList(
    canvas: android.graphics.Canvas,
    responsibilities: List<ResponsibilityItem>,
    element: ResumeElement.WorkExperienceElement,
    context: android.content.Context,
    width: Float,
    startY: Float,
    density: Float
): Float {
    var currentY = 0f
    val responsibilitySpacing = element.responsibilitySpacing * density
    val bulletGap = 4f * density

    val responsibilityPaint = createTextPaint(element.responsibilityStyle, context)

    responsibilities.forEachIndexed { index, responsibility ->
        if (responsibility.text.isNotEmpty()) {
            val bullet = getBulletCharacter(element.bulletStyle, index, responsibility)

            // Measure bullet width
            val bulletWidth = if (bullet.isNotEmpty()) {
                responsibilityPaint.measureText(bullet) + bulletGap
            } else {
                0f
            }

            // Draw bullet
            if (bullet.isNotEmpty()) {
                canvas.drawText(bullet, 0f, startY + currentY + responsibilityPaint.textSize, responsibilityPaint)
            }

            // Draw responsibility text
            val textMaxWidth = (width - bulletWidth).coerceAtLeast(1f)
            val textLayout = createTextLayout(
                responsibility.text,
                responsibilityPaint,
                textMaxWidth.toInt(),
                element.horizontalAlignment ?: HorizontalAlignment.START
            )

            canvas.save()
            canvas.translate(bulletWidth, startY + currentY)
            textLayout.draw(canvas)
            canvas.restore()

            currentY += textLayout.height

            // Add spacing between responsibilities (but not after last)
            if (index < responsibilities.size - 1) {
                currentY += responsibilitySpacing
            }
        }
    }

    return currentY
}

/**
 * Calculate total height of all items for vertical alignment
 */
private fun calculateTotalVerticalHeight(
    element: ResumeElement.WorkExperienceElement,
    context: android.content.Context,
    width: Float,
    density: Float
): Float {
    var totalHeight = 0f
    val spacing = element.spacing * density

    element.items.forEachIndexed { index, item ->
        val itemHeight = calculateItemHeight(item, element, context, width, density)
        totalHeight += itemHeight

        if (index < element.items.size - 1) {
            totalHeight += spacing
        }
    }

    return totalHeight
}

/**
 * Calculate height of a single item
 */
private fun calculateItemHeight(
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement,
    context: android.content.Context,
    width: Float,
    density: Float
): Float {
    var height = 0f
    val itemSpacing = element.itemSpacing * density

    when (element.displayStyle) {
        WorkExperienceDisplayStyle.STANDARD -> {
            // Title
            if (item.jobTitle.isNotEmpty()) {
                val titlePaint = createTextPaint(element.titleStyle, context)
                val titleLayout = createTextLayout(item.jobTitle, titlePaint, width.toInt(), element.horizontalAlignment ?: HorizontalAlignment.START)
                height += titleLayout.height + itemSpacing
            }

            // Company/Date row
            val hasCompany = item.company.isNotEmpty()
            val hasDate = element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isCurrentRole)
            if (hasCompany || hasDate) {
                val companyPaint = createTextPaint(element.companyStyle, context)
                val dateText = if (hasDate) formatDateRange(item, element) else ""
                // Add buffer to match rendering functions
                val dateWidth = if (hasDate) createTextPaint(element.dateStyle, context).measureText(dateText) + (4f * density) else 0f
                val companyWidth = width - dateWidth - (if (hasDate) 8f * density else 0f)
                val companyLayout = createTextLayout(item.company, companyPaint, companyWidth.toInt().coerceAtLeast(1), element.horizontalAlignment ?: HorizontalAlignment.START)
                height += companyLayout.height + itemSpacing
            }

            // Location
            if (element.showLocation && item.location.isNotEmpty()) {
                val locationPaint = createTextPaint(element.locationStyle, context)
                val locationLayout = createTextLayout(item.location, locationPaint, width.toInt(), element.horizontalAlignment ?: HorizontalAlignment.START)
                height += locationLayout.height + itemSpacing
            }
        }
        WorkExperienceDisplayStyle.COMPACT -> {
            // Title+Company row
            val hasTitleOrCompany = item.jobTitle.isNotEmpty() || item.company.isNotEmpty()
            val hasDate = element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isCurrentRole)
            if (hasTitleOrCompany || hasDate) {
                val titleCompany = buildString {
                    if (item.jobTitle.isNotEmpty()) append(item.jobTitle)
                    if (item.jobTitle.isNotEmpty() && item.company.isNotEmpty()) append(" at ")
                    if (item.company.isNotEmpty()) append(item.company)
                }
                val titlePaint = createTextPaint(element.titleStyle, context)
                val dateText = if (hasDate) formatDateRange(item, element) else ""
                // Add buffer to match rendering functions
                val dateWidth = if (hasDate) createTextPaint(element.dateStyle, context).measureText(dateText) + (4f * density) else 0f
                val titleWidth = width - dateWidth - (if (hasDate) 8f * density else 0f)
                val titleLayout = createTextLayout(titleCompany, titlePaint, titleWidth.toInt().coerceAtLeast(1), element.horizontalAlignment ?: HorizontalAlignment.START)
                height += titleLayout.height + itemSpacing
            }

            // Location
            if (element.showLocation && item.location.isNotEmpty()) {
                val locationPaint = createTextPaint(element.locationStyle, context)
                val locationLayout = createTextLayout(item.location, locationPaint, width.toInt(), element.horizontalAlignment ?: HorizontalAlignment.START)
                height += locationLayout.height + itemSpacing
            }
        }
        WorkExperienceDisplayStyle.DETAILED -> {
            // Title
            if (item.jobTitle.isNotEmpty()) {
                val titlePaint = createTextPaint(element.titleStyle, context)
                val titleLayout = createTextLayout(item.jobTitle, titlePaint, width.toInt(), element.horizontalAlignment ?: HorizontalAlignment.START)
                height += titleLayout.height + itemSpacing
            }

            // Company
            if (item.company.isNotEmpty()) {
                val companyPaint = createTextPaint(element.companyStyle, context)
                val companyLayout = createTextLayout(item.company, companyPaint, width.toInt(), element.horizontalAlignment ?: HorizontalAlignment.START)
                height += companyLayout.height + itemSpacing
            }

            // Location/Date row
            val hasLocation = element.showLocation && item.location.isNotEmpty()
            val hasDate = element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isCurrentRole)
            if (hasLocation || hasDate) {
                val locationPaint = createTextPaint(element.locationStyle, context)
                val dateText = if (hasDate) formatDateRange(item, element) else ""
                // Add buffer to match rendering functions
                val dateWidth = if (hasDate) createTextPaint(element.dateStyle, context).measureText(dateText) + (4f * density) else 0f
                val locationWidth = width - dateWidth - (if (hasDate) 8f * density else 0f)
                val locationLayout = createTextLayout(item.location, locationPaint, locationWidth.toInt().coerceAtLeast(1), element.horizontalAlignment ?: HorizontalAlignment.START)
                height += locationLayout.height + itemSpacing
            }
        }
    }

    // Responsibilities
    if (item.responsibilities.isNotEmpty()) {
        val responsibilityPaint = createTextPaint(element.responsibilityStyle, context)
        val bulletGap = 4f * density
        val responsibilitySpacing = element.responsibilitySpacing * density

        item.responsibilities.forEachIndexed { index, responsibility ->
            if (responsibility.text.isNotEmpty()) {
                val bullet = getBulletCharacter(element.bulletStyle, index, responsibility)
                val bulletWidth = if (bullet.isNotEmpty()) responsibilityPaint.measureText(bullet) + bulletGap else 0f
                val textWidth = width - bulletWidth
                val textLayout = createTextLayout(responsibility.text, responsibilityPaint, textWidth.toInt().coerceAtLeast(1), element.horizontalAlignment ?: HorizontalAlignment.START)
                height += textLayout.height

                if (index < item.responsibilities.size - 1) {
                    height += responsibilitySpacing
                }
            }
        }
    }

    return height
}
