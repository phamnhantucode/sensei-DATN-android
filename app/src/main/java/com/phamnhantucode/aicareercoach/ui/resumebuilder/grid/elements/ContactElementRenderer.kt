package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.*

/**
 * Renders a contact element on the resume
 * Displays a list of contact items (phone, email, address, social links, etc.)
 */
@Composable
fun ContactElementRenderer(
    element: ResumeElement.ContactElement,
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
            .padding(8.dp)
    ) {
        when (element.orientation) {
            ContactOrientation.VERTICAL -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(element.spacing.dp)
                ) {
                    element.items.forEach { item ->
                        ContactItemRow(
                            item = item,
                            iconStyle = element.iconStyle,
                            textStyle = element.textStyle,
                            iconSize = element.iconSize
                        )
                    }
                }
            }
            ContactOrientation.HORIZONTAL -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(element.spacing.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    element.items.forEach { item ->
                        ContactItemRow(
                            item = item,
                            iconStyle = element.iconStyle,
                            textStyle = element.textStyle,
                            iconSize = element.iconSize
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders a single contact item (icon/label + value)
 */
@Composable
private fun ContactItemRow(
    item: ContactItem,
    iconStyle: ContactIconStyle,
    textStyle: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextStyle,
    iconSize: Float
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (iconStyle) {
            ContactIconStyle.ICON -> {
                // Show icon prefix
                if (item.iconName.isNotEmpty()) {
                    val icon = getContactIcon(item.type, item.iconName)
                    Icon(
                        imageVector = icon,
                        contentDescription = item.label.ifEmpty { item.type.name },
                        modifier = Modifier.size(iconSize.dp),
                        tint = Color(textStyle.color)
                    )
                }
            }
            ContactIconStyle.BOLD_LABEL -> {
                // Show bold label prefix
                if (item.label.isNotEmpty()) {
                    Text(
                        text = item.label,
                        style = textStyle.toComposeTextStyle().copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            ContactIconStyle.NONE -> {
                // No prefix
            }
        }

        // Contact value
        if (item.value.isNotEmpty()) {
            Text(
                text = item.value,
                style = textStyle.toComposeTextStyle()
            )
        }
    }
}

/**
 * Gets the appropriate Material Icon for a contact type
 */
private fun getContactIcon(type: ContactType, customIconName: String): ImageVector {
    // If custom icon name is provided, try to use it
    // For now, we'll use default icons based on type
    return when (type) {
        ContactType.PHONE -> Icons.Default.Phone
        ContactType.EMAIL -> Icons.Default.Email
        ContactType.ADDRESS -> Icons.Default.LocationOn
        ContactType.LINKEDIN -> Icons.Default.Work // LinkedIn icon approximation
        ContactType.GITHUB -> Icons.Default.Code // GitHub icon approximation
        ContactType.WEBSITE -> Icons.Default.Language
        ContactType.CUSTOM -> Icons.Default.Info
    }
}

/**
 * Converts custom TextStyle to Compose TextStyle
 */
private fun com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextStyle.toComposeTextStyle(): TextStyle {
    return TextStyle(
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        color = Color(color),
        lineHeight = lineHeight?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified,
        letterSpacing = letterSpacing.sp,
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None
    )
}
