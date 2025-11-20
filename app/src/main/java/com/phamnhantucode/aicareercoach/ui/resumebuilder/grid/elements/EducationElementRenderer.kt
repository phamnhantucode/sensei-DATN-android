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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
// TextStyle conflict resolved - using fully qualified names
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Renders an education element on the resume
 * Displays a list of education entries with degrees, institutions, dates, and achievements
 */
@Composable
fun EducationElementRenderer(
    element: ResumeElement.EducationElement,
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
            EducationOrientation.VERTICAL -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = verticalArrangementForColumn,
                    horizontalAlignment = horizontalAlignmentForColumn
                ) {
                    element.items.forEachIndexed { index, item ->
                        EducationItemRenderer(
                            item = item,
                            element = element,
                            zoomLevel = zoomLevel
                        )

                        // Add spacing between items (but not after last item)
                        if (index < element.items.size - 1) {
                            Spacer(modifier = Modifier.height((element.spacing * zoomLevel).dp))
                        }
                    }
                }
            }
            EducationOrientation.HORIZONTAL -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = horizontalArrangement,
                    verticalAlignment = Alignment.Top
                ) {
                    element.items.forEachIndexed { index, item ->
                        // Each item gets equal weight
                        Box(modifier = Modifier.weight(1f)) {
                            EducationItemRenderer(
                                item = item,
                                element = element,
                                zoomLevel = zoomLevel
                            )
                        }

                        // Add spacing between items (but not after last item)
                        if (index < element.items.size - 1) {
                            Spacer(modifier = Modifier.width((element.spacing * zoomLevel).dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a single education item based on display style
 */
@Composable
private fun EducationItemRenderer(
    item: EducationItem,
    element: ResumeElement.EducationElement,
    zoomLevel: Float = 1f
) {
    when (element.displayStyle) {
        EducationDisplayStyle.STANDARD -> {
            StandardEducationLayout(item, element, zoomLevel)
        }
        EducationDisplayStyle.COMPACT -> {
            CompactEducationLayout(item, element, zoomLevel)
        }
        EducationDisplayStyle.DETAILED -> {
            DetailedEducationLayout(item, element, zoomLevel)
        }
    }
}

/**
 * Standard layout: Degree and institution on separate lines
 */
@Composable
private fun StandardEducationLayout(
    item: EducationItem,
    element: ResumeElement.EducationElement,
    zoomLevel: Float = 1f
) {
    Column(
        verticalArrangement = Arrangement.spacedBy((element.itemSpacing * zoomLevel).dp)
    ) {
        // Degree
        if (item.degree.isNotEmpty()) {
            Text(
                text = item.degree,
                style = element.degreeStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Institution and Date Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.institution.isNotEmpty()) {
                Text(
                    text = item.institution,
                    style = element.institutionStyle.toComposeTextStyle(zoomLevel),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            if (element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty())) {
                Text(
                    text = formatDateRange(item, element),
                    style = element.dateStyle.toComposeTextStyle(zoomLevel)
                )
            }
        }

        // Location
        if (element.showLocation && item.location.isNotEmpty()) {
            Text(
                text = item.location,
                style = element.locationStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // GPA
        if (element.showGPA && item.gpa.isNotEmpty()) {
            Text(
                text = "GPA: ${item.gpa}",
                style = element.gpaStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Achievements
        if (item.achievements.isNotEmpty()) {
            AchievementList(
                achievements = item.achievements,
                element = element,
                zoomLevel = zoomLevel
            )
        }
    }
}

/**
 * Compact layout: Degree and institution on same line
 */
@Composable
private fun CompactEducationLayout(
    item: EducationItem,
    element: ResumeElement.EducationElement,
    zoomLevel: Float = 1f
) {
    Column(
        verticalArrangement = Arrangement.spacedBy((element.itemSpacing * zoomLevel).dp)
    ) {
        // Degree + Institution Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Degree and Institution together
            if (item.degree.isNotEmpty() || item.institution.isNotEmpty()) {
                val degreeInstitution = buildString {
                    if (item.degree.isNotEmpty()) append(item.degree)
                    if (item.degree.isNotEmpty() && item.institution.isNotEmpty()) append(", ")
                    if (item.institution.isNotEmpty()) append(item.institution)
                }

                Text(
                    text = degreeInstitution,
                    style = element.degreeStyle.toComposeTextStyle(zoomLevel),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Dates
            if (element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty())) {
                Text(
                    text = formatDateRange(item, element),
                    style = element.dateStyle.toComposeTextStyle(zoomLevel)
                )
            }
        }

        // Location and GPA Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (element.showLocation && item.location.isNotEmpty()) {
                Text(
                    text = item.location,
                    style = element.locationStyle.toComposeTextStyle(zoomLevel),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            if (element.showGPA && item.gpa.isNotEmpty()) {
                Text(
                    text = "GPA: ${item.gpa}",
                    style = element.gpaStyle.toComposeTextStyle(zoomLevel)
                )
            }
        }

        // Achievements
        if (item.achievements.isNotEmpty()) {
            AchievementList(
                achievements = item.achievements,
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
private fun DetailedEducationLayout(
    item: EducationItem,
    element: ResumeElement.EducationElement,
    zoomLevel: Float = 1f
) {
    Column(
        verticalArrangement = Arrangement.spacedBy((element.itemSpacing * zoomLevel).dp)
    ) {
        // Degree
        if (item.degree.isNotEmpty()) {
            Text(
                text = item.degree,
                style = element.degreeStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Institution
        if (item.institution.isNotEmpty()) {
            Text(
                text = item.institution,
                style = element.institutionStyle.toComposeTextStyle(zoomLevel)
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
                    style = element.locationStyle.toComposeTextStyle(zoomLevel),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            if (element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty())) {
                Text(
                    text = formatDateRange(item, element),
                    style = element.dateStyle.toComposeTextStyle(zoomLevel)
                )
            }
        }

        // GPA
        if (element.showGPA && item.gpa.isNotEmpty()) {
            Text(
                text = "GPA: ${item.gpa}",
                style = element.gpaStyle.toComposeTextStyle(zoomLevel)
            )
        }

        // Achievements
        if (item.achievements.isNotEmpty()) {
            AchievementList(
                achievements = item.achievements,
                element = element,
                zoomLevel = zoomLevel
            )
        }
    }
}

/**
 * Renders list of achievements with bullets
 */
@Composable
private fun AchievementList(
    achievements: List<AchievementItem>,
    element: ResumeElement.EducationElement,
    zoomLevel: Float = 1f
) {
    Column(
        verticalArrangement = Arrangement.spacedBy((element.achievementSpacing * zoomLevel).dp)
    ) {
        achievements.forEachIndexed { index, achievement ->
            if (achievement.text.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((4 * zoomLevel).dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Bullet
                    Text(
                        text = getBulletCharacter(element.bulletStyle, index, achievement),
                        style = element.achievementStyle.toComposeTextStyle(zoomLevel)
                    )

                    // Achievement text
                    Text(
                        text = achievement.text,
                        style = element.achievementStyle.toComposeTextStyle(zoomLevel),
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
    achievement: AchievementItem
): String {
    return when (bulletStyle) {
        BulletStyle.DISC -> "•"
        BulletStyle.DASH -> "-"
        BulletStyle.ARROW -> "→"
        BulletStyle.CHEVRON -> "›"
        BulletStyle.NUMBERED -> "${index + 1}."
        BulletStyle.CUSTOM_ICON -> achievement.customBullet ?: "•"
        BulletStyle.NONE -> ""
    }
}

/**
 * Format date range for display
 */
private fun formatDateRange(
    item: EducationItem,
    element: ResumeElement.EducationElement
): String {
    val start = formatDate(item.startDate, element.dateFormat)
    val end = formatDate(item.endDate, element.dateFormat)

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
private fun com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.TextStyle.toComposeTextStyle(zoomLevel: Float = 1f): androidx.compose.ui.text.TextStyle {
    return androidx.compose.ui.text.TextStyle(
        fontSize = (fontSize * zoomLevel).sp,
        fontWeight = fontWeight,
        fontFamily = FontManager.poppinsFontFamily,
        color = Color(color),
        lineHeight = lineHeight?.let { (it * zoomLevel).sp } ?: androidx.compose.ui.unit.TextUnit.Unspecified,
        letterSpacing = (letterSpacing * zoomLevel).sp,
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None
    )
}
