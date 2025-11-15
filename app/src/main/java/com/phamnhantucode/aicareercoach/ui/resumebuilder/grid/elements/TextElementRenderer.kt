package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextAlignment
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.VerticalTextAlignment

/**
 * Renders a text element on the resume
 */
@Composable
fun TextElementRenderer(
    element: ResumeElement.TextElement,
    isEditing: Boolean = false,
    onContentChange: (String) -> Unit = {},
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
            .padding((8 * zoomLevel).dp),
        contentAlignment = (element.verticalAlignment ?: VerticalTextAlignment.CENTER).toAlignment()
    ) {
        if (isEditing) {
            EditableText(
                text = element.content,
                textStyle = element.textStyle.toComposeTextStyle(zoomLevel),
                alignment = element.alignment,
                zoomLevel = zoomLevel,
                onContentChange = onContentChange
            )
        } else {
            StaticText(
                text = element.content,
                textStyle = element.textStyle.toComposeTextStyle(zoomLevel),
                alignment = element.alignment,
                maxLines = element.maxLines
            )
        }
    }
}

/**
 * Static text display (non-editable)
 */
@Composable
private fun StaticText(
    text: String,
    textStyle: TextStyle,
    alignment: TextAlignment,
    maxLines: Int?
) {
    Text(
        text = text,
        style = textStyle,
        textAlign = alignment.toTextAlign(),
        maxLines = maxLines ?: Int.MAX_VALUE,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Editable text field
 */
@Composable
private fun EditableText(
    text: String,
    textStyle: TextStyle,
    alignment: TextAlignment,
    zoomLevel: Float = 1f,
    onContentChange: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(text) }

    LaunchedEffect(text) {
        if (text != textValue) {
            textValue = text
        }
    }

    BasicTextField(
        value = textValue,
        onValueChange = {
            textValue = it
            onContentChange(it)
        },
        textStyle = textStyle.copy(
            textAlign = alignment.toTextAlign()
        ),
        cursorBrush = SolidColor(Color.Blue),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape((4 * zoomLevel).dp)
            )
            .padding((4 * zoomLevel).dp)
    )
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

/**
 * Converts custom TextAlignment to Compose TextAlign
 */
private fun TextAlignment.toTextAlign(): TextAlign {
    return when (this) {
        TextAlignment.LEFT -> TextAlign.Left
        TextAlignment.CENTER -> TextAlign.Center
        TextAlignment.RIGHT -> TextAlign.Right
        TextAlignment.JUSTIFY -> TextAlign.Justify
    }
}

/**
 * Converts VerticalTextAlignment to Compose Alignment
 */
private fun VerticalTextAlignment.toAlignment(): Alignment {
    return when (this) {
        VerticalTextAlignment.TOP -> Alignment.TopStart
        VerticalTextAlignment.CENTER -> Alignment.CenterStart
        VerticalTextAlignment.BOTTOM -> Alignment.BottomStart
    }
}
