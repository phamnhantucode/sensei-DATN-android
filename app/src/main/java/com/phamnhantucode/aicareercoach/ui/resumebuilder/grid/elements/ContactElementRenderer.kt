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
            .padding(8.dp)
    ) {
        // Map alignment enums to Compose alignment values (with null safety for backward compatibility)
        val horizontalArrangement = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> Arrangement.Start
            HorizontalAlignment.CENTER -> Arrangement.Center
            HorizontalAlignment.END -> Arrangement.End
        }

        val verticalArrangementForColumn = when (element.verticalAlignment ?: VerticalAlignment.CENTER) {
            VerticalAlignment.TOP -> Arrangement.Top
            VerticalAlignment.CENTER -> Arrangement.Center
            VerticalAlignment.BOTTOM -> Arrangement.Bottom
        }

        val verticalAlignmentForRow = when (element.verticalAlignment ?: VerticalAlignment.CENTER) {
            VerticalAlignment.TOP -> Alignment.Top
            VerticalAlignment.CENTER -> Alignment.CenterVertically
            VerticalAlignment.BOTTOM -> Alignment.Bottom
        }

        val horizontalAlignmentForColumn = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> Alignment.Start
            HorizontalAlignment.CENTER -> Alignment.CenterHorizontally
            HorizontalAlignment.END -> Alignment.End
        }

        when (element.orientation) {
            ContactOrientation.VERTICAL -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = verticalArrangementForColumn,
                    horizontalAlignment = horizontalAlignmentForColumn
                ) {
                    element.items.forEach { item ->
                        ContactItemRow(
                            item = item,
                            iconStyle = element.iconStyle,
                            textStyle = element.textStyle,
                            iconSize = element.iconSize,
                            iconAfterText = (element.horizontalAlignment ?: HorizontalAlignment.START) == HorizontalAlignment.END,
                            verticalAlignment = element.verticalAlignment ?: VerticalAlignment.CENTER,
                            zoomLevel = zoomLevel
                        )
                    }
                }
            }
            ContactOrientation.HORIZONTAL -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = horizontalArrangement,
                    verticalAlignment = verticalAlignmentForRow
                ) {
                    element.items.forEach { item ->
                        ContactItemRow(
                            item = item,
                            iconStyle = element.iconStyle,
                            textStyle = element.textStyle,
                            iconSize = element.iconSize,
                            iconAfterText = (element.horizontalAlignment ?: HorizontalAlignment.START) == HorizontalAlignment.END,
                            verticalAlignment = element.verticalAlignment ?: VerticalAlignment.CENTER,
                            zoomLevel = zoomLevel
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
    iconSize: Float,
    iconAfterText: Boolean = false,
    verticalAlignment: VerticalAlignment = VerticalAlignment.CENTER,
    zoomLevel: Float = 1f
) {
    val rowVerticalAlignment = when (verticalAlignment) {
        VerticalAlignment.TOP -> Alignment.Top
        VerticalAlignment.CENTER -> Alignment.CenterVertically
        VerticalAlignment.BOTTOM -> Alignment.Bottom
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = rowVerticalAlignment
    ) {
        // Render icon/label before text (default)
        if (!iconAfterText) {
            RenderIconOrLabel(item, iconStyle, textStyle, iconSize, zoomLevel)
        }

        // Contact value
        if (item.value.isNotEmpty()) {
            Text(
                text = item.value,
                style = textStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Render icon/label after text (when horizontalAlignment is END)
        if (iconAfterText) {
            RenderIconOrLabel(item, iconStyle, textStyle, iconSize, zoomLevel)
        }
    }
}

/**
 * Renders the icon or label for a contact item
 */
@Composable
private fun RenderIconOrLabel(
    item: ContactItem,
    iconStyle: ContactIconStyle,
    textStyle: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextStyle,
    iconSize: Float,
    zoomLevel: Float = 1f
) {
    when (iconStyle) {
        ContactIconStyle.ICON -> {
            // Show icon prefix
            if (item.iconName.isNotEmpty()) {
                val icon = getContactIcon(item.type, item.iconName)
                Icon(
                    imageVector = icon,
                    contentDescription = item.label.ifEmpty { item.type.name },
                    modifier = Modifier.size((iconSize * zoomLevel).dp),
                    tint = Color(textStyle.color)
                )
            }
        }
        ContactIconStyle.BOLD_LABEL -> {
            // Show bold label prefix
            if (item.label.isNotEmpty()) {
                Text(
                    text = item.label,
                    style = textStyle.toComposeTextStyle(zoomLevel).copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        ContactIconStyle.NONE -> {
            // No prefix
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
private fun com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextStyle.toComposeTextStyle(zoomLevel: Float = 1f): TextStyle {
    return TextStyle(
        fontSize = (fontSize * zoomLevel).sp,
        fontWeight = fontWeight,
        color = Color(color),
        lineHeight = lineHeight?.let { (it * zoomLevel).sp } ?: androidx.compose.ui.unit.TextUnit.Unspecified,
        letterSpacing = (letterSpacing * zoomLevel).sp,
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None
    )
}
