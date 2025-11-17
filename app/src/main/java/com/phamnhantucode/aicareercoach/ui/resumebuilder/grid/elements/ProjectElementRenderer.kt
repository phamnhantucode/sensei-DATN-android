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
 * Renders a project element on the resume
 * Displays a list of projects with name, description, technologies, highlights, and links
 */
@Composable
fun ProjectElementRenderer(
    element: ResumeElement.ProjectElement,
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
                ProjectItemRenderer(
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
 * Renders a single project item based on display style
 */
@Composable
private fun ProjectItemRenderer(
    item: ProjectItem,
    element: ResumeElement.ProjectElement,
    zoomLevel: Float = 1f
) {
    when (element.displayStyle) {
        ProjectDisplayStyle.STANDARD -> {
            StandardProjectLayout(item, element, zoomLevel)
        }
        ProjectDisplayStyle.COMPACT -> {
            CompactProjectLayout(item, element, zoomLevel)
        }
        ProjectDisplayStyle.DETAILED -> {
            DetailedProjectLayout(item, element, zoomLevel)
        }
    }
}

/**
 * Standard layout: Name on top, description below, then technologies and highlights
 */
@Composable
private fun StandardProjectLayout(
    item: ProjectItem,
    element: ResumeElement.ProjectElement,
    zoomLevel: Float = 1f
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(element.itemSpacing.dp)
    ) {
        // Project Name and Date Row
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

            if (element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isOngoing)) {
                Text(
                    text = formatDateRange(item, element),
                    style = element.dateStyle.toComposeTextStyle(zoomLevel)
                )
            }
        }

        // Description
        if (element.showDescription && item.description.isNotEmpty()) {
            Text(
                text = item.description,
                style = element.descriptionStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Technologies
        if (element.showTechnologies && item.technologies.isNotEmpty()) {
            TechnologyTags(
                technologies = item.technologies,
                element = element,
                zoomLevel = zoomLevel
            )
        }

        // Highlights
        if (item.highlights.isNotEmpty()) {
            HighlightList(
                highlights = item.highlights,
                element = element,
                zoomLevel = zoomLevel
            )
        }

        // Link
        if (element.showLink && item.link.isNotEmpty()) {
            Text(
                text = item.link,
                style = element.linkStyle.toComposeTextStyle(zoomLevel)
            )
        }
    }
}

/**
 * Compact layout: Condensed with minimal spacing
 */
@Composable
private fun CompactProjectLayout(
    item: ProjectItem,
    element: ResumeElement.ProjectElement,
    zoomLevel: Float = 1f
) {
    Column(
        verticalArrangement = Arrangement.spacedBy((element.itemSpacing / 2).dp)
    ) {
        // Name and Date on same row
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

            if (element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isOngoing)) {
                Text(
                    text = formatDateRange(item, element),
                    style = element.dateStyle.toComposeTextStyle(zoomLevel)
                )
            }
        }

        // Technologies (inline, no tags)
        if (element.showTechnologies && item.technologies.isNotEmpty()) {
            Text(
                text = item.technologies,
                style = element.technologyStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Description (shortened if too long)
        if (element.showDescription && item.description.isNotEmpty()) {
            Text(
                text = item.description,
                style = element.descriptionStyle.toComposeTextStyle(zoomLevel),
                maxLines = 2
            )
        }

        // Highlights (condensed)
        if (item.highlights.isNotEmpty()) {
            HighlightList(
                highlights = item.highlights,
                element = element,
                zoomLevel = zoomLevel
            )
        }
    }
}

/**
 * Detailed layout: All fields prominently displayed
 */
@Composable
private fun DetailedProjectLayout(
    item: ProjectItem,
    element: ResumeElement.ProjectElement,
    zoomLevel: Float = 1f
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(element.itemSpacing.dp)
    ) {
        // Project Name
        if (item.name.isNotEmpty()) {
            Text(
                text = item.name,
                style = element.nameStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Date
        if (element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isOngoing)) {
            Text(
                text = formatDateRange(item, element),
                style = element.dateStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Link
        if (element.showLink && item.link.isNotEmpty()) {
            Text(
                text = item.link,
                style = element.linkStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Technologies
        if (element.showTechnologies && item.technologies.isNotEmpty()) {
            TechnologyTags(
                technologies = item.technologies,
                element = element,
                zoomLevel = zoomLevel
            )
        }

        // Description
        if (element.showDescription && item.description.isNotEmpty()) {
            Text(
                text = item.description,
                style = element.descriptionStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Highlights
        if (item.highlights.isNotEmpty()) {
            HighlightList(
                highlights = item.highlights,
                element = element,
                zoomLevel = zoomLevel
            )
        }
    }
}

/**
 * Renders technology tags/chips
 */
@Composable
private fun TechnologyTags(
    technologies: String,
    element: ResumeElement.ProjectElement,
    zoomLevel: Float = 1f
) {
    val techList = technologies.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(element.technologySpacing.dp),
        verticalArrangement = Arrangement.spacedBy((element.technologySpacing / 2).dp)
    ) {
        techList.forEach { tech ->
            val bgColor = element.technologyTagBackgroundColor?.let { Color(it) }
            val borderColor = element.technologyTagBorderColor?.let { Color(it) }

            Box(
                modifier = Modifier
                    .then(
                        if (bgColor != null) {
                            Modifier.background(
                                color = bgColor,
                                shape = RoundedCornerShape(element.technologyTagCornerRadius.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (borderColor != null && element.technologyTagBorderWidth > 0) {
                            Modifier.border(
                                width = element.technologyTagBorderWidth.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(element.technologyTagCornerRadius.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = (8 * zoomLevel).dp, vertical = (4 * zoomLevel).dp)
            ) {
                Text(
                    text = tech,
                    style = element.technologyStyle.toComposeTextStyle(zoomLevel)
                )
            }
        }
    }
}

/**
 * Renders list of project highlights (bullet points)
 */
@Composable
private fun HighlightList(
    highlights: List<ProjectHighlight>,
    element: ResumeElement.ProjectElement,
    zoomLevel: Float = 1f
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(element.highlightSpacing.dp)
    ) {
        highlights.forEachIndexed { index, highlight ->
            if (highlight.text.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((4 * zoomLevel).dp)
                ) {
                    // Bullet character
                    Text(
                        text = getBulletCharacter(element.bulletStyle, highlight, index),
                        style = element.highlightStyle.toComposeTextStyle(zoomLevel)
                    )

                    // Highlight text
                    Text(
                        text = highlight.text,
                        style = element.highlightStyle.toComposeTextStyle(zoomLevel),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Get bullet character based on bullet style
 */
private fun getBulletCharacter(
    bulletStyle: BulletStyle,
    highlight: ProjectHighlight,
    index: Int
): String {
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

/**
 * Format date range for a project
 */
private fun formatDateRange(
    item: ProjectItem,
    element: ResumeElement.ProjectElement
): String {
    val startDate = if (item.startDate.isNotEmpty()) {
        formatDate(item.startDate, element.dateFormat)
    } else ""

    val endDate = when {
        item.isOngoing -> "Present"
        item.endDate.isNotEmpty() -> formatDate(item.endDate, element.dateFormat)
        else -> ""
    }

    return when {
        startDate.isNotEmpty() && endDate.isNotEmpty() -> "$startDate${element.dateSeparator}$endDate"
        startDate.isNotEmpty() -> startDate
        endDate.isNotEmpty() -> endDate
        else -> ""
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
        fontFamily = com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager.poppinsFontFamily,
        color = Color(color),
        lineHeight = lineHeight?.let { (it * zoomLevel).sp } ?: TextUnit.Unspecified,
        letterSpacing = (letterSpacing * zoomLevel).sp,
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None
    )
}
