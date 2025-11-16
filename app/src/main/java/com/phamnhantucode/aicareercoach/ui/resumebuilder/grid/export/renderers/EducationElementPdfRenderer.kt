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
 * Education Element PDF Renderer
 *
 * Renders education entries with degrees, institutions, dates, and achievements
 */
class EducationElementPdfRenderer : ElementPdfRenderer<ResumeElement.EducationElement> {

    override suspend fun render(
        canvas: Canvas,
        element: ResumeElement.EducationElement,
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
        val degreePaint = createTextPaint(element.degreeStyle, element.style.opacity, mapper, context)
        val institutionPaint = createTextPaint(element.institutionStyle, element.style.opacity, mapper, context)
        val datePaint = createTextPaint(element.dateStyle, element.style.opacity, mapper, context)
        val locationPaint = createTextPaint(element.locationStyle, element.style.opacity, mapper, context)
        val gpaPaint = createTextPaint(element.gpaStyle, element.style.opacity, mapper, context)
        val achievementPaint = createTextPaint(element.achievementStyle, element.style.opacity, mapper, context)

        canvas.save()
        canvas.translate(contentBounds.left, contentBounds.top)
        canvas.clipRect(0f, 0f, contentBounds.width(), contentBounds.height())

        // Calculate layout based on orientation
        when (element.orientation) {
            EducationOrientation.VERTICAL -> {
                renderVerticalLayout(
                    canvas,
                    element,
                    contentBounds,
                    degreePaint,
                    institutionPaint,
                    datePaint,
                    locationPaint,
                    gpaPaint,
                    achievementPaint,
                    mapper,
                    context
                )
            }
            EducationOrientation.HORIZONTAL -> {
                renderHorizontalLayout(
                    canvas,
                    element,
                    contentBounds,
                    degreePaint,
                    institutionPaint,
                    datePaint,
                    locationPaint,
                    gpaPaint,
                    achievementPaint,
                    mapper,
                    context
                )
            }
        }

        canvas.restore()
    }

    /**
     * Render education items in vertical layout (stacked)
     */
    private fun renderVerticalLayout(
        canvas: Canvas,
        element: ResumeElement.EducationElement,
        bounds: RectF,
        degreePaint: TextPaint,
        institutionPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        gpaPaint: TextPaint,
        achievementPaint: TextPaint,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val itemSpacing = mapper.borderWidthToPdfPoints(element.itemSpacing)
        val achievementSpacing = mapper.borderWidthToPdfPoints(element.achievementSpacing)

        // Calculate vertical starting position based on vertical alignment
        val totalHeight = calculateTotalHeight(
            element, bounds, degreePaint, institutionPaint, datePaint,
            locationPaint, gpaPaint, achievementPaint, mapper
        )

        var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        // Calculate horizontal starting position based on horizontal alignment
        val maxContentWidth = calculateMaxContentWidth(
            element, bounds.width(), degreePaint, institutionPaint, datePaint,
            locationPaint, gpaPaint, achievementPaint, mapper
        )

        val startX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (bounds.width() - maxContentWidth) / 2f
            HorizontalAlignment.END -> bounds.width() - maxContentWidth
        }

