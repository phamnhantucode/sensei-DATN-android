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
 * Certification Element PDF Renderer
 *
 * Renders certification entries with name, issuer, dates, credential IDs, and verification links
 */
class CertificationElementPdfRenderer : ElementPdfRenderer<ResumeElement.CertificationElement> {

    override suspend fun render(
        canvas: Canvas,
        element: ResumeElement.CertificationElement,
        bounds: RectF,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        if (element.items.isEmpty()) return

        drawElementStyle(canvas, element.style, bounds, mapper, context)

        val contentPadding = mapper.borderWidthToPdfPoints(8f)
        val contentBounds = RectF(
            bounds.left + contentPadding,
            bounds.top + contentPadding,
            bounds.right - contentPadding,
            bounds.bottom - contentPadding
        )

        val namePaint = createTextPaint(element.nameStyle, element.style.opacity, mapper, context)
        val issuerPaint = createTextPaint(element.issuerStyle, element.style.opacity, mapper, context)
        val datePaint = createTextPaint(element.dateStyle, element.style.opacity, mapper, context)
        val credentialIdPaint = createTextPaint(element.credentialIdStyle, element.style.opacity, mapper, context)
        val linkPaint = createTextPaint(element.linkStyle, element.style.opacity, mapper, context)
        val expiryStatusPaint = createTextPaint(element.expiryStatusStyle, element.style.opacity, mapper, context)

        canvas.save()
        canvas.translate(contentBounds.left, contentBounds.top)
        canvas.clipRect(0f, 0f, contentBounds.width(), contentBounds.height())

        renderLayout(canvas, element, contentBounds, namePaint, issuerPaint, datePaint,
            credentialIdPaint, linkPaint, expiryStatusPaint, mapper, context)

        canvas.restore()
    }

    private fun renderLayout(
        canvas: Canvas,
        element: ResumeElement.CertificationElement,
        bounds: RectF,
        namePaint: TextPaint,
        issuerPaint: TextPaint,
        datePaint: TextPaint,
        credentialIdPaint: TextPaint,
        linkPaint: TextPaint,
        expiryStatusPaint: TextPaint,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ) {
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val itemSpacing = mapper.borderWidthToPdfPoints(element.itemSpacing)

        val totalHeight = calculateTotalHeight(element, bounds, namePaint, issuerPaint,
            datePaint, credentialIdPaint, linkPaint, expiryStatusPaint, mapper)

        var currentY = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> 0f
            VerticalAlignment.CENTER -> (bounds.height() - totalHeight) / 2f
            VerticalAlignment.BOTTOM -> bounds.height() - totalHeight
        }

        val maxContentWidth = calculateMaxContentWidth(element, bounds.width(), namePaint,
            issuerPaint, datePaint, credentialIdPaint, linkPaint, expiryStatusPaint, mapper)

