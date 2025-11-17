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
                if (item.name.isNotEmpty()) {
                    canvas.drawText(item.name, x, currentY + namePaint.textSize, namePaint)
                    currentY += namePaint.textSize + itemSpacing
                }

                if (item.issuer.isNotEmpty()) {
                    canvas.drawText(item.issuer, x, currentY + issuerPaint.textSize, issuerPaint)
                    currentY += issuerPaint.textSize + itemSpacing
                }

                currentY += renderDatesAndStatus(canvas, item, element, x, currentY, width,
                    datePaint, expiryStatusPaint, itemSpacing, context)

                currentY += renderCredentialAndLink(canvas, item, element, x, currentY,
                    credentialIdPaint, linkPaint, itemSpacing)
            }

            CertificationDisplayStyle.COMPACT -> {
                val nameIssuer = buildString {
                    if (item.name.isNotEmpty()) append(item.name)
                    if (item.name.isNotEmpty() && item.issuer.isNotEmpty()) append(" - ")
                    if (item.issuer.isNotEmpty()) append(item.issuer)
                }

                if (nameIssuer.isNotEmpty()) {
                    canvas.drawText(nameIssuer, x, currentY + namePaint.textSize, namePaint)
                    currentY += namePaint.textSize + itemSpacing
                }

                currentY += renderDatesAndStatus(canvas, item, element, x, currentY, width,
                    datePaint, expiryStatusPaint, itemSpacing, context)
            }

            CertificationDisplayStyle.DETAILED -> {
                if (item.name.isNotEmpty()) {
                    canvas.drawText(item.name, x, currentY + namePaint.textSize, namePaint)
                    currentY += namePaint.textSize + itemSpacing
                }

                if (item.issuer.isNotEmpty()) {
                    canvas.drawText(item.issuer, x, currentY + issuerPaint.textSize, issuerPaint)
                    currentY += issuerPaint.textSize + itemSpacing
                }

                currentY += renderDatesAndStatus(canvas, item, element, x, currentY, width,
                    datePaint, expiryStatusPaint, itemSpacing, context)

                currentY += renderCredentialAndLink(canvas, item, element, x, currentY,
                    credentialIdPaint, linkPaint, itemSpacing)
            }
        }

        return currentY - y
    }

    private fun renderDatesAndStatus(
        canvas: Canvas,
        item: CertificationItem,
        element: ResumeElement.CertificationElement,
        x: Float,
        y: Float,
        width: Float,
        datePaint: TextPaint,
        expiryStatusPaint: TextPaint,
        itemSpacing: Float,
        context: PdfRenderContext
    ): Float {
        var currentY = y

        if (element.showIssueDate && item.issueDate.isNotEmpty()) {
            val dateText = "Issued: ${formatDate(item.issueDate, element.dateFormat)}"
            canvas.drawText(dateText, x, currentY + datePaint.textSize, datePaint)
            currentY += datePaint.textSize + itemSpacing
        }

        if (element.showExpiryDate && item.expiryDate.isNotEmpty()) {
            val dateText = "Expires: ${formatDate(item.expiryDate, element.dateFormat)}"
            canvas.drawText(dateText, x, currentY + datePaint.textSize, datePaint)
            currentY += datePaint.textSize + itemSpacing
        }

        if (element.showExpiryStatus && item.expiryDate.isNotEmpty()) {
            val isExpired = item.isExpired
            val statusText = if (isExpired) "Expired" else "Active"
            val statusColor = if (isExpired) element.expiredStatusColor else element.activeStatusColor

            val statusPaint = TextPaint(expiryStatusPaint).apply {
                color = context.colorConverter.toIntColorWithOpacity(statusColor, 1f)
            }

            canvas.drawText(statusText, x, currentY + statusPaint.textSize, statusPaint)
            currentY += statusPaint.textSize + itemSpacing
        }

        return currentY - y
    }

    private fun renderCredentialAndLink(
        canvas: Canvas,
        item: CertificationItem,
        element: ResumeElement.CertificationElement,
        x: Float,
        y: Float,
        credentialIdPaint: TextPaint,
        linkPaint: TextPaint,
        itemSpacing: Float
    ): Float {
        var currentY = y

        if (element.showCredentialId && item.credentialId.isNotEmpty()) {
            val credText = "Credential ID: ${item.credentialId}"
            canvas.drawText(credText, x, currentY + credentialIdPaint.textSize, credentialIdPaint)
            currentY += credentialIdPaint.textSize + itemSpacing
        }

        if (element.showVerificationLink && item.verificationLink.isNotEmpty()) {
            canvas.drawText(item.verificationLink, x, currentY + linkPaint.textSize, linkPaint)
            currentY += linkPaint.textSize + itemSpacing
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
        var totalHeight = 0f

        element.items.forEachIndexed { index, item ->
            var itemHeight = 0f

            when (element.displayStyle) {
                CertificationDisplayStyle.STANDARD, CertificationDisplayStyle.DETAILED -> {
                    if (item.name.isNotEmpty()) itemHeight += namePaint.textSize + itemSpacing
                    if (item.issuer.isNotEmpty()) itemHeight += issuerPaint.textSize + itemSpacing
                    if (element.showIssueDate && item.issueDate.isNotEmpty()) itemHeight += datePaint.textSize + itemSpacing
                    if (element.showExpiryDate && item.expiryDate.isNotEmpty()) itemHeight += datePaint.textSize + itemSpacing
                    if (element.showExpiryStatus && item.expiryDate.isNotEmpty()) itemHeight += expiryStatusPaint.textSize + itemSpacing
                    if (element.showCredentialId && item.credentialId.isNotEmpty()) itemHeight += credentialIdPaint.textSize + itemSpacing
                    if (element.showVerificationLink && item.verificationLink.isNotEmpty()) itemHeight += linkPaint.textSize + itemSpacing
                }
                CertificationDisplayStyle.COMPACT -> {
                    itemHeight += namePaint.textSize + itemSpacing
                    if (element.showIssueDate && item.issueDate.isNotEmpty()) itemHeight += datePaint.textSize + itemSpacing
                    if (element.showExpiryDate && item.expiryDate.isNotEmpty()) itemHeight += datePaint.textSize + itemSpacing
                    if (element.showExpiryStatus && item.expiryDate.isNotEmpty()) itemHeight += expiryStatusPaint.textSize + itemSpacing
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

        element.items.forEach { item ->
            when (element.displayStyle) {
                CertificationDisplayStyle.STANDARD, CertificationDisplayStyle.DETAILED -> {
                    if (item.name.isNotEmpty()) maxWidth = maxOf(maxWidth, namePaint.measureText(item.name))
                    if (item.issuer.isNotEmpty()) maxWidth = maxOf(maxWidth, issuerPaint.measureText(item.issuer))
                }
                CertificationDisplayStyle.COMPACT -> {
                    val nameIssuer = buildString {
                        if (item.name.isNotEmpty()) append(item.name)
                        if (item.name.isNotEmpty() && item.issuer.isNotEmpty()) append(" - ")
                        if (item.issuer.isNotEmpty()) append(item.issuer)
                    }
                    if (nameIssuer.isNotEmpty()) maxWidth = maxOf(maxWidth, namePaint.measureText(nameIssuer))
                }
            }

            if (element.showIssueDate && item.issueDate.isNotEmpty()) {
                val dateText = "Issued: ${formatDate(item.issueDate, element.dateFormat)}"
                maxWidth = maxOf(maxWidth, datePaint.measureText(dateText))
            }

            if (element.showExpiryDate && item.expiryDate.isNotEmpty()) {
                val dateText = "Expires: ${formatDate(item.expiryDate, element.dateFormat)}"
                maxWidth = maxOf(maxWidth, datePaint.measureText(dateText))
            }

            if (element.showCredentialId && item.credentialId.isNotEmpty()) {
                val credText = "Credential ID: ${item.credentialId}"
                maxWidth = maxOf(maxWidth, credentialIdPaint.measureText(credText))
            }

            if (element.showVerificationLink && item.verificationLink.isNotEmpty()) {
                maxWidth = maxOf(maxWidth, linkPaint.measureText(item.verificationLink))
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
