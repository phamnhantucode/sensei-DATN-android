package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.*

/**
 * Renders a language element on the resume
 * Displays a list of languages with proficiency levels in various visual styles
 */
@Composable
fun LanguageElementRenderer(
    element: ResumeElement.LanguageElement,
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
        when (element.displayStyle) {
            LanguageDisplayStyle.TEXT_LABELS -> {
                TextLabelsLanguageLayout(element, zoomLevel)
            }
            LanguageDisplayStyle.PROGRESS_BARS -> {
                ProgressBarsLanguageLayout(element, zoomLevel)
            }
            LanguageDisplayStyle.DOTS -> {
                DotsLanguageLayout(element, zoomLevel)
            }
            LanguageDisplayStyle.TAGS -> {
                TagsLanguageLayout(element, zoomLevel)
            }
        }
    }
}

/**
 * Text labels layout: Simple text layout (Language - Proficiency)
 */
@Composable
private fun TextLabelsLanguageLayout(
    element: ResumeElement.LanguageElement,
    zoomLevel: Float = 1f
) {
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
            if (item.name.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Language name
                    Text(
                        text = item.name,
                        style = element.languageStyle.toComposeTextStyle(zoomLevel),
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Proficiency label
                    val proficiencyText = getProficiencyText(item, element.proficiencyType)
                    if (proficiencyText.isNotEmpty()) {
                        Text(
                            text = proficiencyText,
                            style = element.proficiencyLabelStyle.toComposeTextStyle(zoomLevel)
                        )
                    }
                }

                // Add spacing between items (but not after last item)
                if (index < element.items.size - 1) {
                    Spacer(modifier = Modifier.height(element.spacing.dp))
                }
            }
        }
    }
}

/**
 * Progress bars layout: Visual bars showing proficiency
 */
@Composable
private fun ProgressBarsLanguageLayout(
    element: ResumeElement.LanguageElement,
    zoomLevel: Float = 1f
) {
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
            if (item.name.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy((2 * zoomLevel).dp)
                ) {
                    // Language name and proficiency label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = element.languageStyle.toComposeTextStyle(zoomLevel)
                        )

                        val proficiencyText = getProficiencyText(item, element.proficiencyType)
                        if (proficiencyText.isNotEmpty()) {
                            Text(
                                text = proficiencyText,
                                style = element.proficiencyLabelStyle.toComposeTextStyle(zoomLevel)
                            )
                        }
                    }

                    // Progress bar
                    val progressBarColor = element.progressBarColor?.let { Color(it) } ?: Color.Blue
                    val progressBarBackgroundColor = element.progressBarBackgroundColor?.let { Color(it) } ?: Color.LightGray

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(element.progressBarHeight.dp)
                            .clip(RoundedCornerShape(element.progressBarCornerRadius.dp))
                            .background(progressBarBackgroundColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.proficiency.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(element.progressBarCornerRadius.dp))
                                .background(progressBarColor)
                        )
                    }
                }

                // Add spacing between items (but not after last item)
                if (index < element.items.size - 1) {
                    Spacer(modifier = Modifier.height(element.spacing.dp))
                }
            }
        }
    }
}

/**
 * Dots layout: Dot indicators for proficiency
 */
@Composable
private fun DotsLanguageLayout(
    element: ResumeElement.LanguageElement,
    zoomLevel: Float = 1f
) {
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
            if (item.name.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Language name
                    Text(
                        text = item.name,
                        style = element.languageStyle.toComposeTextStyle(zoomLevel),
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Dot rating
                    val filledDots = (item.proficiency * element.maxDots).toInt().coerceIn(0, element.maxDots)
                    val progressBarColor = element.progressBarColor?.let { Color(it) } ?: Color.Blue
                    val progressBarBackgroundColor = element.progressBarBackgroundColor?.let { Color(it) } ?: Color.LightGray

                    Row(
                        horizontalArrangement = Arrangement.spacedBy((4 * zoomLevel).dp)
                    ) {
                        repeat(element.maxDots) { dotIndex ->
                            Box(
                                modifier = Modifier
                                    .size(element.dotSize.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (dotIndex < filledDots) progressBarColor
                                        else progressBarBackgroundColor
                                    )
                            )
                        }
                    }
                }

                // Add spacing between items (but not after last item)
                if (index < element.items.size - 1) {
                    Spacer(modifier = Modifier.height(element.spacing.dp))
                }
            }
        }
    }
}

/**
 * Tags layout: Chip-style tags with proficiency
 */
@Composable
private fun TagsLanguageLayout(
    element: ResumeElement.LanguageElement,
    zoomLevel: Float = 1f
) {
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

    FlowRow(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(element.spacing.dp),
        verticalArrangement = Arrangement.spacedBy((element.spacing / 2).dp)
    ) {
        element.items.forEach { item ->
            if (item.name.isNotEmpty()) {
                val bgColor = element.tagBackgroundColor?.let { Color(it) } ?: Color.LightGray
                val borderColor = element.tagBorderColor?.let { Color(it) }

                Column(
                    modifier = Modifier
                        .then(
                            if (borderColor != null && element.tagBorderWidth > 0) {
                                Modifier.border(
                                    width = element.tagBorderWidth.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(element.tagCornerRadius.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            color = bgColor,
                            shape = RoundedCornerShape(element.tagCornerRadius.dp)
                        )
                        .padding(horizontal = (12 * zoomLevel).dp, vertical = (6 * zoomLevel).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Language name
                    Text(
                        text = item.name,
                        style = element.languageStyle.toComposeTextStyle(zoomLevel)
                    )

                    // Proficiency label
                    val proficiencyText = getProficiencyText(item, element.proficiencyType)
                    if (proficiencyText.isNotEmpty()) {
                        Text(
                            text = proficiencyText,
                            style = element.proficiencyLabelStyle.toComposeTextStyle(zoomLevel)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Get proficiency text based on proficiency type
 */
private fun getProficiencyText(item: LanguageItem, proficiencyType: LanguageProficiencyType): String {
    return when (proficiencyType) {
        LanguageProficiencyType.TEXT -> item.proficiencyLabel
        LanguageProficiencyType.CEFR -> item.cefrLevel ?: item.proficiencyLabel
        LanguageProficiencyType.NUMERIC -> {
            val percentage = (item.proficiency * 100).toInt()
            "$percentage%"
        }
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