        val startX = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> 0f
            HorizontalAlignment.CENTER -> (bounds.width() - maxContentWidth) / 2f
            HorizontalAlignment.END -> bounds.width() - maxContentWidth
        }

        element.items.forEachIndexed { index, item ->
            currentY += renderCertificationItem(canvas, item, element, startX, currentY,
                maxContentWidth, namePaint, issuerPaint, datePaint, credentialIdPaint,
                linkPaint, expiryStatusPaint, itemSpacing, mapper, context)

            if (index < element.items.size - 1) currentY += spacing
        }
    }

    private fun renderCertificationItem(
        canvas: Canvas,
        item: CertificationItem,
        element: ResumeElement.CertificationElement,
        x: Float,
        y: Float,
        width: Float,
        namePaint: TextPaint,
        issuerPaint: TextPaint,
        datePaint: TextPaint,
        credentialIdPaint: TextPaint,
        linkPaint: TextPaint,
        expiryStatusPaint: TextPaint,
        itemSpacing: Float,
        mapper: GridCoordinateMapper,
        context: PdfRenderContext
    ): Float {
        var currentY = y

        when (element.displayStyle) {
            CertificationDisplayStyle.STANDARD -> {
                // Name and Status Badge on same row
                if (item.name.isNotEmpty()) {
                    val padding = mapper.borderWidthToPdfPoints(8f)
                    
                    // Calculate badge dimensions first if showing expiry status
                    var availableTextWidth = width
                    var badgeX = 0f
                    var statusWidth = 0f
                    var statusPaint: TextPaint? = null
                    var statusText = ""
                    var statusColor = 0L
                    var badgeHeight = 0f
                    
                    if (element.showExpiryStatus && item.expiryDate.isNotEmpty()) {
                        statusText = if (item.isActive) "Active" else "Expired"
                        statusColor = if (item.isActive) element.activeStatusColor else element.expiredStatusColor
                        statusPaint = TextPaint(expiryStatusPaint).apply {
                            color = context.colorConverter.toIntColorWithOpacity(statusColor, 1f)
                        }
                        
                        statusWidth = statusPaint.measureText(statusText)
                        badgeHeight = statusPaint.textSize + padding
                        val badgeWidth = statusWidth + padding * 2
                        availableTextWidth = width - badgeWidth - padding // Reserve space for badge + spacer
                        badgeX = x + width - statusWidth - padding
                    }
                    
                    // Draw name text with wrapping using StaticLayout
                    val layout = android.text.StaticLayout.Builder
                        .obtain(item.name, 0, item.name.length, namePaint, availableTextWidth.toInt().coerceAtLeast(1))
                        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1f)
                        .setIncludePad(false)
                        .build()
                    
                    canvas.save()
                    canvas.translate(x, currentY)
                    layout.draw(canvas)
                    canvas.restore()
                    
                    val textHeight = layout.height.toFloat()
                    
                    // Draw status badge on the right (aligned to top)
                    if (element.showExpiryStatus && item.expiryDate.isNotEmpty() && statusPaint != null) {
                        // Draw badge background
                        val badgePaint = Paint().apply {
                            color = context.colorConverter.toIntColorWithOpacity(statusColor, 0.2f)
                            style = Paint.Style.FILL
                            isAntiAlias = true
                        }
                        val cornerRadius = mapper.borderWidthToPdfPoints(12f)
                        val verticalPadding = mapper.borderWidthToPdfPoints(6f)
                        val badgeBounds = RectF(
                            badgeX - padding,
                            currentY,
                            badgeX + statusWidth + padding,
                            currentY + badgeHeight
                        )
                        canvas.drawRoundRect(badgeBounds, cornerRadius, cornerRadius, badgePaint)
                        
                        // Draw badge text centered vertically in badge
                        val fontMetrics = statusPaint.fontMetrics
                        val textY = currentY + badgeHeight / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
                        canvas.drawText(statusText, badgeX, textY, statusPaint)
                    }
                    
                    currentY += textHeight
                    
                    // Add spacing if there's more content below
                    if (item.issuer.isNotEmpty() || 
                        (element.showCredentialId && item.credentialId.isNotEmpty()) ||
                        (element.showVerificationLink && item.verificationLink.isNotEmpty())) {
                        currentY += itemSpacing
                    }
                }

                // Issuer and Date on same row
                if (item.issuer.isNotEmpty()) {
                    val issuerMetrics = issuerPaint.fontMetrics
                    val issuerBaseline = currentY - issuerMetrics.ascent
                    canvas.drawText(item.issuer, x, issuerBaseline, issuerPaint)
                    
                    // Draw date on the right
                    if (element.showIssueDate && item.issueDate.isNotEmpty()) {
                        val dateText = formatDate(item.issueDate, element.dateFormat)
                        val dateWidth = datePaint.measureText(dateText)
                        canvas.drawText(dateText, x + width - dateWidth, issuerBaseline, datePaint)
                    }
                    
                    val issuerHeight = issuerMetrics.descent - issuerMetrics.ascent
                    currentY += issuerHeight
                    
                    // Add spacing if there's more content below
                    if ((element.showCredentialId && item.credentialId.isNotEmpty()) ||
                        (element.showVerificationLink && item.verificationLink.isNotEmpty())) {
                        currentY += itemSpacing
                    }
                }

                // Credential ID
                if (element.showCredentialId && item.credentialId.isNotEmpty()) {
                    val credText = "Credential ID: ${item.credentialId}"
                    val credLayout = android.text.StaticLayout.Builder
                        .obtain(credText, 0, credText.length, credentialIdPaint, width.toInt().coerceAtLeast(1))
                        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1f)
                        .setIncludePad(false)
                        .build()
                    
                    canvas.save()
                    canvas.translate(x, currentY)
                    credLayout.draw(canvas)
                    canvas.restore()
                    
                    currentY += credLayout.height.toFloat()
                    
                    // Add spacing if there's more content below
                    if (element.showVerificationLink && item.verificationLink.isNotEmpty()) {
                        currentY += itemSpacing
                    }
                }

                // Verification Link
                if (element.showVerificationLink && item.verificationLink.isNotEmpty()) {
                    val linkLayout = android.text.StaticLayout.Builder
                        .obtain(item.verificationLink, 0, item.verificationLink.length, linkPaint, width.toInt().coerceAtLeast(1))
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
            }

            CertificationDisplayStyle.COMPACT -> {
                val halfSpacing = itemSpacing / 2f
                
                // Name + Issuer and Status Badge on same row
                val nameIssuer = buildString {
                    if (item.name.isNotEmpty()) append(item.name)
                    if (item.name.isNotEmpty() && item.issuer.isNotEmpty()) append(" - ")
                    if (item.issuer.isNotEmpty()) append(item.issuer)
                }

                if (nameIssuer.isNotEmpty()) {
                    val padding = mapper.borderWidthToPdfPoints(8f)
                    
                    // Calculate badge dimensions first if showing expiry status
                    var availableTextWidth = width
                    var badgeX = 0f
                    var statusWidth = 0f
                    var statusPaint: TextPaint? = null
                    var statusText = ""
                    var statusColor = 0L
                    var badgeHeight = 0f
                    
                    if (element.showExpiryStatus && item.expiryDate.isNotEmpty()) {
                        statusText = if (item.isActive) "Active" else "Expired"
                        statusColor = if (item.isActive) element.activeStatusColor else element.expiredStatusColor
                        statusPaint = TextPaint(expiryStatusPaint).apply {
                            color = context.colorConverter.toIntColorWithOpacity(statusColor, 1f)
                        }
                        
                        statusWidth = statusPaint.measureText(statusText)
                        val verticalPadding = mapper.borderWidthToPdfPoints(6f)
                        
                        // Calculate badge height accounting for font metrics
                        val fontMetrics = statusPaint.fontMetrics
                        val textActualHeight = fontMetrics.descent - fontMetrics.ascent
                        badgeHeight = textActualHeight + verticalPadding * 2
                        
                        val badgeWidth = statusWidth + padding * 2
                        availableTextWidth = width - badgeWidth - padding // Reserve space for badge + spacer
                        badgeX = x + width - statusWidth - padding
                    }
                    
                    // Draw name/issuer text with wrapping using StaticLayout
                    val layout = android.text.StaticLayout.Builder
                        .obtain(nameIssuer, 0, nameIssuer.length, namePaint, availableTextWidth.toInt().coerceAtLeast(1))
                        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1f)
                        .setIncludePad(false)
                        .build()
                    
                    canvas.save()
                    canvas.translate(x, currentY)
                    layout.draw(canvas)
                    canvas.restore()
                    
                    val textHeight = layout.height.toFloat()
                    
                    // Draw status badge on the right (aligned to top)
                    if (element.showExpiryStatus && item.expiryDate.isNotEmpty() && statusPaint != null) {
                        // Draw badge background
                        val badgePaint = Paint().apply {
                            color = context.colorConverter.toIntColorWithOpacity(statusColor, 0.2f)
                            style = Paint.Style.FILL
                            isAntiAlias = true
                        }
                        val cornerRadius = mapper.borderWidthToPdfPoints(12f)
                        val verticalPadding = mapper.borderWidthToPdfPoints(6f)
                        
                        val badgeBounds = RectF(
                            badgeX - padding,
                            currentY,
                            badgeX + statusWidth + padding,
                            currentY + badgeHeight
                        )
                        canvas.drawRoundRect(badgeBounds, cornerRadius, cornerRadius, badgePaint)
                        
                        // Draw badge text centered vertically in badge
                        val fontMetrics = statusPaint.fontMetrics
                        val textY = currentY + badgeHeight / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
                        canvas.drawText(statusText, badgeX, textY, statusPaint)
                    }
                    
                    currentY += textHeight
                    
                    // Add half spacing if there's more content below
                    if ((element.showIssueDate && item.issueDate.isNotEmpty()) ||
                        (element.showCredentialId && item.credentialId.isNotEmpty())) {
                        currentY += halfSpacing
                    }
                }

                // Date
                if (element.showIssueDate && item.issueDate.isNotEmpty()) {
                    val dateText = formatDate(item.issueDate, element.dateFormat)
                    val dateMetrics = datePaint.fontMetrics
                    val dateBaseline = currentY - dateMetrics.ascent
                    canvas.drawText(dateText, x, dateBaseline, datePaint)
                    
                    val dateHeight = dateMetrics.descent - dateMetrics.ascent
                    currentY += dateHeight
                    
                    // Add half spacing if there's more content below
                    if (element.showCredentialId && item.credentialId.isNotEmpty()) {
                        currentY += halfSpacing
                    }
                }

                // Credential ID
                if (element.showCredentialId && item.credentialId.isNotEmpty()) {
                    val credText = "ID: ${item.credentialId}"
                    val credLayout = android.text.StaticLayout.Builder
                        .obtain(credText, 0, credText.length, credentialIdPaint, width.toInt().coerceAtLeast(1))
                        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1f)
                        .setIncludePad(false)
                        .build()
                    
                    canvas.save()
                    canvas.translate(x, currentY)
                    credLayout.draw(canvas)
                    canvas.restore()
                    
                    currentY += credLayout.height.toFloat()
                }
            }

            CertificationDisplayStyle.DETAILED -> {
                // Name and Status Badge on same row
                if (item.name.isNotEmpty()) {
                    val padding = mapper.borderWidthToPdfPoints(8f)
                    
                    // Calculate badge dimensions first if showing expiry status
                    var availableTextWidth = width
                    var badgeX = 0f
                    var statusWidth = 0f
                    var statusPaint: TextPaint? = null
                    var statusText = ""
                    var statusColor = 0L
                    var badgeHeight = 0f
                    
                    if (element.showExpiryStatus && item.expiryDate.isNotEmpty()) {
                        statusText = if (item.isActive) "Active" else "Expired"
                        statusColor = if (item.isActive) element.activeStatusColor else element.expiredStatusColor
                        statusPaint = TextPaint(expiryStatusPaint).apply {
                            color = context.colorConverter.toIntColorWithOpacity(statusColor, 1f)
                        }
                        
                        statusWidth = statusPaint.measureText(statusText)
                        val verticalPadding = mapper.borderWidthToPdfPoints(6f)
                        
                        // Calculate badge height accounting for font metrics
                        val fontMetrics = statusPaint.fontMetrics
                        val textActualHeight = fontMetrics.descent - fontMetrics.ascent
                        badgeHeight = textActualHeight + verticalPadding * 2
                        
                        val badgeWidth = statusWidth + padding * 2
                        availableTextWidth = width - badgeWidth - padding // Reserve space for badge + spacer
                        badgeX = x + width - statusWidth - padding
                    }
                    
                    // Draw name text with wrapping using StaticLayout
                    val layout = android.text.StaticLayout.Builder
                        .obtain(item.name, 0, item.name.length, namePaint, availableTextWidth.toInt().coerceAtLeast(1))
                        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1f)
                        .setIncludePad(false)
                        .build()
                    
                    canvas.save()
                    canvas.translate(x, currentY)
                    layout.draw(canvas)
                    canvas.restore()
                    
                    val textHeight = layout.height.toFloat()
                    
                    // Draw status badge on the right (aligned to top)
                    if (element.showExpiryStatus && item.expiryDate.isNotEmpty() && statusPaint != null) {
                        // Draw badge background
                        val badgePaint = Paint().apply {
                            color = context.colorConverter.toIntColorWithOpacity(statusColor, 0.2f)
                            style = Paint.Style.FILL
                            isAntiAlias = true
                        }
                        val cornerRadius = mapper.borderWidthToPdfPoints(12f)
                        val verticalPadding = mapper.borderWidthToPdfPoints(6f)
                        
                        val badgeBounds = RectF(
                            badgeX - padding,
                            currentY,
                            badgeX + statusWidth + padding,
                            currentY + badgeHeight
                        )
                        canvas.drawRoundRect(badgeBounds, cornerRadius, cornerRadius, badgePaint)
                        
                        // Draw badge text centered vertically in badge
                        val fontMetrics = statusPaint.fontMetrics
                        val textY = currentY + badgeHeight / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
                        canvas.drawText(statusText, badgeX, textY, statusPaint)
                    }
                    
                    currentY += textHeight
                    
                    // Add spacing if there's more content below
                    if (item.issuer.isNotEmpty() ||
                        (element.showIssueDate && item.issueDate.isNotEmpty()) ||
                        (element.showExpiryDate && item.expiryDate.isNotEmpty()) ||
                        (element.showCredentialId && item.credentialId.isNotEmpty()) ||
                        (element.showVerificationLink && item.verificationLink.isNotEmpty())) {
                        currentY += itemSpacing
                    }
                }

                // Issuer
                if (item.issuer.isNotEmpty()) {
                    val issuerMetrics = issuerPaint.fontMetrics
                    val issuerBaseline = currentY - issuerMetrics.ascent
                    canvas.drawText(item.issuer, x, issuerBaseline, issuerPaint)
                    
                    val issuerHeight = issuerMetrics.descent - issuerMetrics.ascent
                    currentY += issuerHeight
                    
                    // Add spacing if there's more content below
                    if ((element.showIssueDate && item.issueDate.isNotEmpty()) ||
                        (element.showExpiryDate && item.expiryDate.isNotEmpty()) ||
                        (element.showCredentialId && item.credentialId.isNotEmpty()) ||
                        (element.showVerificationLink && item.verificationLink.isNotEmpty())) {
                        currentY += itemSpacing
                    }
                }

                // Issue Date
                if (element.showIssueDate && item.issueDate.isNotEmpty()) {
                    val dateText = "Issued: ${formatDate(item.issueDate, element.dateFormat)}"
                    val dateMetrics = datePaint.fontMetrics
                    val dateBaseline = currentY - dateMetrics.ascent
                    canvas.drawText(dateText, x, dateBaseline, datePaint)
                    
                    val dateHeight = dateMetrics.descent - dateMetrics.ascent
                    currentY += dateHeight
                    
                    // Add spacing if there's more content below
                    if ((element.showExpiryDate && item.expiryDate.isNotEmpty()) ||
                        (element.showCredentialId && item.credentialId.isNotEmpty()) ||
                        (element.showVerificationLink && item.verificationLink.isNotEmpty())) {
                        currentY += itemSpacing
                    }
                }

                // Expiry Date
                if (element.showExpiryDate && item.expiryDate.isNotEmpty()) {
                    val dateText = "Expires: ${formatDate(item.expiryDate, element.dateFormat)}"
                    val dateMetrics = datePaint.fontMetrics
                    val dateBaseline = currentY - dateMetrics.ascent
                    canvas.drawText(dateText, x, dateBaseline, datePaint)
                    
                    val dateHeight = dateMetrics.descent - dateMetrics.ascent
                    currentY += dateHeight
                    
                    // Add spacing if there's more content below
                    if ((element.showCredentialId && item.credentialId.isNotEmpty()) ||
                        (element.showVerificationLink && item.verificationLink.isNotEmpty())) {
                        currentY += itemSpacing
                    }
                }

                // Credential ID
                if (element.showCredentialId && item.credentialId.isNotEmpty()) {
                    val credText = "Credential ID: ${item.credentialId}"
                    val credLayout = android.text.StaticLayout.Builder
                        .obtain(credText, 0, credText.length, credentialIdPaint, width.toInt().coerceAtLeast(1))
                        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1f)
                        .setIncludePad(false)
                        .build()
                    
                    canvas.save()
                    canvas.translate(x, currentY)
                    credLayout.draw(canvas)
                    canvas.restore()
                    
                    currentY += credLayout.height.toFloat()
                    
                    // Add spacing if there's more content below
                    if (element.showVerificationLink && item.verificationLink.isNotEmpty()) {
                        currentY += itemSpacing
                    }
                }

                // Verification Link
                if (element.showVerificationLink && item.verificationLink.isNotEmpty()) {
                    val linkText = "Verify: ${item.verificationLink}"
                    val linkLayout = android.text.StaticLayout.Builder
                        .obtain(linkText, 0, linkText.length, linkPaint, width.toInt().coerceAtLeast(1))
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
            }
        }

        return currentY - y
    }



    private fun calculateTotalHeight(
        element: ResumeElement.CertificationElement,
        bounds: RectF,
        namePaint: TextPaint,
        issuerPaint: TextPaint,
        datePaint: TextPaint,
        credentialIdPaint: TextPaint,
        linkPaint: TextPaint,
        expiryStatusPaint: TextPaint,
        mapper: GridCoordinateMapper
    ): Float {
        val spacing = mapper.borderWidthToPdfPoints(element.spacing)
        val itemSpacing = mapper.borderWidthToPdfPoints(element.itemSpacing)
        val padding = mapper.borderWidthToPdfPoints(8f)
        var totalHeight = 0f

        element.items.forEachIndexed { index, item ->
            var itemHeight = 0f
            
            // Calculate available width for text (accounting for badge if present)
            var availableTextWidth = bounds.width()
            if (element.showExpiryStatus && item.expiryDate.isNotEmpty()) {
                val statusText = if (item.isActive) "Active" else "Expired"
                val statusWidth = expiryStatusPaint.measureText(statusText)
                val badgeWidth = statusWidth + padding * 2
                availableTextWidth = bounds.width() - badgeWidth - padding
            }

            when (element.displayStyle) {
                CertificationDisplayStyle.STANDARD -> {
                    var partCount = 0
                    
                    if (item.name.isNotEmpty()) {
                        val layout = android.text.StaticLayout.Builder
                            .obtain(item.name, 0, item.name.length, namePaint, availableTextWidth.toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += layout.height.toFloat()
                        partCount++
                    }
                    if (item.issuer.isNotEmpty()) {
                        val issuerMetrics = issuerPaint.fontMetrics
                        itemHeight += issuerMetrics.descent - issuerMetrics.ascent
                        partCount++
                    }
                    if (element.showCredentialId && item.credentialId.isNotEmpty()) {
                        val credText = "Credential ID: ${item.credentialId}"
                        val credLayout = android.text.StaticLayout.Builder
                            .obtain(credText, 0, credText.length, credentialIdPaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += credLayout.height.toFloat()
                        partCount++
                    }
                    if (element.showVerificationLink && item.verificationLink.isNotEmpty()) {
                        val linkLayout = android.text.StaticLayout.Builder
                            .obtain(item.verificationLink, 0, item.verificationLink.length, linkPaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += linkLayout.height.toFloat()
                        partCount++
                    }
                    
                    // Add spacing between parts (not after last)
                    if (partCount > 1) {
                        itemHeight += itemSpacing * (partCount - 1)
                    }
                }
                CertificationDisplayStyle.COMPACT -> {
                    val halfSpacing = itemSpacing / 2f
                    var partCount = 0
                    
                    val nameIssuer = buildString {
                        if (item.name.isNotEmpty()) append(item.name)
                        if (item.name.isNotEmpty() && item.issuer.isNotEmpty()) append(" - ")
                        if (item.issuer.isNotEmpty()) append(item.issuer)
                    }
                    if (nameIssuer.isNotEmpty()) {
                        val layout = android.text.StaticLayout.Builder
                            .obtain(nameIssuer, 0, nameIssuer.length, namePaint, availableTextWidth.toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += layout.height.toFloat()
                        partCount++
                    }
                    if (element.showIssueDate && item.issueDate.isNotEmpty()) {
                        val dateMetrics = datePaint.fontMetrics
                        itemHeight += dateMetrics.descent - dateMetrics.ascent
                        partCount++
                    }
                    if (element.showCredentialId && item.credentialId.isNotEmpty()) {
                        val credText = "ID: ${item.credentialId}"
                        val credLayout = android.text.StaticLayout.Builder
                            .obtain(credText, 0, credText.length, credentialIdPaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += credLayout.height.toFloat()
                        partCount++
                    }
                    
                    // Add half spacing between parts (not after last)
                    if (partCount > 1) {
                        itemHeight += halfSpacing * (partCount - 1)
                    }
                }
                CertificationDisplayStyle.DETAILED -> {
                    var partCount = 0
                    
                    if (item.name.isNotEmpty()) {
                        val layout = android.text.StaticLayout.Builder
                            .obtain(item.name, 0, item.name.length, namePaint, availableTextWidth.toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += layout.height.toFloat()
                        partCount++
                    }
                    if (item.issuer.isNotEmpty()) {
                        val issuerMetrics = issuerPaint.fontMetrics
                        itemHeight += issuerMetrics.descent - issuerMetrics.ascent
                        partCount++
                    }
                    if (element.showIssueDate && item.issueDate.isNotEmpty()) {
                        val dateMetrics = datePaint.fontMetrics
                        itemHeight += dateMetrics.descent - dateMetrics.ascent
                        partCount++
                    }
                    if (element.showExpiryDate && item.expiryDate.isNotEmpty()) {
                        val dateMetrics = datePaint.fontMetrics
                        itemHeight += dateMetrics.descent - dateMetrics.ascent
                        partCount++
                    }
                    if (element.showCredentialId && item.credentialId.isNotEmpty()) {
                        val credText = "Credential ID: ${item.credentialId}"
                        val credLayout = android.text.StaticLayout.Builder
                            .obtain(credText, 0, credText.length, credentialIdPaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += credLayout.height.toFloat()
                        partCount++
                    }
                    if (element.showVerificationLink && item.verificationLink.isNotEmpty()) {
                        val linkText = "Verify: ${item.verificationLink}"
                        val linkLayout = android.text.StaticLayout.Builder
                            .obtain(linkText, 0, linkText.length, linkPaint, bounds.width().toInt().coerceAtLeast(1))
                            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1f)
                            .setIncludePad(false)
                            .build()
                        itemHeight += linkLayout.height.toFloat()
                        partCount++
                    }
                    
                    // Add spacing between parts (not after last)
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
        element: ResumeElement.CertificationElement,
        availableWidth: Float,
        namePaint: TextPaint,
        issuerPaint: TextPaint,
        datePaint: TextPaint,
        credentialIdPaint: TextPaint,
        linkPaint: TextPaint,
        expiryStatusPaint: TextPaint,
        mapper: GridCoordinateMapper
    ): Float {
        var maxWidth = 0f
        val padding = mapper.borderWidthToPdfPoints(8f)

        element.items.forEach { item ->
            when (element.displayStyle) {
                CertificationDisplayStyle.STANDARD, CertificationDisplayStyle.DETAILED -> {
                    if (item.name.isNotEmpty()) {
                        var nameRowWidth = namePaint.measureText(item.name)
                        
                        // Add badge width if showing expiry status
                        if (element.showExpiryStatus && item.expiryDate.isNotEmpty()) {
                            val statusText = if (item.isActive) "Active" else "Expired"
                            val statusWidth = expiryStatusPaint.measureText(statusText)
                            nameRowWidth += statusWidth + padding * 3 // badge padding + spacer
                        }
                        
                        maxWidth = maxOf(maxWidth, nameRowWidth)
                    }
                    
                    if (item.issuer.isNotEmpty()) {
                        var issuerRowWidth = issuerPaint.measureText(item.issuer)
                        
                        // Add date width if showing issue date (STANDARD only)
                        if (element.displayStyle == CertificationDisplayStyle.STANDARD && 
                            element.showIssueDate && item.issueDate.isNotEmpty()) {
                            val dateText = formatDate(item.issueDate, element.dateFormat)
                            issuerRowWidth += datePaint.measureText(dateText) + padding
                        }
                        
                        maxWidth = maxOf(maxWidth, issuerRowWidth)
                    }
                }
                CertificationDisplayStyle.COMPACT -> {
                    val nameIssuer = buildString {
                        if (item.name.isNotEmpty()) append(item.name)
                        if (item.name.isNotEmpty() && item.issuer.isNotEmpty()) append(" - ")
                        if (item.issuer.isNotEmpty()) append(item.issuer)
                    }
                    
                    if (nameIssuer.isNotEmpty()) {
                        var nameRowWidth = namePaint.measureText(nameIssuer)
                        
                        // Add badge width if showing expiry status
                        if (element.showExpiryStatus && item.expiryDate.isNotEmpty()) {
                            val statusText = if (item.isActive) "Active" else "Expired"
                            val statusWidth = expiryStatusPaint.measureText(statusText)
                            nameRowWidth += statusWidth + padding * 3 // badge padding + spacer
                        }
                        
                        maxWidth = maxOf(maxWidth, nameRowWidth)
                    }
                }
            }

            if (element.displayStyle == CertificationDisplayStyle.DETAILED) {
                if (element.showIssueDate && item.issueDate.isNotEmpty()) {
                    val dateText = "Issued: ${formatDate(item.issueDate, element.dateFormat)}"
                    maxWidth = maxOf(maxWidth, datePaint.measureText(dateText))
                }

                if (element.showExpiryDate && item.expiryDate.isNotEmpty()) {
                    val dateText = "Expires: ${formatDate(item.expiryDate, element.dateFormat)}"
                    maxWidth = maxOf(maxWidth, datePaint.measureText(dateText))
                }
            }
            
            if (element.displayStyle == CertificationDisplayStyle.COMPACT) {
                if (element.showIssueDate && item.issueDate.isNotEmpty()) {
                    val dateText = formatDate(item.issueDate, element.dateFormat)
                    maxWidth = maxOf(maxWidth, datePaint.measureText(dateText))
                }
            }

            if (element.showCredentialId && item.credentialId.isNotEmpty()) {
                val credText = if (element.displayStyle == CertificationDisplayStyle.COMPACT) {
                    "ID: ${item.credentialId}"
                } else {
                    "Credential ID: ${item.credentialId}"
                }
                maxWidth = maxOf(maxWidth, credentialIdPaint.measureText(credText))
            }

            if (element.showVerificationLink && item.verificationLink.isNotEmpty()) {
                val linkText = if (element.displayStyle == CertificationDisplayStyle.DETAILED) {
                    "Verify: ${item.verificationLink}"
                } else {
                    item.verificationLink
                }
                maxWidth = maxOf(maxWidth, linkPaint.measureText(linkText))
            }
        }

        return minOf(maxWidth, availableWidth)
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

