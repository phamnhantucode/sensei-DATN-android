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
 * Project Element PDF Renderer
 *
 * Renders project entries with name, description, dates, technologies, links, and highlights
 */
class ProjectElementPdfRenderer : ElementPdfRenderer<ResumeElement.ProjectElement> {

    override suspend fun render(
        canvas: Canvas,
        element: ResumeElement.ProjectElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        if (element.items.isEmpty()) return

        // Draw background and borders
        drawElementStyle(canvas, element.style, bounds, mapper, context)

        // Apply 8dp content padding
        val contentPadding = mapper.borderWidthToPdfPoints(8f)
        val contentBounds = RectF(
            bounds.left + contentPadding,
            bounds.top + contentPadding,
            bounds.right - contentPadding,
            bounds.bottom - contentPadding
        )

        // Create text paints
        val namePaint = createTextPaint(element.nameStyle, element.style.opacity, mapper, context)
        val descriptionPaint = createTextPaint(element.descriptionStyle, element.style.opacity, mapper, context)
        val datePaint = createTextPaint(element.dateStyle, element.style.opacity, mapper, context)
        val technologyPaint = createTextPaint(element.technologyStyle, element.style.opacity, mapper, context)
        val highlightPaint = createTextPaint(element.highlightStyle, element.style.opacity, mapper, context)
        val linkPaint = createTextPaint(element.linkStyle, element.style.opacity, mapper, context)

        canvas.save()
        canvas.translate(contentBounds.left, contentBounds.top)
        canvas.clipRect(0f, 0f, contentBounds.width(), contentBounds.height())

        renderVerticalLayout(
            canvas,
            element,
            contentBounds,
            namePaint,
            descriptionPaint,
            datePaint,
            technologyPaint,
            highlightPaint,
            linkPaint,
            mapper,
            context
        )

        canvas.restore()
    }

    private fun renderVerticalLayout(
        canvas: Canvas,
        element: ResumeElement.ProjectElement,
        bounds: RectF,
        namePaint: TextPaint,
        descriptionPaint: TextPaint,
        datePaint: TextPaint,
        technologyPaint: TextPaint,
        highlightPaint: TextPaint,
        linkPaint: TextPaint,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val itemSpacing = mapper.borderWidthToPdfPoints(element.itemSpacing)

        // Calculate vertical alignment
        val totalHeight = calculateTotalHeight(
            element, bounds, namePaint, descriptionPaint, datePaint,
            technologyPaint, highlightPaint, linkPaint, mapper
        )

        var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        // Calculate horizontal alignment
        val maxContentWidth = calculateMaxContentWidth(
            element, bounds.width(), namePaint, descriptionPaint, datePaint,
            technologyPaint, highlightPaint, linkPaint, mapper
        )

        val startX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (bounds.width() - maxContentWidth) / 2f
            HorizontalAlignment.END -> bounds.width() - maxContentWidth
        }

