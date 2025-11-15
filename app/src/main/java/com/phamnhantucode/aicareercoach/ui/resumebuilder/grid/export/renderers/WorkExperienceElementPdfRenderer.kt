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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Work Experience Element PDF Renderer
 *
 * Renders work experience entries with job titles, companies, dates, and responsibilities
 */
class WorkExperienceElementPdfRenderer : ElementPdfRenderer<ResumeElement.WorkExperienceElement> {

    override suspend fun render(
        canvas: Canvas,
        element: ResumeElement.WorkExperienceElement,
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

        // Create text paints for different styles
        val titlePaint = createTextPaint(element.titleStyle, element.style.opacity, mapper, context)
        val companyPaint = createTextPaint(element.companyStyle, element.style.opacity, mapper, context)
        val datePaint = createTextPaint(element.dateStyle, element.style.opacity, mapper, context)
        val locationPaint = createTextPaint(element.locationStyle, element.style.opacity, mapper, context)
        val responsibilityPaint = createTextPaint(element.responsibilityStyle, element.style.opacity, mapper, context)

        canvas.save()
        canvas.translate(contentBounds.left, contentBounds.top)
        canvas.clipRect(0f, 0f, contentBounds.width(), contentBounds.height())

        // Calculate layout based on orientation
        when (element.orientation) {
            WorkExperienceOrientation.VERTICAL -> {
                renderVerticalLayout(
                    canvas,
                    element,
                    contentBounds,
                    titlePaint,
                    companyPaint,
                    datePaint,
                    locationPaint,
                    responsibilityPaint,
                    mapper,
                    context
                )
            }
            WorkExperienceOrientation.HORIZONTAL -> {
                renderHorizontalLayout(
                    canvas,
                    element,
                    contentBounds,
                    titlePaint,
                    companyPaint,
                    datePaint,
                    locationPaint,
                    responsibilityPaint,
                    mapper,
                    context
                )
            }
        }

        canvas.restore()
    }

    /**
     * Render work experience items in vertical layout (stacked)
     */
    private fun renderVerticalLayout(
        canvas: Canvas,
        element: ResumeElement.WorkExperienceElement,
        bounds: RectF,
        titlePaint: TextPaint,
        companyPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        responsibilityPaint: TextPaint,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val itemSpacing = mapper.borderWidthToPdfPoints(element.itemSpacing)
        val responsibilitySpacing = mapper.borderWidthToPdfPoints(element.responsibilitySpacing)

        // Calculate vertical starting position based on vertical alignment
        val totalHeight = calculateTotalHeight(
            element, bounds, titlePaint, companyPaint, datePaint,
            locationPaint, responsibilityPaint, mapper
        )

        var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        element.items.forEachIndexed { index, item ->
            currentY += renderWorkItem(
                canvas,
                item,
                element,
                0f,
                currentY,
                bounds.width(),
                titlePaint,
                companyPaint,
                datePaint,
                locationPaint,
                responsibilityPaint,
                itemSpacing,
                responsibilitySpacing,
                mapper,
                context
            )

            // Add spacing between items (but not after last item)
            if (index < element.items.size - 1) {
                currentY += spacing
            }
        }
    }

    /**
     * Render work experience items in horizontal layout (columns)
     */
    private fun renderHorizontalLayout(
        canvas: Canvas,
        element: ResumeElement.WorkExperienceElement,
        bounds: RectF,
        titlePaint: TextPaint,
        companyPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        responsibilityPaint: TextPaint,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        if (element.items.isEmpty()) return

        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val itemSpacing = mapper.borderWidthToPdfPoints(element.itemSpacing)
        val responsibilitySpacing = mapper.borderWidthToPdfPoints(element.responsibilitySpacing)

        // Calculate column width
        val totalSpacingWidth = spacing * (element.items.size - 1)
        val columnWidth = (bounds.width() - totalSpacingWidth) / element.items.size

        var currentX = 0f

        element.items.forEachIndexed { index, item ->
            renderWorkItem(
                canvas,
                item,
                element,
                currentX,
                0f,
                columnWidth,
                titlePaint,
                companyPaint,
                datePaint,
                locationPaint,
                responsibilityPaint,
                itemSpacing,
                responsibilitySpacing,
                mapper,
                context
            )

            currentX += columnWidth + spacing
        }
    }

    /**
     * Render a single work experience item and return its height
     */
    private fun renderWorkItem(
        canvas: Canvas,
        item: WorkExperienceItem,
        element: ResumeElement.WorkExperienceElement,
        x: Float,
        y: Float,
        width: Float,
        titlePaint: TextPaint,
        companyPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        responsibilityPaint: TextPaint,
        itemSpacing: Float,
        responsibilitySpacing: Float,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): Float {
        var currentY = y

        when (element.displayStyle) {
            WorkExperienceDisplayStyle.STANDARD -> {
                currentY += renderStandardLayout(
                    canvas, item, element, x, currentY, width,
                    titlePaint, companyPaint, datePaint, locationPaint,
                    responsibilityPaint, itemSpacing, responsibilitySpacing
                )
            }
            WorkExperienceDisplayStyle.COMPACT -> {
                currentY += renderCompactLayout(
                    canvas, item, element, x, currentY, width,
                    titlePaint, companyPaint, datePaint, locationPaint,
                    responsibilityPaint, itemSpacing, responsibilitySpacing
                )
            }
            WorkExperienceDisplayStyle.DETAILED -> {
                currentY += renderDetailedLayout(
                    canvas, item, element, x, currentY, width,
                    titlePaint, companyPaint, datePaint, locationPaint,
                    responsibilityPaint, itemSpacing, responsibilitySpacing
                )
            }
        }

        return currentY - y
    }

