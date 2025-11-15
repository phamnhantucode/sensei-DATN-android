package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.*

/**
 * Renders a skill element on the resume
 * Displays a list of skills with various visual styles (tags, bars, list, etc.)
 */
@Composable
fun SkillElementRenderer(
    element: ResumeElement.SkillElement,
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
            SkillDisplayStyle.LIST -> {
                SkillListLayout(element, zoomLevel)
            }
            SkillDisplayStyle.TAGS -> {
                SkillTagsLayout(element, zoomLevel)
            }
            SkillDisplayStyle.PROGRESS_BARS -> {
                SkillProgressBarsLayout(element, zoomLevel)
            }
            SkillDisplayStyle.DOTS -> {
                SkillDotsLayout(element, zoomLevel)
            }
            SkillDisplayStyle.GROUPED -> {
                SkillGroupedLayout(element, zoomLevel)
            }
        }
    }
}

/**
 * Simple list layout: Skills as a bulleted list
 */
@Composable
private fun SkillListLayout(
    element: ResumeElement.SkillElement,
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
        element.items.forEach { item ->
            if (item.name.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((4 * zoomLevel).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = (element.spacing / 2 * zoomLevel).dp)
                ) {
                    // Bullet
                    if (element.showBullets) {
                        Text(
                            text = getBulletCharacter(element.bulletStyle),
                            style = element.skillStyle.toComposeTextStyle(zoomLevel)
                        )
                    }

                    // Skill name
                    Text(
                        text = item.name,
                        style = element.skillStyle.toComposeTextStyle(zoomLevel)
                    )
                }
            }
        }
    }
}

/**
 * Tags layout: Skills as rounded tags/chips
 */