        element.items.forEachIndexed { index, item ->
            currentY += renderProjectItem(
                canvas, item, element, startX, currentY, maxContentWidth,
                namePaint, descriptionPaint, datePaint, technologyPaint,
                highlightPaint, linkPaint, itemSpacing, mapper, context
            )

            if (index < element.items.size - 1) {
                currentY += spacing
            }
        }
    }

    private fun renderProjectItem(
        canvas: Canvas,
        item: ProjectItem,
        element: ResumeElement.ProjectElement,
        x: Float,
        y: Float,
        width: Float,
        namePaint: TextPaint,
        descriptionPaint: TextPaint,
        datePaint: TextPaint,
        technologyPaint: TextPaint,
        highlightPaint: TextPaint,
        linkPaint: TextPaint,
        itemSpacing: Float,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): Float {
        var currentY = y

        when (element.displayStyle) {
            ProjectDisplayStyle.STANDARD -> {
                currentY += renderStandardLayout(
                    canvas, item, element, x, currentY, width,
                    namePaint, descriptionPaint, datePaint, technologyPaint,
                    highlightPaint, linkPaint, itemSpacing, mapper, context
                )
            }
            ProjectDisplayStyle.COMPACT -> {
                currentY += renderCompactLayout(
                    canvas, item, element, x, currentY, width,
                    namePaint, descriptionPaint, datePaint, technologyPaint,
                    highlightPaint, linkPaint, itemSpacing, mapper, context
                )
            }
            ProjectDisplayStyle.DETAILED -> {
                currentY += renderDetailedLayout(
                    canvas, item, element, x, currentY, width,
                    namePaint, descriptionPaint, datePaint, technologyPaint,
                    highlightPaint, linkPaint, itemSpacing, mapper, context
                )
            }
        }

        return currentY - y
    }

    private fun renderStandardLayout(
        canvas: Canvas,
        item: ProjectItem,
        element: ResumeElement.ProjectElement,
        x: Float,
        y: Float,
        width: Float,
        namePaint: TextPaint,
        descriptionPaint: TextPaint,
        datePaint: TextPaint,
        technologyPaint: TextPaint,
        highlightPaint: TextPaint,
        linkPaint: TextPaint,
        itemSpacing: Float,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): Float {
        var currentY = y
        var partCount = 0

        // Project name and date on same row
        if (item.name.isNotEmpty()) {
            val dateText = if (element.showDates) formatDateRange(item, element) else ""
            val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
            val availableNameWidth = if (dateText.isNotEmpty()) width - dateWidth - itemSpacing else width

            val nameLayout = android.text.StaticLayout.Builder
                .obtain(item.name, 0, item.name.length, namePaint, availableNameWidth.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            nameLayout.draw(canvas)
            canvas.restore()

            if (dateText.isNotEmpty()) {
                val dateMetrics = datePaint.fontMetrics
                val dateBaseline = currentY - dateMetrics.ascent
                canvas.drawText(dateText, x + width - dateWidth, dateBaseline, datePaint)
            }

            currentY += nameLayout.height.toFloat()
            partCount++
        }

        // Technologies
        if (element.showTechnologies && item.technologies.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            currentY += renderTechnologies(canvas, item, element, x, currentY, width, technologyPaint, mapper, context)
            partCount++
        }

        // Description
        if (element.showDescription && item.description.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            
            val descLayout = android.text.StaticLayout.Builder
                .obtain(item.description, 0, item.description.length, descriptionPaint, width.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            descLayout.draw(canvas)
            canvas.restore()
            
            currentY += descLayout.height.toFloat()
            partCount++
        }

        // Link
        if (element.showLink && item.link.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            
            val linkLayout = android.text.StaticLayout.Builder
                .obtain(item.link, 0, item.link.length, linkPaint, width.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            linkLayout.draw(canvas)
            canvas.restore()
            
            currentY += linkLayout.height.toFloat()
            partCount++
        }

        // Highlights
        if (item.highlights.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            currentY += renderHighlights(canvas, item, element, x, currentY, width, highlightPaint, itemSpacing, mapper)
        }

        return currentY - y
    }

    private fun renderCompactLayout(
        canvas: Canvas,
        item: ProjectItem,
        element: ResumeElement.ProjectElement,
        x: Float,
        y: Float,
        width: Float,
        namePaint: TextPaint,
        descriptionPaint: TextPaint,
        datePaint: TextPaint,
        technologyPaint: TextPaint,
        highlightPaint: TextPaint,
        linkPaint: TextPaint,
        itemSpacing: Float,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): Float {
        var currentY = y
        var partCount = 0

        // Name + Date
        if (item.name.isNotEmpty()) {
            val dateText = if (element.showDates) formatDateRange(item, element) else ""
            val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
            val availableNameWidth = if (dateText.isNotEmpty()) width - dateWidth - itemSpacing else width

            val nameLayout = android.text.StaticLayout.Builder
                .obtain(item.name, 0, item.name.length, namePaint, availableNameWidth.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            nameLayout.draw(canvas)
            canvas.restore()

            if (dateText.isNotEmpty()) {
                val dateMetrics = datePaint.fontMetrics
                val dateBaseline = currentY - dateMetrics.ascent
                canvas.drawText(dateText, x + width - dateWidth, dateBaseline, datePaint)
            }

            currentY += nameLayout.height.toFloat()
            partCount++
        }

        // Technologies
        if (element.showTechnologies && item.technologies.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            currentY += renderTechnologies(canvas, item, element, x, currentY, width, technologyPaint, mapper, context)
        }

        return currentY - y
    }

    private fun renderDetailedLayout(
        canvas: Canvas,
        item: ProjectItem,
        element: ResumeElement.ProjectElement,
        x: Float,
        y: Float,
        width: Float,
        namePaint: TextPaint,
        descriptionPaint: TextPaint,
        datePaint: TextPaint,
        technologyPaint: TextPaint,
        highlightPaint: TextPaint,
        linkPaint: TextPaint,
        itemSpacing: Float,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): Float {
        var currentY = y
        var partCount = 0

        // Project name
        if (item.name.isNotEmpty()) {
            val nameLayout = android.text.StaticLayout.Builder
                .obtain(item.name, 0, item.name.length, namePaint, width.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            nameLayout.draw(canvas)
            canvas.restore()
            
            currentY += nameLayout.height.toFloat()
            partCount++
        }

        // Date
        if (element.showDates) {
            val dateText = formatDateRange(item, element)
            if (dateText.isNotEmpty()) {
                if (partCount > 0) currentY += itemSpacing
                
                val dateMetrics = datePaint.fontMetrics
                val dateBaseline = currentY - dateMetrics.ascent
                canvas.drawText(dateText, x, dateBaseline, datePaint)
                
                val dateHeight = dateMetrics.descent - dateMetrics.ascent
                currentY += dateHeight
                partCount++
            }
        }

        // Technologies
        if (element.showTechnologies && item.technologies.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            currentY += renderTechnologies(canvas, item, element, x, currentY, width, technologyPaint, mapper, context)
            partCount++
        }

        // Description
        if (element.showDescription && item.description.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            
            val descLayout = android.text.StaticLayout.Builder
                .obtain(item.description, 0, item.description.length, descriptionPaint, width.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            descLayout.draw(canvas)
            canvas.restore()
            
            currentY += descLayout.height.toFloat()
            partCount++
        }

        // Highlights
        if (item.highlights.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            currentY += renderHighlights(canvas, item, element, x, currentY, width, highlightPaint, itemSpacing, mapper)
            partCount++
        }

        // Link
        if (element.showLink && item.link.isNotEmpty()) {
            if (partCount > 0) currentY += itemSpacing
            
            val linkLayout = android.text.StaticLayout.Builder
                .obtain(item.link, 0, item.link.length, linkPaint, width.toInt().coerceAtLeast(1))
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            
            canvas.save()
            canvas.translate(x, currentY)
            linkLayout.draw(canvas)
            canvas.restore()
            
            currentY += linkLayout.height.toFloat()
        }

        return currentY - y
    }

    private fun renderTechnologies(
        canvas: Canvas,
        item: ProjectItem,
        element: ResumeElement.ProjectElement,
        x: Float,
        y: Float,
        width: Float,
        technologyPaint: TextPaint,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): Float {
        val technologies = item.technologies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (technologies.isEmpty()) return 0f

        val tagPadding = mapper.borderWidthToPdfPoints(8f)
        val tagSpacing = mapper.borderWidthToPdfPoints(element.technologySpacing)
        val tagRadius = mapper.cornerRadiusToPdfPoints(element.technologyTagCornerRadius)
        val verticalPadding = mapper.borderWidthToPdfPoints(6f)
        
        // Calculate badge height accounting for font metrics
        val fontMetrics = technologyPaint.fontMetrics
        val textActualHeight = fontMetrics.descent - fontMetrics.ascent
        val tagHeight = textActualHeight + verticalPadding * 2

        val tagBackgroundPaint = Paint().apply {
            color = element.technologyTagBackgroundColor?.toInt() ?: 0xFFE3F2FD.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        var currentX = x
        var currentY = y

        technologies.forEach { tech ->
            val textWidth = technologyPaint.measureText(tech)
            val tagWidth = textWidth + tagPadding * 2

            if (currentX + tagWidth > x + width && currentX > x) {
                currentX = x
                currentY += tagHeight + tagSpacing
            }

            val tagRect = RectF(currentX, currentY, currentX + tagWidth, currentY + tagHeight)
            canvas.drawRoundRect(tagRect, tagRadius, tagRadius, tagBackgroundPaint)

            if (element.technologyTagBorderColor != null && element.technologyTagBorderWidth > 0) {
                val borderPaint = Paint().apply {
                    color = element.technologyTagBorderColor.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = mapper.borderWidthToPdfPoints(element.technologyTagBorderWidth)
                    isAntiAlias = true
                }
                canvas.drawRoundRect(tagRect, tagRadius, tagRadius, borderPaint)
            }

            // Draw text centered vertically in badge
            val textY = currentY + tagHeight / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(tech, currentX + tagPadding, textY, technologyPaint)

            currentX += tagWidth + tagSpacing
        }

        return currentY - y + tagHeight
    }

    private fun renderHighlights(
        canvas: Canvas,
        item: ProjectItem,
        element: ResumeElement.ProjectElement,
        x: Float,
        y: Float,
        width: Float,
        highlightPaint: TextPaint,
        highlightSpacing: Float,
        mapper: GridCoordinateMapper
    ): Float {
        var currentY = y

        item.highlights.forEachIndexed { index, highlight ->
            if (highlight.text.isNotEmpty()) {
                val bullet = getBulletCharacter(element.bulletStyle, index, highlight)
                val bulletWidth = highlightPaint.measureText("$bullet ")
                val textWidth = width - bulletWidth

                // Draw bullet
                val bulletMetrics = highlightPaint.fontMetrics
                val bulletBaseline = currentY - bulletMetrics.ascent
                canvas.drawText(bullet, x, bulletBaseline, highlightPaint)

                // Draw highlight text with wrapping using StaticLayout
                val textLayout = android.text.StaticLayout.Builder
                    .obtain(highlight.text, 0, highlight.text.length, highlightPaint, textWidth.toInt().coerceAtLeast(1))
                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1f)
                    .setIncludePad(false)
                    .build()
                
                canvas.save()
                canvas.translate(x + bulletWidth, currentY)
                textLayout.draw(canvas)
                canvas.restore()

                currentY += textLayout.height.toFloat()
                
                // Add spacing only if not the last highlight
                if (index < item.highlights.size - 1) {
                    currentY += highlightSpacing
                }
            }
        }

        return currentY - y
    }

    private fun calculateTotalHeight(
        element: ResumeElement.ProjectElement,
        bounds: RectF,
        namePaint: TextPaint,
        descriptionPaint: TextPaint,
        datePaint: TextPaint,
        technologyPaint: TextPaint,
        highlightPaint: TextPaint,
        linkPaint: TextPaint,
        mapper: GridCoordinateMapper
    ): Float {
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val itemSpacing = mapper.borderWidthToPdfPoints(element.itemSpacing)
        val highlightSpacing = itemSpacing
        var totalHeight = 0f

        element.items.forEachIndexed { index, item ->
            var itemHeight = 0f
            var partCount = 0

            when (element.displayStyle) {
                ProjectDisplayStyle.STANDARD -> {
                    if (item.name.isNotEmpty()) {
                        val dateText = if (element.showDates) formatDateRange(item, element) else ""
                        val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
                        val availableNameWidth = if (dateText.isNotEmpty()) bounds.width() - dateWidth - itemSpacing else bounds.width()
                        
                        val nameLayout = android.text.StaticLayout.Builder
                            .obtain(item.name, 0, item.name.length, namePaint, availableNameWidth.toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += nameLayout.height.toFloat()
                        partCount++
                    }
                    
                    if (element.showTechnologies && item.technologies.isNotEmpty()) {
                        val technologies = item.technologies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val tagPadding = mapper.borderWidthToPdfPoints(8f)
                        val verticalPadding = mapper.borderWidthToPdfPoints(6f)
                        val fontMetrics = technologyPaint.fontMetrics
                        val textActualHeight = fontMetrics.descent - fontMetrics.ascent
                        val tagHeight = textActualHeight + verticalPadding * 2
                        itemHeight += tagHeight
                        partCount++
                    }
                    
                    if (element.showDescription && item.description.isNotEmpty()) {
                        val descLayout = android.text.StaticLayout.Builder
                            .obtain(item.description, 0, item.description.length, descriptionPaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += descLayout.height.toFloat()
                        partCount++
                    }
                    
                    if (element.showLink && item.link.isNotEmpty()) {
                        val linkLayout = android.text.StaticLayout.Builder
                            .obtain(item.link, 0, item.link.length, linkPaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += linkLayout.height.toFloat()
                        partCount++
                    }
                    
                    if (item.highlights.isNotEmpty()) {
                        item.highlights.forEachIndexed { hIndex, highlight ->
                            if (highlight.text.isNotEmpty()) {
                                val bullet = getBulletCharacter(element.bulletStyle, hIndex, highlight)
                                val bulletWidth = highlightPaint.measureText("$bullet ")
                                val textWidth = bounds.width() - bulletWidth
                                
                                val textLayout = android.text.StaticLayout.Builder
                                    .obtain(highlight.text, 0, highlight.text.length, highlightPaint, textWidth.toInt().coerceAtLeast(1))
                                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                                    .setLineSpacing(0f, 1f)
                                    .setIncludePad(false)
                                    .build()
                                
                                itemHeight += textLayout.height.toFloat()
                                
                                if (hIndex < item.highlights.size - 1) {
                                    itemHeight += highlightSpacing
                                }
                            }
                        }
                        
                        if (item.highlights.isNotEmpty()) partCount++
                    }
                    
                    if (partCount > 1) {
                        itemHeight += itemSpacing * (partCount - 1)
                    }
                }
                ProjectDisplayStyle.COMPACT -> {
                    if (item.name.isNotEmpty()) {
                        val dateText = if (element.showDates) formatDateRange(item, element) else ""
                        val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
                        val availableNameWidth = if (dateText.isNotEmpty()) bounds.width() - dateWidth - itemSpacing else bounds.width()
                        
                        val nameLayout = android.text.StaticLayout.Builder
                            .obtain(item.name, 0, item.name.length, namePaint, availableNameWidth.toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += nameLayout.height.toFloat()
                        partCount++
                    }
                    
                    if (element.showTechnologies && item.technologies.isNotEmpty()) {
                        val technologies = item.technologies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val verticalPadding = mapper.borderWidthToPdfPoints(6f)
                        val fontMetrics = technologyPaint.fontMetrics
                        val textActualHeight = fontMetrics.descent - fontMetrics.ascent
                        val tagHeight = textActualHeight + verticalPadding * 2
                        itemHeight += tagHeight
                        partCount++
                    }
                    
                    if (partCount > 1) {
                        itemHeight += itemSpacing * (partCount - 1)
                    }
                }
                ProjectDisplayStyle.DETAILED -> {
                    if (item.name.isNotEmpty()) {
                        val nameLayout = android.text.StaticLayout.Builder
                            .obtain(item.name, 0, item.name.length, namePaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += nameLayout.height.toFloat()
                        partCount++
                    }
                    
                    if (element.showDates) {
                        val dateText = formatDateRange(item, element)
                        if (dateText.isNotEmpty()) {
                            val dateMetrics = datePaint.fontMetrics
                            itemHeight += dateMetrics.descent - dateMetrics.ascent
                            partCount++
                        }
                    }
                    
                    if (element.showTechnologies && item.technologies.isNotEmpty()) {
                        val technologies = item.technologies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val verticalPadding = mapper.borderWidthToPdfPoints(6f)
                        val fontMetrics = technologyPaint.fontMetrics
                        val textActualHeight = fontMetrics.descent - fontMetrics.ascent
                        val tagHeight = textActualHeight + verticalPadding * 2
                        itemHeight += tagHeight
                        partCount++
                    }
                    
                    if (element.showDescription && item.description.isNotEmpty()) {
                        val descLayout = android.text.StaticLayout.Builder
                            .obtain(item.description, 0, item.description.length, descriptionPaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += descLayout.height.toFloat()
                        partCount++
                    }
                    
                    if (item.highlights.isNotEmpty()) {
                        item.highlights.forEachIndexed { hIndex, highlight ->
                            if (highlight.text.isNotEmpty()) {
                                val bullet = getBulletCharacter(element.bulletStyle, hIndex, highlight)
                                val bulletWidth = highlightPaint.measureText("$bullet ")
                                val textWidth = bounds.width() - bulletWidth
                                
                                val textLayout = android.text.StaticLayout.Builder
                                    .obtain(highlight.text, 0, highlight.text.length, highlightPaint, textWidth.toInt().coerceAtLeast(1))
                                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                                    .setLineSpacing(0f, 1f)
                                    .setIncludePad(false)
                                    .build()
                                
                                itemHeight += textLayout.height.toFloat()
                                
                                if (hIndex < item.highlights.size - 1) {
                                    itemHeight += highlightSpacing
                                }
                            }
                        }
                        
                        if (item.highlights.isNotEmpty()) partCount++
                    }
                    
                    if (element.showLink && item.link.isNotEmpty()) {
                        val linkLayout = android.text.StaticLayout.Builder
                            .obtain(item.link, 0, item.link.length, linkPaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += linkLayout.height.toFloat()
                        partCount++
                    }
                    
                    if (partCount > 1) {
                        itemHeight += itemSpacing * (partCount - 1)
                    }
                }
            }

            totalHeight += itemHeight
            if (index < element.items.size - 1) totalHeight += spacing
        }

        return totalHeight
    }

    private fun calculateMaxContentWidth(
        element: ResumeElement.ProjectElement,
        availableWidth: Float,
        namePaint: TextPaint,
        descriptionPaint: TextPaint,
        datePaint: TextPaint,
        technologyPaint: TextPaint,
        highlightPaint: TextPaint,
        linkPaint: TextPaint,
        mapper: GridCoordinateMapper
    ): Float {
        var maxWidth = 0f
        var hasRightAlignedDate = false

        element.items.forEach { item ->
            if (item.name.isNotEmpty()) {
                val nameWidth = namePaint.measureText(item.name)
                if (element.showDates) {
                    // When date is shown, it's aligned right, so use full width
                    hasRightAlignedDate = true
                    maxWidth = maxOf(maxWidth, nameWidth)
                } else {
                    maxWidth = maxOf(maxWidth, nameWidth)
                }
            }

            if (element.showDescription && item.description.isNotEmpty()) {
                maxWidth = maxOf(maxWidth, descriptionPaint.measureText(item.description))
            }

            if (element.showLink && item.link.isNotEmpty()) {
                maxWidth = maxOf(maxWidth, linkPaint.measureText(item.link))
            }

            item.highlights.forEach { highlight ->
                if (highlight.text.isNotEmpty()) {
                    val bullet = getBulletCharacter(element.bulletStyle, 0, highlight)
                    val bulletWidth = highlightPaint.measureText("$bullet ")
                    val textWidth = highlightPaint.measureText(highlight.text)
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

    private fun getBulletCharacter(bulletStyle: BulletStyle, index: Int, highlight: ProjectHighlight): String {
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

    private fun formatDateRange(item: ProjectItem, element: ResumeElement.ProjectElement): String {
        val start = formatDate(item.startDate, element.dateFormat)
        val end = if (item.isOngoing) "Present" else formatDate(item.endDate, element.dateFormat)

        return when {
            start.isNotEmpty() && end.isNotEmpty() -> "$start${element.dateSeparator}$end"
            start.isNotEmpty() -> start
            end.isNotEmpty() -> end
            else -> ""
        }
    }

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

            isUnderlineText = textStyle.isUnderlined

            if (textStyle.letterSpacing != 0f) {
                letterSpacing = textStyle.letterSpacing / textStyle.fontSize
            }
        }
    }

    private fun drawElementStyle(
        canvas: Canvas,
        style: ElementStyle,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val paint = Paint().apply { isAntiAlias = true }

        // Draw shadow
        if (style.shadowColor != null && style.shadowBlur > 0f) {
            val shadowPaint = Paint(paint).apply {
                color = context.colorConverter.toIntColorWithOpacity(style.shadowColor, style.opacity)
                setShadowLayer(style.shadowBlur, style.shadowOffsetX, style.shadowOffsetY,
                    context.colorConverter.toIntColor(style.shadowColor))
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
            paint.color = context.colorConverter.toIntColorWithOpacity(style.backgroundColor, style.opacity)
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
            paint.color = context.colorConverter.toIntColorWithOpacity(style.borderColor, style.opacity)
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
