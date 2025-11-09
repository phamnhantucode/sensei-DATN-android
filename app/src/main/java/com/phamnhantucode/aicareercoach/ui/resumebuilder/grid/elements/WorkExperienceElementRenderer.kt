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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Renders a work experience element on the resume
 * Displays a list of work experiences with job titles, companies, dates, and responsibilities
 */
@Composable
fun WorkExperienceElementRenderer(
    element: ResumeElement.WorkExperienceElement,
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

        val verticalArrangementForColumn = when (element.verticalAlignment ?: VerticalAlignment.TOP) {
            VerticalAlignment.TOP -> Arrangement.Top
            VerticalAlignment.CENTER -> Arrangement.Center
            VerticalAlignment.BOTTOM -> Arrangement.Bottom
        }

        val horizontalAlignmentForColumn = when (element.horizontalAlignment ?: HorizontalAlignment.START) {
            HorizontalAlignment.START -> Alignment.Start
            HorizontalAlignment.CENTER -> Alignment.CenterHorizontally
            HorizontalAlignment.END -> Alignment.End
        }

        when (element.orientation) {
            WorkExperienceOrientation.VERTICAL -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = verticalArrangementForColumn,
                    horizontalAlignment = horizontalAlignmentForColumn
                ) {
                    element.items.forEachIndexed { index, item ->
                        WorkExperienceItemRenderer(
                            item = item,
                            element = element
                        )

                        // Add spacing between items (but not after last item)
                        if (index < element.items.size - 1) {
                            Spacer(modifier = Modifier.height(element.spacing.dp))
                        }
                    }
                }
            }
            WorkExperienceOrientation.HORIZONTAL -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = horizontalArrangement,
                    verticalAlignment = Alignment.Top
                ) {
                    element.items.forEachIndexed { index, item ->
                        // Each item gets equal weight
                        Box(modifier = Modifier.weight(1f)) {
                            WorkExperienceItemRenderer(
                                item = item,
                                element = element
                            )
                        }

                        // Add spacing between items (but not after last item)
                        if (index < element.items.size - 1) {
                            Spacer(modifier = Modifier.width(element.spacing.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a single work experience item based on display style
 */
@Composable
private fun WorkExperienceItemRenderer(
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement
) {
    when (element.displayStyle) {
        WorkExperienceDisplayStyle.STANDARD -> {
            StandardWorkExperienceLayout(item, element)
        }
        WorkExperienceDisplayStyle.COMPACT -> {
            CompactWorkExperienceLayout(item, element)
        }
        WorkExperienceDisplayStyle.DETAILED -> {
            DetailedWorkExperienceLayout(item, element)
        }
    }
}

/**
 * Standard layout: Title and company on separate lines
 */
@Composable
private fun StandardWorkExperienceLayout(
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(element.itemSpacing.dp)
    ) {
        // Job Title
        if (item.jobTitle.isNotEmpty()) {
            Text(
                text = item.jobTitle,
                style = element.titleStyle.toComposeTextStyle()
            )
        }

        // Company and Date Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.company.isNotEmpty()) {
                Text(
                    text = item.company,
                    style = element.companyStyle.toComposeTextStyle(),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            if (element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isCurrentRole)) {
                Text(
                    text = formatDateRange(item, element),
                    style = element.dateStyle.toComposeTextStyle()
                )
            }
        }

        // Location
        if (element.showLocation && item.location.isNotEmpty()) {
            Text(
                text = item.location,
                style = element.locationStyle.toComposeTextStyle()
            )
        }

        // Responsibilities
        if (item.responsibilities.isNotEmpty()) {
            ResponsibilityList(
                responsibilities = item.responsibilities,
                element = element
            )
        }
    }
}

/**
 * Compact layout: Title and company on same line
 */
@Composable
private fun CompactWorkExperienceLayout(
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(element.itemSpacing.dp)
    ) {
        // Title + Company Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title and Company together
            if (item.jobTitle.isNotEmpty() || item.company.isNotEmpty()) {
                val titleCompany = buildString {
                    if (item.jobTitle.isNotEmpty()) append(item.jobTitle)
                    if (item.jobTitle.isNotEmpty() && item.company.isNotEmpty()) append(" at ")
                    if (item.company.isNotEmpty()) append(item.company)
                }

                Text(
                    text = titleCompany,
                    style = element.titleStyle.toComposeTextStyle(),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Dates
            if (element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isCurrentRole)) {
                Text(
                    text = formatDateRange(item, element),
                    style = element.dateStyle.toComposeTextStyle()
                )
            }
        }

        // Location
        if (element.showLocation && item.location.isNotEmpty()) {
            Text(
                text = item.location,
                style = element.locationStyle.toComposeTextStyle()
            )
        }

        // Responsibilities
        if (item.responsibilities.isNotEmpty()) {
            ResponsibilityList(
                responsibilities = item.responsibilities,
                element = element
            )
        }
    }
}

/**
 * Detailed layout: All fields prominently displayed
 */
@Composable
private fun DetailedWorkExperienceLayout(
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(element.itemSpacing.dp)
    ) {
        // Job Title
        if (item.jobTitle.isNotEmpty()) {
            Text(
                text = item.jobTitle,
                style = element.titleStyle.toComposeTextStyle()
            )
        }

        // Company
        if (item.company.isNotEmpty()) {
            Text(
                text = item.company,
                style = element.companyStyle.toComposeTextStyle()
            )
        }

        // Location and Dates Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (element.showLocation && item.location.isNotEmpty()) {
                Text(
                    text = item.location,
                    style = element.locationStyle.toComposeTextStyle(),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            if (element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.isCurrentRole)) {
                Text(
                    text = formatDateRange(item, element),
                    style = element.dateStyle.toComposeTextStyle()
                )
            }
        }

        // Responsibilities
        if (item.responsibilities.isNotEmpty()) {
            ResponsibilityList(
                responsibilities = item.responsibilities,
                element = element
            )
        }
    }
}

