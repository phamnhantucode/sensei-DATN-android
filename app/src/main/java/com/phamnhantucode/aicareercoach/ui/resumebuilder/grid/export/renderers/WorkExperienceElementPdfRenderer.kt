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

        // Calculate horizontal starting position based on horizontal alignment
        val maxContentWidth = calculateMaxContentWidth(
            element, bounds.width(), titlePaint, companyPaint, datePaint,
            locationPaint, responsibilityPaint, mapper
        )

        val startX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (bounds.width() - maxContentWidth) / 2f
            HorizontalAlignment.END -> bounds.width() - maxContentWidth
        }

        element.items.forEachIndexed { index, item ->
            currentY += renderWorkItem(
                canvas,
                item,
                element,
                startX,
                currentY,
                maxContentWidth,
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
        var partCount = 0

        // Job Title
        if (item.jobTitle.isNotEmpty()) {
            val layout = android.text.StaticLayout.Builder
                .obtain(item.jobTitle, 0, item.jobTitle.length, titlePaint, width.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            layout.draw(canvas)
            canvas.restore()
            
            currentY += layout.height.toFloat()
            partCount++
        }

        // Company and Date Row
        if (item.company.isNotEmpty()) {
            val dateText = if (element.showDates) formatDateRange(item, element) else ""
            val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
            val availableCompanyWidth = if (dateText.isNotEmpty()) width - dateWidth - itemSpacing else width

            val companyLayout = android.text.StaticLayout.Builder
                .obtain(item.company, 0, item.company.length, companyPaint, availableCompanyWidth.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            companyLayout.draw(canvas)
            canvas.restore()

            if (dateText.isNotEmpty()) {
                val dateMetrics = datePaint.fontMetrics
                val dateBaseline = currentY - dateMetrics.ascent
                canvas.drawText(dateText, x + width - dateWidth, dateBaseline, datePaint)
            }

            currentY += companyLayout.height.toFloat()
            partCount++
        } else if (element.showDates) {
            val dateText = formatDateRange(item, element)
            if (dateText.isNotEmpty()) {
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

        // Responsibilities
        if (item.responsibilities.isNotEmpty()) {
            // Add spacing before responsibilities if there's content above
            if (partCount > 0) {
                currentY += itemSpacing
            }
            
            currentY += renderResponsibilities(
                canvas, item.responsibilities, element, x, currentY, width,
                responsibilityPaint, responsibilitySpacing
            )
        } else {
            // Remove trailing spacing if responsibilities is the last part
            if (partCount > 0) {
                currentY -= itemSpacing
            }
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
        var partCount = 0

        // Title + Company + Date Row
        val titleCompany = buildString {
            if (item.jobTitle.isNotEmpty()) append(item.jobTitle)
            if (item.jobTitle.isNotEmpty() && item.company.isNotEmpty()) append(" at ")
            if (item.company.isNotEmpty()) append(item.company)
        }

        if (titleCompany.isNotEmpty()) {
            val dateText = if (element.showDates) formatDateRange(item, element) else ""
            val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
            val availableTitleWidth = if (dateText.isNotEmpty()) width - dateWidth - itemSpacing else width

            val titleLayout = android.text.StaticLayout.Builder
                .obtain(titleCompany, 0, titleCompany.length, titlePaint, availableTitleWidth.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            titleLayout.draw(canvas)
            canvas.restore()

            if (dateText.isNotEmpty()) {
                val dateMetrics = datePaint.fontMetrics
                val dateBaseline = currentY - dateMetrics.ascent
                canvas.drawText(dateText, x + width - dateWidth, dateBaseline, datePaint)
            }

            currentY += titleLayout.height.toFloat()
            partCount++
        }

        // Location
        if (element.showLocation && item.location.isNotEmpty()) {
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

        // Responsibilities
        if (item.responsibilities.isNotEmpty()) {
            // Add spacing before responsibilities if there's content above
            if (partCount > 0) {
                currentY += itemSpacing
            }
            
            currentY += renderResponsibilities(
                canvas, item.responsibilities, element, x, currentY, width,
                responsibilityPaint, responsibilitySpacing
            )
        } else {
            // Remove trailing spacing if responsibilities is the last part
            if (partCount > 0) {
                currentY -= itemSpacing
            }
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
        var partCount = 0

        // Job Title
        if (item.jobTitle.isNotEmpty()) {
            val layout = android.text.StaticLayout.Builder
                .obtain(item.jobTitle, 0, item.jobTitle.length, titlePaint, width.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            layout.draw(canvas)
            canvas.restore()
            
            currentY += layout.height.toFloat()
            partCount++
        }

        // Company
        if (item.company.isNotEmpty()) {
            val companyLayout = android.text.StaticLayout.Builder
                .obtain(item.company, 0, item.company.length, companyPaint, width.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            companyLayout.draw(canvas)
            canvas.restore()
            
            currentY += companyLayout.height.toFloat()
            partCount++
        }

        // Location and Dates Row
        if (element.showLocation && item.location.isNotEmpty()) {
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
                val dateMetrics = datePaint.fontMetrics
                val dateBaseline = currentY - dateMetrics.ascent
                val dateWidth = datePaint.measureText(dateText)
                canvas.drawText(dateText, x + width - dateWidth, dateBaseline, datePaint)
                
                val dateHeight = dateMetrics.descent - dateMetrics.ascent
                currentY += dateHeight
                partCount++
            }
        }

        // Responsibilities
        if (item.responsibilities.isNotEmpty()) {
            // Add spacing before responsibilities if there's content above
            if (partCount > 0) {
                currentY += itemSpacing
            }
            
            currentY += renderResponsibilities(
                canvas, item.responsibilities, element, x, currentY, width,
                responsibilityPaint, responsibilitySpacing
            )
        } else {
            // Remove trailing spacing if responsibilities is the last part
            if (partCount > 0) {
                currentY -= itemSpacing
            }
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
                val textWidth = width - bulletWidth

                // Draw bullet
                val bulletMetrics = responsibilityPaint.fontMetrics
                val bulletBaseline = currentY - bulletMetrics.ascent
                canvas.drawText(bullet, x, bulletBaseline, responsibilityPaint)

                // Draw responsibility text with wrapping using StaticLayout
                val textLayout = android.text.StaticLayout.Builder
                    .obtain(responsibility.text, 0, responsibility.text.length, responsibilityPaint, textWidth.toInt().coerceAtLeast(1))
                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1f)
                    .setIncludePad(false)
                    .build()
                
                canvas.save()
                canvas.translate(x + bulletWidth, currentY)
                textLayout.draw(canvas)
                canvas.restore()

                currentY += textLayout.height.toFloat()
                
                // Add spacing only if not the last responsibility
                if (index < responsibilities.size - 1) {
                    currentY += responsibilitySpacing
                }
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
            var itemHeight = 0f
            var partCount = 0

            when (element.displayStyle) {
                WorkExperienceDisplayStyle.STANDARD -> {
                    if (item.jobTitle.isNotEmpty()) {
                        val layout = android.text.StaticLayout.Builder
                            .obtain(item.jobTitle, 0, item.jobTitle.length, titlePaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += layout.height.toFloat()
                        partCount++
                    }
                    if (item.company.isNotEmpty()) {
                        val dateText = if (element.showDates) formatDateRange(item, element) else ""
                        val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
                        val availableCompanyWidth = if (dateText.isNotEmpty()) bounds.width() - dateWidth - itemSpacing else bounds.width()
                        
                        val companyLayout = android.text.StaticLayout.Builder
                            .obtain(item.company, 0, item.company.length, companyPaint, availableCompanyWidth.toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += companyLayout.height.toFloat()
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
                }
                WorkExperienceDisplayStyle.COMPACT -> {
                    val titleCompany = buildString {
                        if (item.jobTitle.isNotEmpty()) append(item.jobTitle)
                        if (item.jobTitle.isNotEmpty() && item.company.isNotEmpty()) append(" at ")
                        if (item.company.isNotEmpty()) append(item.company)
                    }
                    if (titleCompany.isNotEmpty()) {
                        val dateText = if (element.showDates) formatDateRange(item, element) else ""
                        val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
                        val availableTitleWidth = if (dateText.isNotEmpty()) bounds.width() - dateWidth - itemSpacing else bounds.width()
                        
                        val titleLayout = android.text.StaticLayout.Builder
                            .obtain(titleCompany, 0, titleCompany.length, titlePaint, availableTitleWidth.toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += titleLayout.height.toFloat()
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
                }
                WorkExperienceDisplayStyle.DETAILED -> {
                    if (item.jobTitle.isNotEmpty()) {
                        val layout = android.text.StaticLayout.Builder
                            .obtain(item.jobTitle, 0, item.jobTitle.length, titlePaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += layout.height.toFloat()
                        partCount++
                    }
                    if (item.company.isNotEmpty()) {
                        val companyLayout = android.text.StaticLayout.Builder
                            .obtain(item.company, 0, item.company.length, companyPaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += companyLayout.height.toFloat()
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
                }
            }

            // Add responsibilities height
            if (item.responsibilities.isNotEmpty()) {
                item.responsibilities.forEachIndexed { respIndex, responsibility ->
                    if (responsibility.text.isNotEmpty()) {
                        val bullet = getBulletCharacter(element.bulletStyle, respIndex, responsibility)
                        val bulletWidth = responsibilityPaint.measureText("$bullet ")
                        val textWidth = bounds.width() - bulletWidth
                        
                        val textLayout = android.text.StaticLayout.Builder
                            .obtain(responsibility.text, 0, responsibility.text.length, responsibilityPaint, textWidth.toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        
                        itemHeight += textLayout.height.toFloat()
                        
                        // Add spacing between responsibilities (not after last)
                        if (respIndex < item.responsibilities.size - 1) {
                            itemHeight += responsibilitySpacing
                        }
                    }
                }
                
                // Add spacing before responsibilities if there's content above
                if (partCount > 0) {
                    itemHeight += itemSpacing
                }
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
        element: ResumeElement.WorkExperienceElement,
        availableWidth: Float,
        titlePaint: TextPaint,
        companyPaint: TextPaint,
        datePaint: TextPaint,
        locationPaint: TextPaint,
        responsibilityPaint: TextPaint,
        mapper: GridCoordinateMapper
    ): Float {
        var maxWidth = 0f
        var hasRightAlignedDate = false

        element.items.forEach { item ->
            // Measure each field and track the maximum width
            when (element.displayStyle) {
                WorkExperienceDisplayStyle.STANDARD, WorkExperienceDisplayStyle.DETAILED -> {
                    // Job title width
                    if (item.jobTitle.isNotEmpty()) {
                        maxWidth = maxOf(maxWidth, titlePaint.measureText(item.jobTitle))
                    }

                    // Company width (with or without date)
                    if (item.company.isNotEmpty()) {
                        val companyWidth = companyPaint.measureText(item.company)
                        if (element.showDates) {
                            // When date is shown, it's aligned right, so the row uses full width
                            hasRightAlignedDate = true
                            maxWidth = maxOf(maxWidth, companyWidth)
                        } else {
                            maxWidth = maxOf(maxWidth, companyWidth)
                        }
                    } else if (element.showDates) {
                        // Date only row, aligned right
                        hasRightAlignedDate = true
                        val dateText = formatDateRange(item, element)
                        maxWidth = maxOf(maxWidth, datePaint.measureText(dateText))
                    }

                    // Location width (in DETAILED, date can be on same row)
                    if (element.showLocation && item.location.isNotEmpty()) {
                        if (element.displayStyle == WorkExperienceDisplayStyle.DETAILED && element.showDates) {
                            hasRightAlignedDate = true
                            maxWidth = maxOf(maxWidth, locationPaint.measureText(item.location))
                        } else {
                            maxWidth = maxOf(maxWidth, locationPaint.measureText(item.location))
                        }
                    }
                }
                WorkExperienceDisplayStyle.COMPACT -> {
                    // Title + company on same line
                    val titleCompany = buildString {
                        if (item.jobTitle.isNotEmpty()) append(item.jobTitle)
                        if (item.jobTitle.isNotEmpty() && item.company.isNotEmpty()) append(" at ")
                        if (item.company.isNotEmpty()) append(item.company)
                    }
                    if (titleCompany.isNotEmpty()) {
                        val titleCompanyWidth = titlePaint.measureText(titleCompany)
                        if (element.showDates) {
                            // When date is shown, it's aligned right, so the row uses full width
                            hasRightAlignedDate = true
                            maxWidth = maxOf(maxWidth, titleCompanyWidth)
                        } else {
                            maxWidth = maxOf(maxWidth, titleCompanyWidth)
                        }
                    }

                    // Location width
                    if (element.showLocation && item.location.isNotEmpty()) {
                        maxWidth = maxOf(maxWidth, locationPaint.measureText(item.location))
                    }
                }
            }

            // Responsibilities width (bullet + text)
            item.responsibilities.forEach { responsibility ->
                if (responsibility.text.isNotEmpty()) {
                    val bullet = getBulletCharacter(element.bulletStyle, 0, responsibility)
                    val bulletWidth = responsibilityPaint.measureText("$bullet ")
                    val textWidth = responsibilityPaint.measureText(responsibility.text)
                    maxWidth = maxOf(maxWidth, bulletWidth + textWidth)
                }
            }
        }

        // If we have right-aligned dates, use full available width to ensure proper spacing
        return if (hasRightAlignedDate) {
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