    /**
     * Standard layout: Title and company on separate lines
     */
    private fun renderStandardLayout(
        canvas: Canvas,
        item: WorkExperienceItem,
        element: ResumeElement.WorkExperienceElement,
        x: Float,
        y: Float,
        width: Float,
        titlePaint: TextPaint,
        companyPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        responsibilityPaint: TextPaint,
        itemSpacing: Float,
        responsibilitySpacing: Float
    ): Float {
        var currentY = y

        // Job Title
        if (item.jobTitle.isNotEmpty()) {
            canvas.drawText(item.jobTitle, x, currentY + titlePaint.textSize, titlePaint)
            currentY += titlePaint.textSize + itemSpacing
        }

        // Company and Date Row
        if (item.company.isNotEmpty()) {
            val dateText = if (element.showDates) formatDateRange(item, element) else ""
            val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f

            canvas.drawText(item.company, x, currentY + companyPaint.textSize, companyPaint)

            if (dateText.isNotEmpty()) {
                canvas.drawText(dateText, x + width - dateWidth, currentY + datePaint.textSize, datePaint)
            }

            currentY += maxOf(companyPaint.textSize, datePaint.textSize) + itemSpacing
        } else if (element.showDates) {
            val dateText = formatDateRange(item, element)
            if (dateText.isNotEmpty()) {
                val dateWidth = datePaint.measureText(dateText)
                canvas.drawText(dateText, x + width - dateWidth, currentY + datePaint.textSize, datePaint)
                currentY += datePaint.textSize + itemSpacing
            }
        }

        // Location
        if (element.showLocation && item.location.isNotEmpty()) {
            canvas.drawText(item.location, x, currentY + locationPaint.textSize, locationPaint)
            currentY += locationPaint.textSize + itemSpacing
        }

        // Responsibilities
        if (item.responsibilities.isNotEmpty()) {
            currentY += renderResponsibilities(
                canvas, item.responsibilities, element, x, currentY, width,
                responsibilityPaint, responsibilitySpacing
            )
        }

        return currentY - y
    }

    /**
     * Compact layout: Title and company on same line
     */
    private fun renderCompactLayout(
        canvas: Canvas,
        item: WorkExperienceItem,
        element: ResumeElement.WorkExperienceElement,
        x: Float,
        y: Float,
        width: Float,
        titlePaint: TextPaint,
        companyPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        responsibilityPaint: TextPaint,
        itemSpacing: Float,
        responsibilitySpacing: Float
    ): Float {
        var currentY = y

        // Title + Company + Date Row
        val titleCompany = buildString {
            if (item.jobTitle.isNotEmpty()) append(item.jobTitle)
            if (item.jobTitle.isNotEmpty() && item.company.isNotEmpty()) append(" at ")
            if (item.company.isNotEmpty()) append(item.company)
        }

        if (titleCompany.isNotEmpty()) {
            val dateText = if (element.showDates) formatDateRange(item, element) else ""
            val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f

            canvas.drawText(titleCompany, x, currentY + titlePaint.textSize, titlePaint)

            if (dateText.isNotEmpty()) {
                canvas.drawText(dateText, x + width - dateWidth, currentY + datePaint.textSize, datePaint)
            }

            currentY += maxOf(titlePaint.textSize, datePaint.textSize) + itemSpacing
        }

        // Location
        if (element.showLocation && item.location.isNotEmpty()) {
            canvas.drawText(item.location, x, currentY + locationPaint.textSize, locationPaint)
            currentY += locationPaint.textSize + itemSpacing
        }

        // Responsibilities
        if (item.responsibilities.isNotEmpty()) {
            currentY += renderResponsibilities(
                canvas, item.responsibilities, element, x, currentY, width,
                responsibilityPaint, responsibilitySpacing
            )
        }

        return currentY - y
    }