/**
 * Renders list of responsibilities with bullets
 */
@Composable
private fun ResponsibilityList(
    responsibilities: List<ResponsibilityItem>,
    element: ResumeElement.WorkExperienceElement
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(element.responsibilitySpacing.dp)
    ) {
        responsibilities.forEachIndexed { index, responsibility ->
            if (responsibility.text.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Bullet
                    Text(
                        text = getBulletCharacter(element.bulletStyle, index, responsibility),
                        style = element.responsibilityStyle.toComposeTextStyle()
                    )

                    // Responsibility text
                    Text(
                        text = responsibility.text,
                        style = element.responsibilityStyle.toComposeTextStyle(),
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
    index: Int,
    responsibility: ResponsibilityItem
): String {
    return when (bulletStyle) {
        BulletStyle.DISC -> "•"
        BulletStyle.DASH -> "-"
        BulletStyle.ARROW -> "→"
        BulletStyle.CHEVRON -> "›"
        BulletStyle.NUMBERED -> "${index + 1}."
        BulletStyle.CUSTOM_ICON -> responsibility.customBullet ?: "•"
        BulletStyle.NONE -> ""
    }
}

/**
 * Format date range for display
 */
private fun formatDateRange(
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement
): String {
    val start = formatDate(item.startDate, element.dateFormat)
    val end = if (item.isCurrentRole) "Present" else formatDate(item.endDate, element.dateFormat)

    return when {
        start.isNotEmpty() && end.isNotEmpty() -> "$start${element.dateSeparator}$end"
        start.isNotEmpty() -> start
        end.isNotEmpty() -> end
        else -> ""
    }
}

/**
 * Format a single date string based on date format
 */
private fun formatDate(dateString: String, dateFormat: DateFormat): String {
    if (dateString.isEmpty()) return ""

    return try {
        // Try to parse as LocalDate (ISO format: yyyy-MM-dd)
        val date = LocalDate.parse(dateString)

        when (dateFormat) {
            DateFormat.MMM_YYYY -> date.format(DateTimeFormatter.ofPattern("MMM yyyy"))
            DateFormat.MM_YYYY -> date.format(DateTimeFormatter.ofPattern("MM/yyyy"))
            DateFormat.FULL -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            DateFormat.SHORT -> date.format(DateTimeFormatter.ofPattern("M/yy"))
            DateFormat.YYYY -> date.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    } catch (e: Exception) {
        // If parsing fails, return the original string (might be pre-formatted)
        dateString
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