@Composable
private fun SkillTagsLayout(
    element: ResumeElement.SkillElement,
    zoomLevel: Float = 1f
) {
    val horizontalArrangement = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
        HorizontalAlignment.START -> Arrangement.Start
        HorizontalAlignment.CENTER -> Arrangement.Center
        HorizontalAlignment.END -> Arrangement.End
    }

    // Use a flow-like layout with Row and wrapping
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(element.spacing.dp)
    ) {
        var currentRow = mutableListOf<SkillItem>()
        val rows = mutableListOf<List<SkillItem>>()
        
        element.items.forEach { item ->
            currentRow.add(item)
            // Simple wrapping - in real implementation, measure text width
            if (currentRow.size >= 4) {
                rows.add(currentRow.toList())
                currentRow = mutableListOf()
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        rows.forEach { rowItems ->
            Row(
                horizontalArrangement = horizontalArrangement,
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { item ->
                    SkillTag(
                        item = item,
                        element = element,
                        zoomLevel = zoomLevel
                    )
                    Spacer(modifier = Modifier.width(element.spacing.dp))
                }
            }
        }
    }
}

/**
 * Individual skill tag/chip
 */
@Composable
private fun SkillTag(
    item: SkillItem,
    element: ResumeElement.SkillElement,
    zoomLevel: Float = 1f
) {
    val tagBackgroundColor = element.tagBackgroundColor?.let { Color(it) } 
        ?: Color(0xFFE3F2FD)
    val tagBorderColor = element.tagBorderColor?.let { Color(it) }

    Box(
        modifier = Modifier
            .background(
                color = tagBackgroundColor,
                shape = RoundedCornerShape(element.tagCornerRadius.dp)
            )
            .then(
                if (tagBorderColor != null && element.tagBorderWidth > 0) {
                    Modifier.border(
                        width = element.tagBorderWidth.dp,
                        color = tagBorderColor,
                        shape = RoundedCornerShape(element.tagCornerRadius.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = (12 * zoomLevel).dp, vertical = (6 * zoomLevel).dp)
    ) {
        Text(
            text = item.name,
            style = element.skillStyle.toComposeTextStyle(zoomLevel)
        )
    }
}

/**
 * Progress bars layout: Skills with proficiency bars
 */
@Composable
private fun SkillProgressBarsLayout(
    element: ResumeElement.SkillElement,
    zoomLevel: Float = 1f
) {
    val horizontalAlignmentForColumn = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
        HorizontalAlignment.START -> Alignment.Start
        HorizontalAlignment.CENTER -> Alignment.CenterHorizontally
        HorizontalAlignment.END -> Alignment.End
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(element.spacing.dp),
        horizontalAlignment = horizontalAlignmentForColumn
    ) {
        element.items.forEach { item ->
            if (item.name.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Skill name
                    Text(
                        text = item.name,
                        style = element.skillStyle.toComposeTextStyle(zoomLevel),
                        modifier = Modifier.padding(bottom = (4 * zoomLevel).dp)
                    )

                    // Progress bar
                    val proficiency = item.proficiency?.coerceIn(0f, 1f) ?: 0.5f
                    val progressColor = element.progressBarColor?.let { Color(it) } 
                        ?: Color(0xFF2196F3)
                    val progressBackgroundColor = element.progressBarBackgroundColor?.let { Color(it) } 
                        ?: Color(0xFFE0E0E0)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(element.progressBarHeight.dp)
                            .clip(RoundedCornerShape(element.progressBarCornerRadius.dp))
                            .background(progressBackgroundColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(proficiency)
                                .fillMaxHeight()
                                .background(progressColor)
                        )
                    }

                    // Optional proficiency label
                    if (element.showProficiencyLabel && item.proficiencyLabel.isNotEmpty()) {
                        Text(
                            text = item.proficiencyLabel,
                            style = element.proficiencyLabelStyle.toComposeTextStyle(zoomLevel),
                            modifier = Modifier.padding(top = (2 * zoomLevel).dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dots layout: Skills with dot-based proficiency rating
 */
@Composable
private fun SkillDotsLayout(
    element: ResumeElement.SkillElement,
    zoomLevel: Float = 1f
) {
    val horizontalAlignmentForColumn = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
        HorizontalAlignment.START -> Alignment.Start
        HorizontalAlignment.CENTER -> Alignment.CenterHorizontally
        HorizontalAlignment.END -> Alignment.End
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(element.spacing.dp),
        horizontalAlignment = horizontalAlignmentForColumn
    ) {
        element.items.forEach { item ->
            if (item.name.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skill name
                    Text(
                        text = item.name,
                        style = element.skillStyle.toComposeTextStyle(zoomLevel),
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((4 * zoomLevel).dp)
                    ) {
                        val proficiency = item.proficiency?.coerceIn(0f, 1f) ?: 0.5f
                        val filledDots = (proficiency * element.maxDots).toInt()

                        repeat(element.maxDots) { index ->
                            val dotColor = if (index < filledDots) {
                                element.progressBarColor?.let { Color(it) } ?: Color(0xFF2196F3)
                            } else {
                                element.progressBarBackgroundColor?.let { Color(it) } ?: Color(0xFFE0E0E0)
                            }

                            Box(
                                modifier = Modifier
                                    .size(element.dotSize.dp)
                                    .background(dotColor, shape = androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grouped layout: Skills organized by category
 */
@Composable
private fun SkillGroupedLayout(
    element: ResumeElement.SkillElement,
    zoomLevel: Float = 1f
) {
    val horizontalAlignmentForColumn = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
        HorizontalAlignment.START -> Alignment.Start
        HorizontalAlignment.CENTER -> Alignment.CenterHorizontally
        HorizontalAlignment.END -> Alignment.End
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(element.groupSpacing.dp),
        horizontalAlignment = horizontalAlignmentForColumn
    ) {
        element.items.groupBy { it.category }.forEach { (category, skills) ->
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category header
                if (category.isNotEmpty()) {
                    Text(
                        text = category,
                        style = element.categoryStyle.toComposeTextStyle(zoomLevel),
                        modifier = Modifier.padding(bottom = (4 * zoomLevel).dp)
                    )
                }

                // Skills in this category (as simple list)
                skills.forEach { item ->
                    if (item.name.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((4 * zoomLevel).dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = (element.spacing / 2 * zoomLevel).dp)
                        ) {
                            if (element.showBullets) {
                                Text(
                                    text = getBulletCharacter(element.bulletStyle),
                                    style = element.skillStyle.toComposeTextStyle(zoomLevel)
                                )
                            }
                            Text(
                                text = item.name,
                                style = element.skillStyle.toComposeTextStyle(zoomLevel)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get bullet character based on bullet style
 */
private fun getBulletCharacter(bulletStyle: BulletStyle): String {
    return when (bulletStyle) {
        BulletStyle.DISC -> "•"
        BulletStyle.DASH -> "-"
        BulletStyle.ARROW -> "→"
        BulletStyle.CHEVRON -> "›"
        BulletStyle.NUMBERED -> "1."
        BulletStyle.CUSTOM_ICON -> "•"
        BulletStyle.NONE -> ""
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
