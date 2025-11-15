package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Renders a certification element on the resume
 * Displays a list of certifications with name, issuer, dates, credential ID, and verification link
 */
@Composable
fun CertificationElementRenderer(
    element: ResumeElement.CertificationElement,
    zoomLevel: Float = 1f,
    modifier: Modifier = Modifier
) {
    val backgroundColor = element.style.backgroundColor?.let { Color(it) }
    val borderColor = element.style.borderColor?.let { Color(it) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (backgroundColor != null) {
                    Modifier.background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(element.style.borderRadius.dp)
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (borderColor != null && element.style.borderWidth > 0) {
                    Modifier
                        .border(
                            width = element.style.borderWidth.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(element.style.borderRadius.dp)
                        )
                } else {
                    Modifier
                }
            )
            .then(
                if (element.style.shadowBlur > 0) {
                    Modifier.shadow(
                        elevation = element.style.shadowBlur.dp,
                        shape = RoundedCornerShape(element.style.borderRadius.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding((8 * zoomLevel).dp)
    ) {
        // Map alignment enums to Compose alignment values
        val horizontalAlignmentForColumn = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> Alignment.Start
            HorizontalAlignment.CENTER -> Alignment.CenterHorizontally
            HorizontalAlignment.END -> Alignment.End
        }

        val verticalArrangementForColumn = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> Arrangement.Top
            VerticalAlignment.CENTER -> Arrangement.Center
            VerticalAlignment.BOTTOM -> Arrangement.Bottom
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = verticalArrangementForColumn,
            horizontalAlignment = horizontalAlignmentForColumn
        ) {
            element.items.forEachIndexed { index, item ->
                CertificationItemRenderer(
                    item = item,
                    element = element,
                    zoomLevel = zoomLevel
                )

                // Add spacing between items (but not after last item)
                if (index < element.items.size - 1) {
                    Spacer(modifier = Modifier.height(element.spacing.dp))
                }
            }
        }
    }
}

/**
 * Renders a single certification item based on display style
 */
@Composable
private fun CertificationItemRenderer(
    item: CertificationItem,
    element: ResumeElement.CertificationElement,
    zoomLevel: Float = 1f
) {
    when (element.displayStyle) {
        CertificationDisplayStyle.STANDARD -> {
            StandardCertificationLayout(item, element, zoomLevel)
        }
        CertificationDisplayStyle.COMPACT -> {
            CompactCertificationLayout(item, element, zoomLevel)
        }
        CertificationDisplayStyle.DETAILED -> {
            DetailedCertificationLayout(item, element, zoomLevel)
        }
    }
}

/**
 * Standard layout: Name and issuer on separate lines
 */
@Composable
private fun StandardCertificationLayout(
    item: CertificationItem,
    element: ResumeElement.CertificationElement,
    zoomLevel: Float = 1f
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(element.itemSpacing.dp)
    ) {
        // Certification Name and Expiry Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.name.isNotEmpty()) {
                Text(
                    text = item.name,
                    style = element.nameStyle.toComposeTextStyle(zoomLevel),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Expiry status badge
            if (element.showExpiryStatus && item.expiryDate.isNotEmpty()) {
                ExpiryStatusBadge(item, element, zoomLevel)
            }
        }

        // Issuer and Date Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.issuer.isNotEmpty()) {
                Text(
                    text = item.issuer,
                    style = element.issuerStyle.toComposeTextStyle(zoomLevel),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            if (element.showIssueDate && item.issueDate.isNotEmpty()) {
                Text(
                    text = formatDate(item.issueDate, element.dateFormat),
                    style = element.dateStyle.toComposeTextStyle(zoomLevel)
                )
            }
        }

        // Credential ID
        if (element.showCredentialId && item.credentialId.isNotEmpty()) {
            Text(
                text = "Credential ID: ${item.credentialId}",
                style = element.credentialIdStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Verification Link
        if (element.showVerificationLink && item.verificationLink.isNotEmpty()) {
            Text(
                text = item.verificationLink,
                style = element.linkStyle.toComposeTextStyle(zoomLevel)
            )
        }
    }
}

/**
 * Compact layout: Name and issuer on same line
 */
@Composable
private fun CompactCertificationLayout(
    item: CertificationItem,
    element: ResumeElement.CertificationElement,
    zoomLevel: Float = 1f
) {
    Column(
        verticalArrangement = Arrangement.spacedBy((element.itemSpacing / 2).dp)
    ) {
        // Name and Issuer on same row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Name + Issuer together
            if (item.name.isNotEmpty() || item.issuer.isNotEmpty()) {
                val nameIssuer = buildString {
                    if (item.name.isNotEmpty()) append(item.name)
                    if (item.name.isNotEmpty() && item.issuer.isNotEmpty()) append(" - ")
                    if (item.issuer.isNotEmpty()) append(item.issuer)
                }

                Text(
                    text = nameIssuer,
                    style = element.nameStyle.toComposeTextStyle(zoomLevel),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Expiry status badge
            if (element.showExpiryStatus && item.expiryDate.isNotEmpty()) {
                ExpiryStatusBadge(item, element, zoomLevel)
            }
        }

        // Date
        if (element.showIssueDate && item.issueDate.isNotEmpty()) {
            Text(
                text = formatDate(item.issueDate, element.dateFormat),
                style = element.dateStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Credential ID (condensed)
        if (element.showCredentialId && item.credentialId.isNotEmpty()) {
            Text(
                text = "ID: ${item.credentialId}",
                style = element.credentialIdStyle.toComposeTextStyle(zoomLevel)
            )
        }
    }
}

/**
 * Detailed layout: All fields prominently displayed
 */
@Composable
private fun DetailedCertificationLayout(
    item: CertificationItem,
    element: ResumeElement.CertificationElement,
    zoomLevel: Float = 1f
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(element.itemSpacing.dp)
    ) {
        // Certification Name
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.name.isNotEmpty()) {
                Text(
                    text = item.name,
                    style = element.nameStyle.toComposeTextStyle(zoomLevel),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Expiry status badge
            if (element.showExpiryStatus && item.expiryDate.isNotEmpty()) {
                ExpiryStatusBadge(item, element, zoomLevel)
            }
        }

        // Issuer
        if (item.issuer.isNotEmpty()) {
            Text(
                text = item.issuer,
                style = element.issuerStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Issue Date
        if (element.showIssueDate && item.issueDate.isNotEmpty()) {
            Text(
                text = "Issued: ${formatDate(item.issueDate, element.dateFormat)}",
                style = element.dateStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Expiry Date
        if (element.showExpiryDate && item.expiryDate.isNotEmpty()) {
            Text(
                text = "Expires: ${formatDate(item.expiryDate, element.dateFormat)}",
                style = element.dateStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Credential ID
        if (element.showCredentialId && item.credentialId.isNotEmpty()) {
            Text(
                text = "Credential ID: ${item.credentialId}",
                style = element.credentialIdStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Verification Link
        if (element.showVerificationLink && item.verificationLink.isNotEmpty()) {
            Text(
                text = "Verify: ${item.verificationLink}",
                style = element.linkStyle.toComposeTextStyle(zoomLevel)
            )
        }
    }
}

/**
 * Renders expiry status badge (Active/Expired)
 */
@Composable
private fun ExpiryStatusBadge(
    item: CertificationItem,
    element: ResumeElement.CertificationElement,
    zoomLevel: Float = 1f
) {
    val statusText = if (item.isActive) "Active" else "Expired"
    val statusColor = if (item.isActive) {
        Color(element.activeStatusColor)
    } else {
        Color(element.expiredStatusColor)
    }

    Box(
        modifier = Modifier
            .background(
                color = statusColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape((12 * zoomLevel).dp)
            )
            .padding(horizontal = (8 * zoomLevel).dp, vertical = (4 * zoomLevel).dp)
    ) {
        Text(
            text = statusText,
            style = element.expiryStatusStyle.toComposeTextStyle(zoomLevel).copy(
                color = statusColor
            )
        )
    }
}

/**
 * Format a single date string
 */
private fun formatDate(dateString: String, dateFormat: DateFormat): String {
    if (dateString.isEmpty()) return ""

    return try {
        val date = LocalDate.parse(dateString) // ISO format: yyyy-MM-dd
        when (dateFormat) {
            DateFormat.MMM_YYYY -> date.format(DateTimeFormatter.ofPattern("MMM yyyy"))
            DateFormat.MM_YYYY -> date.format(DateTimeFormatter.ofPattern("MM/yyyy"))
            DateFormat.FULL -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            DateFormat.SHORT -> date.format(DateTimeFormatter.ofPattern("M/yy"))
            DateFormat.YYYY -> date.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    } catch (e: Exception) {
        dateString // Return original if parsing fails
    }
}

/**
 * Convert custom TextStyle to Compose TextStyle
 */
private fun com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextStyle.toComposeTextStyle(
    zoomLevel: Float = 1f
): TextStyle {
    return TextStyle(
        fontSize = (fontSize * zoomLevel).sp,
        fontWeight = fontWeight,
        color = Color(color),
        lineHeight = lineHeight?.let { (it * zoomLevel).sp } ?: TextUnit.Unspecified,
        letterSpacing = (letterSpacing * zoomLevel).sp,
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None
    )
}
