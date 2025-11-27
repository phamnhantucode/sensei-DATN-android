package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager
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
        var partCount = 0

        // Degree
        if (item.degree.isNotEmpty()) {
            val degreeLayout = android.text.StaticLayout.Builder
                .obtain(item.degree, 0, item.degree.length, degreePaint, width.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            degreeLayout.draw(canvas)
            canvas.restore()
            
            currentY += degreeLayout.height.toFloat()
            partCount++
        }

        // Institution and Date Row
        if (item.institution.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            
            val dateText = if (element.showDates) formatDateRange(item, element) else ""
            val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
            val availableInstitutionWidth = if (dateText.isNotEmpty()) width - dateWidth - itemSpacing else width

            val institutionLayout = android.text.StaticLayout.Builder
                .obtain(item.institution, 0, item.institution.length, institutionPaint, availableInstitutionWidth.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            institutionLayout.draw(canvas)
            canvas.restore()

            if (dateText.isNotEmpty()) {
                val dateMetrics = datePaint.fontMetrics
                val dateBaseline = currentY - dateMetrics.ascent
                canvas.drawText(dateText, x + width - dateWidth, dateBaseline, datePaint)
            }

            currentY += institutionLayout.height.toFloat()
            partCount++
        } else if (element.showDates) {
            val dateText = formatDateRange(item, element)
            if (dateText.isNotEmpty()) {
                if (partCount > 0) currentY += itemSpacing
                
                val dateMetrics = datePaint.fontMetrics
                val dateBaseline = currentY - dateMetrics.ascent
                val dateWidth = datePaint.measureText(dateText)
                canvas.drawText(dateText, x + width - dateWidth, dateBaseline, datePaint)
                
                val dateHeight = dateMetrics.descent - dateMetrics.ascent
                currentY += dateHeight
                partCount++
            }
        }

        // Location
        if (element.showLocation && item.location.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            
            val locationLayout = android.text.StaticLayout.Builder
                .obtain(item.location, 0, item.location.length, locationPaint, width.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            locationLayout.draw(canvas)
            canvas.restore()
            
            currentY += locationLayout.height.toFloat()
            partCount++
        }

        // GPA
        if (element.showGPA && item.gpa.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            
            val gpaText = "GPA: ${item.gpa}"
            val gpaMetrics = gpaPaint.fontMetrics
            val gpaBaseline = currentY - gpaMetrics.ascent
            canvas.drawText(gpaText, x, gpaBaseline, gpaPaint)
            
            val gpaHeight = gpaMetrics.descent - gpaMetrics.ascent
            currentY += gpaHeight
            partCount++
        }

        // Achievements
        if (item.achievements.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
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
        var partCount = 0

        // Degree + Institution + Date Row
        val degreeInstitution = buildString {
            if (item.degree.isNotEmpty()) append(item.degree)
            if (item.degree.isNotEmpty() && item.institution.isNotEmpty()) append(", ")
            if (item.institution.isNotEmpty()) append(item.institution)
        }

        if (degreeInstitution.isNotEmpty()) {
            val dateText = if (element.showDates) formatDateRange(item, element) else ""
            val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
            val availableDegreeWidth = if (dateText.isNotEmpty()) width - dateWidth - itemSpacing else width

            val degreeLayout = android.text.StaticLayout.Builder
                .obtain(degreeInstitution, 0, degreeInstitution.length, degreePaint, availableDegreeWidth.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            degreeLayout.draw(canvas)
            canvas.restore()

            if (dateText.isNotEmpty()) {
                val dateMetrics = datePaint.fontMetrics
                val dateBaseline = currentY - dateMetrics.ascent
                canvas.drawText(dateText, x + width - dateWidth, dateBaseline, datePaint)
            }

            currentY += degreeLayout.height.toFloat()
            partCount++
        }

        // Location and GPA Row
        if (element.showLocation && item.location.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            
            val gpaText = if (element.showGPA && item.gpa.isNotEmpty()) "GPA: ${item.gpa}" else ""
            val gpaWidth = if (gpaText.isNotEmpty()) gpaPaint.measureText(gpaText) else 0f
            val availableLocationWidth = if (gpaText.isNotEmpty()) width - gpaWidth - itemSpacing else width

            val locationLayout = android.text.StaticLayout.Builder
                .obtain(item.location, 0, item.location.length, locationPaint, availableLocationWidth.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            locationLayout.draw(canvas)
            canvas.restore()

            if (gpaText.isNotEmpty()) {
                val gpaMetrics = gpaPaint.fontMetrics
                val gpaBaseline = currentY - gpaMetrics.ascent
                canvas.drawText(gpaText, x + width - gpaWidth, gpaBaseline, gpaPaint)
            }

            currentY += locationLayout.height.toFloat()
            partCount++
        } else if (element.showGPA && item.gpa.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            
            val gpaText = "GPA: ${item.gpa}"
            val gpaMetrics = gpaPaint.fontMetrics
            val gpaBaseline = currentY - gpaMetrics.ascent
            canvas.drawText(gpaText, x, gpaBaseline, gpaPaint)
            
            val gpaHeight = gpaMetrics.descent - gpaMetrics.ascent
            currentY += gpaHeight
            partCount++
        }

        // Achievements
        if (item.achievements.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
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
        var partCount = 0

        // Degree
        if (item.degree.isNotEmpty()) {
            val degreeLayout = android.text.StaticLayout.Builder
                .obtain(item.degree, 0, item.degree.length, degreePaint, width.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            degreeLayout.draw(canvas)
            canvas.restore()
            
            currentY += degreeLayout.height.toFloat()
            partCount++
        }

        // Institution
        if (item.institution.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            
            val institutionLayout = android.text.StaticLayout.Builder
                .obtain(item.institution, 0, item.institution.length, institutionPaint, width.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            institutionLayout.draw(canvas)
            canvas.restore()
            
            currentY += institutionLayout.height.toFloat()
            partCount++
        }

        // Location and Dates Row
        if (element.showLocation && item.location.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            
            val dateText = if (element.showDates) formatDateRange(item, element) else ""
            val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
            val availableLocationWidth = if (dateText.isNotEmpty()) width - dateWidth - itemSpacing else width

            val locationLayout = android.text.StaticLayout.Builder
                .obtain(item.location, 0, item.location.length, locationPaint, availableLocationWidth.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            locationLayout.draw(canvas)
            canvas.restore()

            if (dateText.isNotEmpty()) {
                val dateMetrics = datePaint.fontMetrics
                val dateBaseline = currentY - dateMetrics.ascent
                canvas.drawText(dateText, x + width - dateWidth, dateBaseline, datePaint)
            }

            currentY += locationLayout.height.toFloat()
            partCount++
        } else if (element.showDates) {
            val dateText = formatDateRange(item, element)
            if (dateText.isNotEmpty()) {
                if (partCount > 0) currentY += itemSpacing
                
                val dateMetrics = datePaint.fontMetrics
                val dateBaseline = currentY - dateMetrics.ascent
                val dateWidth = datePaint.measureText(dateText)
                canvas.drawText(dateText, x + width - dateWidth, dateBaseline, datePaint)
                
                val dateHeight = dateMetrics.descent - dateMetrics.ascent
                currentY += dateHeight
                partCount++
            }
        }

        // GPA
        if (element.showGPA && item.gpa.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            
            val gpaText = "GPA: ${item.gpa}"
            val gpaMetrics = gpaPaint.fontMetrics
            val gpaBaseline = currentY - gpaMetrics.ascent
            canvas.drawText(gpaText, x, gpaBaseline, gpaPaint)
            
            val gpaHeight = gpaMetrics.descent - gpaMetrics.ascent
            currentY += gpaHeight
            partCount++
        }

        // Achievements
        if (item.achievements.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
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
                val textWidth = width - bulletWidth

                // Draw bullet
                val bulletMetrics = achievementPaint.fontMetrics
                val bulletBaseline = currentY - bulletMetrics.ascent
                canvas.drawText(bullet, x, bulletBaseline, achievementPaint)

                // Draw achievement text with wrapping using StaticLayout
                val textLayout = android.text.StaticLayout.Builder
                    .obtain(achievement.text, 0, achievement.text.length, achievementPaint, textWidth.toInt().coerceAtLeast(1))
                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1f)
                    .setIncludePad(false)
                    .build()
                
                canvas.save()
                canvas.translate(x + bulletWidth, currentY)
                textLayout.draw(canvas)
                canvas.restore()

                currentY += textLayout.height.toFloat()
                
                // Add spacing only if not the last achievement
                if (index < achievements.size - 1) {
                    currentY += achievementSpacing
                }
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
            var itemHeight = 0f
            var partCount = 0

            when (element.displayStyle) {
                EducationDisplayStyle.STANDARD -> {
                    if (item.degree.isNotEmpty()) {
                        val degreeLayout = android.text.StaticLayout.Builder
                            .obtain(item.degree, 0, item.degree.length, degreePaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += degreeLayout.height.toFloat()
                        partCount++
                    }
                    
                    if (item.institution.isNotEmpty()) {
                        val dateText = if (element.showDates) formatDateRange(item, element) else ""
                        val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
                        val availableInstitutionWidth = if (dateText.isNotEmpty()) bounds.width() - dateWidth - itemSpacing else bounds.width()
                        
                        val institutionLayout = android.text.StaticLayout.Builder
                            .obtain(item.institution, 0, item.institution.length, institutionPaint, availableInstitutionWidth.toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += institutionLayout.height.toFloat()
                        partCount++
                    } else if (element.showDates) {
                        val dateMetrics = datePaint.fontMetrics
                        itemHeight += dateMetrics.descent - dateMetrics.ascent
                        partCount++
                    }
                    
                    if (element.showLocation && item.location.isNotEmpty()) {
                        val locationLayout = android.text.StaticLayout.Builder
                            .obtain(item.location, 0, item.location.length, locationPaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += locationLayout.height.toFloat()
                        partCount++
                    }
                    
                    if (element.showGPA && item.gpa.isNotEmpty()) {
                        val gpaMetrics = gpaPaint.fontMetrics
                        itemHeight += gpaMetrics.descent - gpaMetrics.ascent
                        partCount++
                    }
                }
                EducationDisplayStyle.COMPACT -> {
                    val degreeInstitution = buildString {
                        if (item.degree.isNotEmpty()) append(item.degree)
                        if (item.degree.isNotEmpty() && item.institution.isNotEmpty()) append(", ")
                        if (item.institution.isNotEmpty()) append(item.institution)
                    }
                    
                    if (degreeInstitution.isNotEmpty()) {
                        val dateText = if (element.showDates) formatDateRange(item, element) else ""
                        val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
                        val availableDegreeWidth = if (dateText.isNotEmpty()) bounds.width() - dateWidth - itemSpacing else bounds.width()
                        
                        val degreeLayout = android.text.StaticLayout.Builder
                            .obtain(degreeInstitution, 0, degreeInstitution.length, degreePaint, availableDegreeWidth.toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += degreeLayout.height.toFloat()
                        partCount++
                    }
                    
                    if (element.showLocation && item.location.isNotEmpty()) {
                        val gpaText = if (element.showGPA && item.gpa.isNotEmpty()) "GPA: ${item.gpa}" else ""
                        val gpaWidth = if (gpaText.isNotEmpty()) gpaPaint.measureText(gpaText) else 0f
                        val availableLocationWidth = if (gpaText.isNotEmpty()) bounds.width() - gpaWidth - itemSpacing else bounds.width()
                        
                        val locationLayout = android.text.StaticLayout.Builder
                            .obtain(item.location, 0, item.location.length, locationPaint, availableLocationWidth.toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += locationLayout.height.toFloat()
                        partCount++
                    } else if (element.showGPA && item.gpa.isNotEmpty()) {
                        val gpaMetrics = gpaPaint.fontMetrics
                        itemHeight += gpaMetrics.descent - gpaMetrics.ascent
                        partCount++
                    }
                }
                EducationDisplayStyle.DETAILED -> {
                    if (item.degree.isNotEmpty()) {
                        val degreeLayout = android.text.StaticLayout.Builder
                            .obtain(item.degree, 0, item.degree.length, degreePaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += degreeLayout.height.toFloat()
                        partCount++
                    }
                    
                    if (item.institution.isNotEmpty()) {
                        val institutionLayout = android.text.StaticLayout.Builder
                            .obtain(item.institution, 0, item.institution.length, institutionPaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += institutionLayout.height.toFloat()
                        partCount++
                    }
                    
                    if (element.showLocation && item.location.isNotEmpty()) {
                        val dateText = if (element.showDates) formatDateRange(item, element) else ""
                        val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
                        val availableLocationWidth = if (dateText.isNotEmpty()) bounds.width() - dateWidth - itemSpacing else bounds.width()
                        
                        val locationLayout = android.text.StaticLayout.Builder
                            .obtain(item.location, 0, item.location.length, locationPaint, availableLocationWidth.toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += locationLayout.height.toFloat()
                        partCount++
                    } else if (element.showDates) {
                        val dateMetrics = datePaint.fontMetrics
                        itemHeight += dateMetrics.descent - dateMetrics.ascent
                        partCount++
                    }
                    
                    if (element.showGPA && item.gpa.isNotEmpty()) {
                        val gpaMetrics = gpaPaint.fontMetrics
                        itemHeight += gpaMetrics.descent - gpaMetrics.ascent
                        partCount++
                    }
                }
            }

            // Add achievements height
            if (item.achievements.isNotEmpty()) {
                item.achievements.forEachIndexed { achIndex, achievement ->
                    if (achievement.text.isNotEmpty()) {
                        val bullet = getBulletCharacter(element.bulletStyle, achIndex, achievement)
                        val bulletWidth = achievementPaint.measureText("$bullet ")
                        val textWidth = bounds.width() - bulletWidth
                        
                        val textLayout = android.text.StaticLayout.Builder
                            .obtain(achievement.text, 0, achievement.text.length, achievementPaint, textWidth.toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        
                        itemHeight += textLayout.height.toFloat()
                        
                        if (achIndex < item.achievements.size - 1) {
                            itemHeight += achievementSpacing
                        }
                    }
                }
                
                if (item.achievements.isNotEmpty()) partCount++
            }
            
            // Add spacing between parts (not after last)
            if (partCount > 1) {
                itemHeight += itemSpacing * (partCount - 1)
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
        var hasRightAlignedContent = false

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
                            // When date is shown, it's aligned right, so use full width
                            hasRightAlignedContent = true
                            maxWidth = maxOf(maxWidth, institutionWidth)
                        } else {
                            maxWidth = maxOf(maxWidth, institutionWidth)
                        }
                    } else if (element.showDates) {
                        // Date only row, aligned right
                        hasRightAlignedContent = true
                        val dateText = formatDateRange(item, element)
                        maxWidth = maxOf(maxWidth, datePaint.measureText(dateText))
                    }

                    // Location width (in DETAILED, date can be on same row)
                    if (element.showLocation && item.location.isNotEmpty()) {
                        if (element.displayStyle == EducationDisplayStyle.DETAILED && element.showDates) {
                            hasRightAlignedContent = true
                            maxWidth = maxOf(maxWidth, locationPaint.measureText(item.location))
                        } else {
                            maxWidth = maxOf(maxWidth, locationPaint.measureText(item.location))
                        }
                    }

                    // GPA width
                    if (element.showGPA && item.gpa.isNotEmpty()) {
                        maxWidth = maxOf(maxWidth, gpaPaint.measureText("GPA: ${item.gpa}"))
                    }
                }
                EducationDisplayStyle.COMPACT -> {
                    // Degree + institution on same line
                    val degreeInstitution = buildString {
                        if (item.degree.isNotEmpty()) append(item.degree)
                        if (item.degree.isNotEmpty() && item.institution.isNotEmpty()) append(", ")
                        if (item.institution.isNotEmpty()) append(item.institution)
                    }
                    if (degreeInstitution.isNotEmpty()) {
                        val degreeInstitutionWidth = degreePaint.measureText(degreeInstitution)
                        if (element.showDates) {
                            // When date is shown, it's aligned right, so use full width
                            hasRightAlignedContent = true
                            maxWidth = maxOf(maxWidth, degreeInstitutionWidth)
                        } else {
                            maxWidth = maxOf(maxWidth, degreeInstitutionWidth)
                        }
                    }

                    // Location and GPA (can be on same row)
                    if (element.showLocation && item.location.isNotEmpty()) {
                        val locationWidth = locationPaint.measureText(item.location)
                        if (element.showGPA && item.gpa.isNotEmpty()) {
                            // GPA is aligned right
                            hasRightAlignedContent = true
                            maxWidth = maxOf(maxWidth, locationWidth)
                        } else {
                            maxWidth = maxOf(maxWidth, locationWidth)
                        }
                    } else if (element.showGPA && item.gpa.isNotEmpty()) {
                        maxWidth = maxOf(maxWidth, gpaPaint.measureText("GPA: ${item.gpa}"))
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

        // If we have right-aligned content, use full available width to ensure proper spacing
        return if (hasRightAlignedContent) {
            availableWidth
        } else {
            minOf(maxWidth, availableWidth)
        }
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
            textSize = mapper.fontSizeToPdfPoints(textStyle.fontSize)
            color = context.colorConverter.toIntColorWithOpacity(textStyle.color, opacity)

            // Use Poppins font with proper weight mapping
            typeface = com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager.getPoppinsTypeface(
                context.context,
                textStyle.fontWeight,
                textStyle.isItalic
            )

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