    /**
     * Detailed layout: All fields prominently displayed
     */
    private fun renderDetailedLayout(
        canvas: Canvas,
        item: WorkExperienceItem,
        element: ResumeElement.WorkExperienceElement,
        x: Float,
        y: Float,
        width: Float,
        titlePaint: TextPaint,
        companyPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        responsibilityPaint: TextPaint,
        itemSpacing: Float,
        responsibilitySpacing: Float
    ): Float {
        var currentY = y

        // Job Title
        if (item.jobTitle.isNotEmpty()) {
            canvas.drawText(item.jobTitle, x, currentY + titlePaint.textSize, titlePaint)
            currentY += titlePaint.textSize + itemSpacing
        }

        // Company
        if (item.company.isNotEmpty()) {
            canvas.drawText(item.company, x, currentY + companyPaint.textSize, companyPaint)
            currentY += companyPaint.textSize + itemSpacing
        }

        // Location and Dates Row
        if (element.showLocation && item.location.isNotEmpty()) {
            val dateText = if (element.showDates) formatDateRange(item, element) else ""
            val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f

            canvas.drawText(item.location, x, currentY + locationPaint.textSize, locationPaint)

            if (dateText.isNotEmpty()) {
                canvas.drawText(dateText, x + width - dateWidth, currentY + datePaint.textSize, datePaint)
            }

            currentY += maxOf(locationPaint.textSize, datePaint.textSize) + itemSpacing
        } else if (element.showDates) {
            val dateText = formatDateRange(item, element)
            if (dateText.isNotEmpty()) {
                val dateWidth = datePaint.measureText(dateText)
                canvas.drawText(dateText, x + width - dateWidth, currentY + datePaint.textSize, datePaint)
                currentY += datePaint.textSize + itemSpacing
            }
        }

        // Responsibilities
        if (item.responsibilities.isNotEmpty()) {
            currentY += renderResponsibilities(
                canvas, item.responsibilities, element, x, currentY, width,
                responsibilityPaint, responsibilitySpacing
            )
        }

        return currentY - y
    }

    /**
     * Render list of responsibilities with bullets
     */
    private fun renderResponsibilities(
        canvas: Canvas,
        responsibilities: List<ResponsibilityItem>,
        element: ResumeElement.WorkExperienceElement,
        x: Float,
        y: Float,
        width: Float,
        responsibilityPaint: TextPaint,
        responsibilitySpacing: Float
    ): Float {
        var currentY = y

        responsibilities.forEachIndexed { index, responsibility ->
            if (responsibility.text.isNotEmpty()) {
                val bullet = getBulletCharacter(element.bulletStyle, index, responsibility)
                val bulletWidth = responsibilityPaint.measureText("$bullet ")

                // Draw bullet
                canvas.drawText(bullet, x, currentY + responsibilityPaint.textSize, responsibilityPaint)

                // Draw responsibility text (may need word wrapping for long text)
                canvas.drawText(
                    responsibility.text,
                    x + bulletWidth,
                    currentY + responsibilityPaint.textSize,
                    responsibilityPaint
                )

                currentY += responsibilityPaint.textSize + responsibilitySpacing
            }
        }

        return currentY - y
    }

    /**
     * Calculate total height needed for vertical layout
     */
    private fun calculateTotalHeight(
        element: ResumeElement.WorkExperienceElement,
        bounds: RectF,
        titlePaint: TextPaint,
        companyPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        responsibilityPaint: TextPaint,
        mapper: GridCoordinateMapper
    ): Float {
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val itemSpacing = mapper.borderWidthToPdfPoints(element.itemSpacing)
        val responsibilitySpacing = mapper.borderWidthToPdfPoints(element.responsibilitySpacing)

        var totalHeight = 0f

        element.items.forEachIndexed { index, item ->
            // Estimate height for this item
            var itemHeight = 0f

            when (element.displayStyle) {
                WorkExperienceDisplayStyle.STANDARD -> {
                    if (item.jobTitle.isNotEmpty()) itemHeight += titlePaint.textSize + itemSpacing
                    if (item.company.isNotEmpty() || element.showDates) {
                        itemHeight += maxOf(companyPaint.textSize, datePaint.textSize) + itemSpacing
                    }
                    if (element.showLocation && item.location.isNotEmpty()) {
                        itemHeight += locationPaint.textSize + itemSpacing
                    }
                }
                WorkExperienceDisplayStyle.COMPACT -> {
                    if (item.jobTitle.isNotEmpty() || item.company.isNotEmpty()) {
                        itemHeight += maxOf(titlePaint.textSize, datePaint.textSize) + itemSpacing
                    }
                    if (element.showLocation && item.location.isNotEmpty()) {
                        itemHeight += locationPaint.textSize + itemSpacing
                    }
                }
                WorkExperienceDisplayStyle.DETAILED -> {
                    if (item.jobTitle.isNotEmpty()) itemHeight += titlePaint.textSize + itemSpacing
                    if (item.company.isNotEmpty()) itemHeight += companyPaint.textSize + itemSpacing
                    if (element.showLocation && item.location.isNotEmpty() || element.showDates) {
                        itemHeight += maxOf(locationPaint.textSize, datePaint.textSize) + itemSpacing
                    }
                }
            }

            // Add responsibilities height
            if (item.responsibilities.isNotEmpty()) {
                itemHeight += (responsibilityPaint.textSize + responsibilitySpacing) * item.responsibilities.size
            }

            totalHeight += itemHeight

            // Add spacing between items (but not after last item)
            if (index < element.items.size - 1) {
                totalHeight += spacing
            }
        }

        return totalHeight
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

            // Font weight and style
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

            // Underline
            isUnderlineText = textStyle.isUnderlined

            // Letter spacing (in EM units)
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
}