        element.items.forEachIndexed { index, item ->
            currentY += renderEducationItem(
                canvas,
                item,
                element,
                startX,
                currentY,
                maxContentWidth,
                degreePaint,
                institutionPaint,
                datePaint,
                locationPaint,
                gpaPaint,
                achievementPaint,
                itemSpacing,
                achievementSpacing,
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
     * Render education items in horizontal layout (columns)
     */
    private fun renderHorizontalLayout(
        canvas: Canvas,
        element: ResumeElement.EducationElement,
        bounds: RectF,
        degreePaint: TextPaint,
        institutionPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        gpaPaint: TextPaint,
        achievementPaint: TextPaint,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        if (element.items.isEmpty()) return

        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val itemSpacing = mapper.borderWidthToPdfPoints(element.itemSpacing)
        val achievementSpacing = mapper.borderWidthToPdfPoints(element.achievementSpacing)

        // Calculate column width
        val totalSpacingWidth = spacing * (element.items.size - 1)
        val columnWidth = (bounds.width() - totalSpacingWidth) / element.items.size

        var currentX = 0f

        element.items.forEachIndexed { index, item ->
            renderEducationItem(
                canvas,
                item,
                element,
                currentX,
                0f,
                columnWidth,
                degreePaint,
                institutionPaint,
                datePaint,
                locationPaint,
                gpaPaint,
                achievementPaint,
                itemSpacing,
                achievementSpacing,
                mapper,
                context
            )

            currentX += columnWidth + spacing
        }
    }

    /**
     * Render a single education item and return its height
     */
    private fun renderEducationItem(
        canvas: Canvas,
        item: EducationItem,
        element: ResumeElement.EducationElement,
        x: Float,
        y: Float,
        width: Float,
        degreePaint: TextPaint,
        institutionPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        gpaPaint: TextPaint,
        achievementPaint: TextPaint,
        itemSpacing: Float,
        achievementSpacing: Float,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): Float {
        var currentY = y

        when (element.displayStyle) {
            EducationDisplayStyle.STANDARD -> {
                currentY += renderStandardLayout(
                    canvas, item, element, x, currentY, width,
                    degreePaint, institutionPaint, datePaint, locationPaint,
                    gpaPaint, achievementPaint, itemSpacing, achievementSpacing
                )
            }
            EducationDisplayStyle.COMPACT -> {
                currentY += renderCompactLayout(
                    canvas, item, element, x, currentY, width,
                    degreePaint, institutionPaint, datePaint, locationPaint,
                    gpaPaint, achievementPaint, itemSpacing, achievementSpacing
                )
            }
            EducationDisplayStyle.DETAILED -> {
                currentY += renderDetailedLayout(
                    canvas, item, element, x, currentY, width,
                    degreePaint, institutionPaint, datePaint, locationPaint,
                    gpaPaint, achievementPaint, itemSpacing, achievementSpacing
                )
            }
        }

        return currentY - y
    }

    /**
     * Standard layout: Degree and institution on separate lines
     */
    private fun renderStandardLayout(
        canvas: Canvas,
        item: EducationItem,
        element: ResumeElement.EducationElement,
        x: Float,
        y: Float,
        width: Float,
        degreePaint: TextPaint,
        institutionPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        gpaPaint: TextPaint,
        achievementPaint: TextPaint,
        itemSpacing: Float,
        achievementSpacing: Float
    ): Float {
        var currentY = y

        // Degree
        if (item.degree.isNotEmpty()) {
            canvas.drawText(item.degree, x, currentY + degreePaint.textSize, degreePaint)
            currentY += degreePaint.textSize + itemSpacing
        }

        // Institution and Date Row
        if (item.institution.isNotEmpty()) {
            val dateText = if (element.showDates) formatDateRange(item, element) else ""
            val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f

            canvas.drawText(item.institution, x, currentY + institutionPaint.textSize, institutionPaint)

            if (dateText.isNotEmpty()) {
                canvas.drawText(dateText, x + width - dateWidth, currentY + datePaint.textSize, datePaint)
            }

            currentY += maxOf(institutionPaint.textSize, datePaint.textSize) + itemSpacing
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

        // GPA
        if (element.showGPA && item.gpa.isNotEmpty()) {
            canvas.drawText("GPA: ${item.gpa}", x, currentY + gpaPaint.textSize, gpaPaint)
            currentY += gpaPaint.textSize + itemSpacing
        }

        // Achievements
        if (item.achievements.isNotEmpty()) {
            currentY += renderAchievements(
                canvas, item.achievements, element, x, currentY, width,
                achievementPaint, achievementSpacing
            )
        }

        return currentY - y
    }

    /**
     * Compact layout: Degree and institution on same line
     */
    private fun renderCompactLayout(
        canvas: Canvas,
        item: EducationItem,
        element: ResumeElement.EducationElement,
        x: Float,
        y: Float,
        width: Float,
        degreePaint: TextPaint,
        institutionPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        gpaPaint: TextPaint,
        achievementPaint: TextPaint,
        itemSpacing: Float,
        achievementSpacing: Float
    ): Float {
        var currentY = y

        // Degree + Institution + Date Row
        val degreeInstitution = buildString {
            if (item.degree.isNotEmpty()) append(item.degree)
            if (item.degree.isNotEmpty() && item.institution.isNotEmpty()) append(", ")
            if (item.institution.isNotEmpty()) append(item.institution)
        }

        if (degreeInstitution.isNotEmpty()) {
            val dateText = if (element.showDates) formatDateRange(item, element) else ""
            val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f

            canvas.drawText(degreeInstitution, x, currentY + degreePaint.textSize, degreePaint)

            if (dateText.isNotEmpty()) {
                canvas.drawText(dateText, x + width - dateWidth, currentY + datePaint.textSize, datePaint)
            }

            currentY += maxOf(degreePaint.textSize, datePaint.textSize) + itemSpacing
        }

        // Location and GPA Row
        if (element.showLocation && item.location.isNotEmpty()) {
            val gpaText = if (element.showGPA && item.gpa.isNotEmpty()) "GPA: ${item.gpa}" else ""
            val gpaWidth = if (gpaText.isNotEmpty()) gpaPaint.measureText(gpaText) else 0f

            canvas.drawText(item.location, x, currentY + locationPaint.textSize, locationPaint)

            if (gpaText.isNotEmpty()) {
                canvas.drawText(gpaText, x + width - gpaWidth, currentY + gpaPaint.textSize, gpaPaint)
            }

            currentY += maxOf(locationPaint.textSize, gpaPaint.textSize) + itemSpacing
        } else if (element.showGPA && item.gpa.isNotEmpty()) {
            canvas.drawText("GPA: ${item.gpa}", x, currentY + gpaPaint.textSize, gpaPaint)
            currentY += gpaPaint.textSize + itemSpacing
        }

        // Achievements
        if (item.achievements.isNotEmpty()) {
            currentY += renderAchievements(
                canvas, item.achievements, element, x, currentY, width,
                achievementPaint, achievementSpacing
            )
        }

        return currentY - y
    }

    /**
     * Detailed layout: All fields prominently displayed
     */
    private fun renderDetailedLayout(
        canvas: Canvas,
        item: EducationItem,
        element: ResumeElement.EducationElement,
        x: Float,
        y: Float,
        width: Float,
        degreePaint: TextPaint,
        institutionPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        gpaPaint: TextPaint,
        achievementPaint: TextPaint,
        itemSpacing: Float,
        achievementSpacing: Float
    ): Float {
        var currentY = y

        // Degree
        if (item.degree.isNotEmpty()) {
            canvas.drawText(item.degree, x, currentY + degreePaint.textSize, degreePaint)
            currentY += degreePaint.textSize + itemSpacing
        }

        // Institution
        if (item.institution.isNotEmpty()) {
            canvas.drawText(item.institution, x, currentY + institutionPaint.textSize, institutionPaint)
            currentY += institutionPaint.textSize + itemSpacing
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

        // GPA
        if (element.showGPA && item.gpa.isNotEmpty()) {
            canvas.drawText("GPA: ${item.gpa}", x, currentY + gpaPaint.textSize, gpaPaint)
            currentY += gpaPaint.textSize + itemSpacing
        }

        // Achievements
        if (item.achievements.isNotEmpty()) {
            currentY += renderAchievements(
                canvas, item.achievements, element, x, currentY, width,
                achievementPaint, achievementSpacing
            )
        }

        return currentY - y
    }

    /**
     * Render list of achievements with bullets
     */
    private fun renderAchievements(
        canvas: Canvas,
        achievements: List<AchievementItem>,
        element: ResumeElement.EducationElement,
        x: Float,
        y: Float,
        width: Float,
        achievementPaint: TextPaint,
        achievementSpacing: Float
    ): Float {
        var currentY = y

        achievements.forEachIndexed { index, achievement ->
            if (achievement.text.isNotEmpty()) {
                val bullet = getBulletCharacter(element.bulletStyle, index, achievement)
                val bulletWidth = achievementPaint.measureText("$bullet ")

                // Draw bullet
                canvas.drawText(bullet, x, currentY + achievementPaint.textSize, achievementPaint)

                // Draw achievement text (may need word wrapping for long text)
                canvas.drawText(
                    achievement.text,
                    x + bulletWidth,
                    currentY + achievementPaint.textSize,
                    achievementPaint
                )

                currentY += achievementPaint.textSize + achievementSpacing
            }
        }

        return currentY - y
    }

    /**
     * Calculate total height needed for vertical layout
     */
    private fun calculateTotalHeight(
        element: ResumeElement.EducationElement,
        bounds: RectF,
        degreePaint: TextPaint,
        institutionPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        gpaPaint: TextPaint,
        achievementPaint: TextPaint,
        mapper: GridCoordinateMapper
    ): Float {
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val itemSpacing = mapper.borderWidthToPdfPoints(element.itemSpacing)
        val achievementSpacing = mapper.borderWidthToPdfPoints(element.achievementSpacing)

        var totalHeight = 0f

        element.items.forEachIndexed { index, item ->
            // Estimate height for this item
            var itemHeight = 0f

            when (element.displayStyle) {
                EducationDisplayStyle.STANDARD -> {
                    if (item.degree.isNotEmpty()) itemHeight += degreePaint.textSize + itemSpacing
                    if (item.institution.isNotEmpty() || element.showDates) {
                        itemHeight += maxOf(institutionPaint.textSize, datePaint.textSize) + itemSpacing
                    }
                    if (element.showLocation && item.location.isNotEmpty()) {
                        itemHeight += locationPaint.textSize + itemSpacing
                    }
                    if (element.showGPA && item.gpa.isNotEmpty()) {
                        itemHeight += gpaPaint.textSize + itemSpacing
                    }
                }
                EducationDisplayStyle.COMPACT -> {
                    if (item.degree.isNotEmpty() || item.institution.isNotEmpty()) {
                        itemHeight += maxOf(degreePaint.textSize, datePaint.textSize) + itemSpacing
                    }
                    if (element.showLocation && item.location.isNotEmpty() || element.showGPA && item.gpa.isNotEmpty()) {
                        itemHeight += maxOf(locationPaint.textSize, gpaPaint.textSize) + itemSpacing
                    }
                }
                EducationDisplayStyle.DETAILED -> {
                    if (item.degree.isNotEmpty()) itemHeight += degreePaint.textSize + itemSpacing
                    if (item.institution.isNotEmpty()) itemHeight += institutionPaint.textSize + itemSpacing
                    if (element.showLocation && item.location.isNotEmpty() || element.showDates) {
                        itemHeight += maxOf(locationPaint.textSize, datePaint.textSize) + itemSpacing
                    }
                    if (element.showGPA && item.gpa.isNotEmpty()) {
                        itemHeight += gpaPaint.textSize + itemSpacing
                    }
                }
            }

            // Add achievements height
            if (item.achievements.isNotEmpty()) {
                itemHeight += (achievementPaint.textSize + achievementSpacing) * item.achievements.size
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
     * Calculate maximum content width for horizontal alignment
     */
    private fun calculateMaxContentWidth(
        element: ResumeElement.EducationElement,
        availableWidth: Float,
        degreePaint: TextPaint,
        institutionPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        gpaPaint: TextPaint,
        achievementPaint: TextPaint,
        mapper: GridCoordinateMapper
    ): Float {
        var maxWidth = 0f

        element.items.forEach { item ->
            // Measure each field and track the maximum width
            when (element.displayStyle) {
                EducationDisplayStyle.STANDARD, EducationDisplayStyle.DETAILED -> {
                    // Degree width
                    if (item.degree.isNotEmpty()) {
                        maxWidth = maxOf(maxWidth, degreePaint.measureText(item.degree))
                    }

                    // Institution width (with or without date)
                    if (item.institution.isNotEmpty()) {
                        val institutionWidth = institutionPaint.measureText(item.institution)
                        if (element.showDates) {
                            val dateText = formatDateRange(item, element)
                            val dateWidth = datePaint.measureText(dateText)
                            // Institution and date are on same row, add both
                            maxWidth = maxOf(maxWidth, institutionWidth + dateWidth)
                        } else {
                            maxWidth = maxOf(maxWidth, institutionWidth)
                        }
                    } else if (element.showDates) {
                        val dateText = formatDateRange(item, element)
                        maxWidth = maxOf(maxWidth, datePaint.measureText(dateText))
                    }

                    // Location width
                    if (element.showLocation && item.location.isNotEmpty()) {
                        maxWidth = maxOf(maxWidth, locationPaint.measureText(item.location))
                    }

                    // GPA width
                    if (element.showGPA && item.gpa.isNotEmpty()) {
                        maxWidth = maxOf(maxWidth, gpaPaint.measureText(item.gpa))
                    }
                }
                EducationDisplayStyle.COMPACT -> {
                    // Degree + institution on same line
                    val degreeInstitution = buildString {
                        if (item.degree.isNotEmpty()) append(item.degree)
                        if (item.degree.isNotEmpty() && item.institution.isNotEmpty()) append(" at ")
                        if (item.institution.isNotEmpty()) append(item.institution)
                    }
                    if (degreeInstitution.isNotEmpty()) {
                        val degreeInstitutionWidth = degreePaint.measureText(degreeInstitution)
                        if (element.showDates) {
                            val dateText = formatDateRange(item, element)
                            val dateWidth = datePaint.measureText(dateText)
                            maxWidth = maxOf(maxWidth, degreeInstitutionWidth + dateWidth)
                        } else {
                            maxWidth = maxOf(maxWidth, degreeInstitutionWidth)
                        }
                    }

                    // Location and GPA (can be on same row)
                    var locationGpaWidth = 0f
                    if (element.showLocation && item.location.isNotEmpty()) {
                        locationGpaWidth += locationPaint.measureText(item.location)
                    }
                    if (element.showGPA && item.gpa.isNotEmpty()) {
                        locationGpaWidth += gpaPaint.measureText(item.gpa)
                    }
                    if (locationGpaWidth > 0f) {
                        maxWidth = maxOf(maxWidth, locationGpaWidth)
                    }
                }
            }

            // Achievements width (bullet + text)
            item.achievements.forEach { achievement ->
                if (achievement.text.isNotEmpty()) {
                    val bullet = getBulletCharacter(element.bulletStyle, 0, achievement)
                    val bulletWidth = achievementPaint.measureText("$bullet ")
                    val textWidth = achievementPaint.measureText(achievement.text)
                    maxWidth = maxOf(maxWidth, bulletWidth + textWidth)
                }
            }
        }

        // Ensure we don't exceed available width
        return minOf(maxWidth, availableWidth)
    }

    /**
     * Get bullet character based on bullet style
     */
    private fun getBulletCharacter(
        bulletStyle: BulletStyle,
        index: Int,
        achievement: AchievementItem
    ): String {
        return when (bulletStyle) {
            BulletStyle.DISC -> "•"
            BulletStyle.DASH -> "-"
            BulletStyle.ARROW -> "→"
            BulletStyle.CHEVRON -> "›"
            BulletStyle.NUMBERED -> "${index + 1}."
            BulletStyle.CUSTOM_ICON -> achievement.customBullet ?: "•"
            BulletStyle.NONE -> ""
        }
    }

    /**
     * Format date range for display
     */
    private fun formatDateRange(
        item: EducationItem,
        element: ResumeElement.EducationElement
    ): String {
        val start = formatDate(item.startDate, element.dateFormat)
        val end = formatDate(item.endDate, element.dateFormat)

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
