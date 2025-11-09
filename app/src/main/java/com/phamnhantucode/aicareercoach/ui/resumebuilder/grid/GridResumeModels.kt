package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.*
import java.util.UUID

/**
 * Grid-based Resume Data Models
 *
 * This file contains the data structures for the grid-based resume editor.
 * The grid uses a 48x64 cell system for precise layout control.
 */

// ============================================================================
// Core Grid Resume Model
// ============================================================================

/**
 * Main grid-based resume data structure
 * @param id Unique identifier
 * @param userId Owner's user ID
 * @param name Resume name/title
 * @param pages List of pages (usually just one for now)
 * @param gridConfig Grid configuration
 * @param globalStyles Global styling defaults
 * @param metadata Resume metadata
 */
data class GridResume(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String = "Untitled Resume",
    val pages: List<ResumePage> = listOf(ResumePage()),
    val gridConfig: GridConfig = GridConfig(),
    val globalStyles: GlobalStyles = GlobalStyles(),
    val metadata: ResumeMetadata = ResumeMetadata()
)

/**
 * A single page in the resume
 * @param id Page identifier
 * @param elements List of elements on this page
 * @param backgroundColor Background color of the page
 */
data class ResumePage(
    val id: String = UUID.randomUUID().toString(),
    val elements: List<ResumeElement> = emptyList(),
    val backgroundColor: Long = 0xFFFFFFFF
)

/**
 * Grid configuration
 * @param columns Number of columns (default: 48)
 * @param rows Number of rows (default: 68 to match A4 aspect ratio √2)
 * @param cellSizeDp Size of each cell in DP
 * @param showGrid Whether to show grid lines
 * @param snapToGrid Whether to snap elements to grid
 * @param snapThreshold Snap threshold as fraction of cell size (0.0-1.0)
 */
data class GridConfig(
    val columns: Int = 48,
    val rows: Int = 68,  // 48 × √2 ≈ 67.9, rounded to 68 for A4 aspect ratio
    val cellSizeDp: Float = 12f,
    val showGrid: Boolean = true,
    val snapToGrid: Boolean = true,
    val snapThreshold: Float = 0.3f // 30% of cell size
)

/**
 * Global styling defaults
 */
data class GlobalStyles(
    val defaultFontSize: Float = 14f,
    val defaultTextColor: Long = 0xFF000000,
    val defaultFontWeight: Int = 400, // FontWeight.Normal
    val accentColor: Long = 0xFF2196F3
)

/**
 * Resume metadata
 */
data class ResumeMetadata(
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val editorType: EditorType = EditorType.GRID
)

enum class EditorType {
    FORM,  // Traditional form-based editor
    GRID   // New grid-based editor
}

/**
 * User information tags for template mode
 * Used to mark elements that should be replaced with user data when applying a template
 */
enum class UserInfoTag {
    NONE,       // No tag - regular element
    NAME,       // Full name
    EMAIL,      // Email address
    PHONE,      // Phone number
    LOCATION,   // Location/address
    GITHUB,     // GitHub URL
    LINKEDIN,   // LinkedIn URL
    WEBSITE,    // Portfolio/website URL
    AVATAR      // Profile picture
}

// ============================================================================
// Resume Element Hierarchy
// ============================================================================

/**
 * Base class for all resume elements
 */
sealed class ResumeElement {
    abstract val id: String
    abstract val position: GridPosition
    abstract val style: ElementStyle
    abstract val zIndex: Int
    abstract val locked: Boolean
    abstract val userInfoTag: UserInfoTag?

    /**
     * Text element - displays formatted text
     */
    data class TextElement(
        override val id: String = UUID.randomUUID().toString(),
        override val position: GridPosition,
        override val style: ElementStyle = ElementStyle(),
        override val zIndex: Int = 0,
        override val locked: Boolean = false,
        override val userInfoTag: UserInfoTag? = null,
        val content: String = "",
        val textStyle: TextStyle = TextStyle(),
        val alignment: TextAlignment = TextAlignment.LEFT,
        val verticalAlignment: VerticalTextAlignment? = VerticalTextAlignment.CENTER,
        val maxLines: Int? = null
    ) : ResumeElement()

    /**
     * Image element - displays an image
     */
    data class ImageElement(
        override val id: String = UUID.randomUUID().toString(),
        override val position: GridPosition,
        override val style: ElementStyle = ElementStyle(),
        override val zIndex: Int = 0,
        override val locked: Boolean = false,
        override val userInfoTag: UserInfoTag? = null,
        val imageUrl: String = "", // Can be file:// or http://
        val contentScale: ImageScale = ImageScale.FIT,
        val cornerRadius: Float = 0f,
        val isCircle: Boolean = false, // If true, crops image to circle
        val description: String = "" // Alt text
    ) : ResumeElement()

    /**
     * Shape element - rectangles, circles, dividers
     */
    data class ShapeElement(
        override val id: String = UUID.randomUUID().toString(),
        override val position: GridPosition,
        override val style: ElementStyle = ElementStyle(),
        override val zIndex: Int = -1, // Behind by default
        override val locked: Boolean = false,
        override val userInfoTag: UserInfoTag? = null,
        val shapeType: ShapeType = ShapeType.RECTANGLE,
        val cornerRadius: Float = 0f,
        val customHeightDp: Float? = null, // Custom height for dividers (overrides rowSpan)
        val customWidthDp: Float? = null  // Custom width if needed
    ) : ResumeElement()

    /**
     * Chart element - skill bars, progress meters
     */
    data class ChartElement(
        override val id: String = UUID.randomUUID().toString(),
        override val position: GridPosition,
        override val style: ElementStyle = ElementStyle(),
        override val zIndex: Int = 0,
        override val locked: Boolean = false,
        override val userInfoTag: UserInfoTag? = null,
        val chartType: ChartType = ChartType.HORIZONTAL_BAR,
        val data: ChartData = ChartData()
    ) : ResumeElement()

    /**
     * Container element - groups other elements
     */
    data class ContainerElement(
        override val id: String = UUID.randomUUID().toString(),
        override val position: GridPosition,
        override val style: ElementStyle = ElementStyle(),
        override val zIndex: Int = 0,
        override val locked: Boolean = false,
        override val userInfoTag: UserInfoTag? = null,
        val children: List<String> = emptyList(), // Child element IDs
        val padding: Padding = Padding(),
        val clipContent: Boolean = false
    ) : ResumeElement()

    /**
     * Icon element - displays an icon/emoji
     */
    data class IconElement(
        override val id: String = UUID.randomUUID().toString(),
        override val position: GridPosition,
        override val style: ElementStyle = ElementStyle(),
        override val zIndex: Int = 0,
        override val locked: Boolean = false,
        override val userInfoTag: UserInfoTag? = null,
        val iconName: String = "", // Material icon name or emoji
        val iconType: IconType = IconType.MATERIAL
    ) : ResumeElement()

    /**
     * Contact element - pre-composed component for contact information
     * Displays a list of contact items (phone, email, address, social links, etc.)
     */
    data class ContactElement(
        override val id: String = UUID.randomUUID().toString(),
        override val position: GridPosition,
        override val style: ElementStyle = ElementStyle(),
        override val zIndex: Int = 0,
        override val locked: Boolean = false,
        override val userInfoTag: UserInfoTag? = null,
        val items: List<ContactItem> = emptyList(),
        val iconStyle: ContactIconStyle = ContactIconStyle.ICON,
        val spacing: Float = 8f, // dp between items
        val orientation: ContactOrientation = ContactOrientation.VERTICAL,
        val textStyle: TextStyle = TextStyle(),
        val iconSize: Float = 16f // Size of icons in dp
    ) : ResumeElement()
}

// ============================================================================
// Position & Layout
// ============================================================================

/**
 * Grid-based position
 * @param row Starting row (0-based)
 * @param col Starting column (0-based)
 * @param rowSpan Number of rows to span
 * @param colSpan Number of columns to span
 */
data class GridPosition(
    val row: Int = 0,
    val col: Int = 0,
    val rowSpan: Int = 1,
    val colSpan: Int = 1
) {
    fun overlaps(other: GridPosition): Boolean {
        val thisEndRow = row + rowSpan
        val thisEndCol = col + colSpan
        val otherEndRow = other.row + other.rowSpan
        val otherEndCol = other.col + other.colSpan

        return !(thisEndRow <= other.row ||
                 row >= otherEndRow ||
                 thisEndCol <= other.col ||
                 col >= otherEndCol)
    }

    fun contains(gridRow: Int, gridCol: Int): Boolean {
        return gridRow >= row && gridRow < row + rowSpan &&
               gridCol >= col && gridCol < col + colSpan
    }
}

data class Padding(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f
)

/**
 * Individual contact item within a ContactElement
 */
data class ContactItem(
    val id: String = UUID.randomUUID().toString(),
    val type: ContactType = ContactType.CUSTOM,
    val value: String = "",
    val label: String = "", // e.g., "Phone:", "Email:"
    val iconName: String = "", // Material icon name or emoji
    val userInfoTag: UserInfoTag? = null // For template mode
)

// ============================================================================
// Styling
// ============================================================================

/**
 * Element styling
 */
data class ElementStyle(
    val backgroundColor: Long? = null, // Nullable for transparent
    val borderColor: Long? = null,
    val borderWidth: Float = 0f,
    val borderRadius: Float = 0f,
    val shadowColor: Long? = null,
    val shadowBlur: Float = 0f,
    val shadowOffsetX: Float = 0f,
    val shadowOffsetY: Float = 0f,
    val opacity: Float = 1f
)

/**
 * Text styling
 */
data class TextStyle(
    val fontSize: Float = 14f,
    val fontWeight: FontWeight = FontWeight.Normal,
    val color: Long = 0xFF000000,
    val lineHeight: Float? = null,
    val letterSpacing: Float = 0f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false
)

// ============================================================================
// Enums
// ============================================================================

enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY
}

enum class VerticalTextAlignment {
    TOP,
    CENTER,
    BOTTOM
}

enum class ImageScale {
    FIT,      // Fit inside bounds
    FILL,     // Fill bounds (may crop)
    STRETCH   // Stretch to fill
}

enum class ShapeType {
    RECTANGLE,
    CIRCLE,
    LINE,
    DIVIDER
}

enum class ChartType {
    HORIZONTAL_BAR,  // Skill bar
    VERTICAL_BAR,    // Bar chart
    CIRCLE_PROGRESS, // Circular progress
    DOT_METER        // Dot-based rating
}

enum class IconType {
    MATERIAL,  // Material Icons
    EMOJI      // Unicode emoji
}

enum class ContactType {
    PHONE,
    EMAIL,
    ADDRESS,
    LINKEDIN,
    GITHUB,
    WEBSITE,
    CUSTOM
}

enum class ContactIconStyle {
    ICON,        // Material icon prefix
    BOLD_LABEL,  // Bold text prefix like "Phone:"
    NONE         // Just the value
}

enum class ContactOrientation {
    VERTICAL,   // Stacked
    HORIZONTAL  // Side by side
}

/**
 * Chart data for skill bars and progress meters
 */
data class ChartData(
    val items: List<ChartItem> = emptyList()
)

data class ChartItem(
    val label: String = "",
    val value: Float = 0f, // 0.0 to 1.0 (percentage)
    val color: Long? = null // Null = use theme color
)

// ============================================================================
// Converters (Form-based Resume <-> Grid-based Resume)
// ============================================================================

/**
 * Converts a traditional form-based Resume to a grid-based GridResume
 * This creates a simple, single-page layout with predefined positions
 */
fun Resume.toGridResume(templateType: GridTemplateType = GridTemplateType.PROFESSIONAL): GridResume {
    val elements = mutableListOf<ResumeElement>()
    var currentRow = 0

    // Helper function to add a text element
    fun addTextElement(
        content: String,
        row: Int,
        col: Int,
        colSpan: Int,
        rowSpan: Int = 1,
        fontSize: Float = 14f,
        fontWeight: FontWeight = FontWeight.Normal,
        color: Long = 0xFF000000
    ): Int {
        if (content.isNotEmpty()) {
            elements.add(
                ResumeElement.TextElement(
                    position = GridPosition(row, col, rowSpan, colSpan),
                    content = content,
                    textStyle = TextStyle(
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = color
                    )
                )
            )
        }
        return row + rowSpan
    }

    // Header/Personal Info (full width)
    currentRow = addTextElement(
        content = personalInfo.fullName,
        row = currentRow,
        col = 0,
        colSpan = 48,
        fontSize = 24f,
        fontWeight = FontWeight.Bold
    )

    currentRow = addTextElement(
        content = "${personalInfo.email} | ${personalInfo.phone} | ${personalInfo.location}",
        row = currentRow,
        col = 0,
        colSpan = 48,
        fontSize = 12f
    )

    // Add divider
    elements.add(
        ResumeElement.ShapeElement(
            position = GridPosition(currentRow, 0, 4, 48),
            shapeType = ShapeType.DIVIDER,
            style = ElementStyle(backgroundColor = 0xFF000000),
            customHeightDp = 2f // Thin divider line
        )
    )
    currentRow += 4

    // Helper to check if section is visible
    fun isSectionVisible(sectionType: ResumeSectionType): Boolean {
        return sectionConfig.any { it.sectionType.id == sectionType.id && it.isVisible }
    }

    // Helper to format dates
    fun formatDates(startDate: java.time.LocalDate?, endDate: java.time.LocalDate?, isCurrent: Boolean = false): String {
        val start = startDate?.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")) ?: ""
        val end = if (isCurrent) "Present" else endDate?.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")) ?: ""
        return if (start.isNotEmpty() && end.isNotEmpty()) "$start - $end" else ""
    }

    // Summary (if visible and not empty)
    if (isSectionVisible(ResumeSectionType.Summary) && professionalSummary.isNotEmpty()) {
        currentRow = addTextElement(
            content = "SUMMARY",
            row = currentRow,
            col = 0,
            colSpan = 48,
            fontSize = 16f,
            fontWeight = FontWeight.Bold
        )
        currentRow = addTextElement(
            content = professionalSummary,
            row = currentRow,
            col = 0,
            colSpan = 48,
            rowSpan = 8
        )
    }

    // Work Experience
    if (isSectionVisible(ResumeSectionType.WorkExperience) && workExperiences.isNotEmpty()) {
        currentRow = addTextElement(
            content = "EXPERIENCE",
            row = currentRow,
            col = 0,
            colSpan = 48,
            fontSize = 16f,
            fontWeight = FontWeight.Bold
        )

        workExperiences.forEach { work ->
            currentRow = addTextElement(
                content = "${work.jobTitle} at ${work.company}",
                row = currentRow,
                col = 0,
                colSpan = 36,
                fontSize = 14f,
                fontWeight = FontWeight.SemiBold
            )
            currentRow = addTextElement(
                content = formatDates(work.startDate, work.endDate, work.isCurrentRole),
                row = currentRow - 1,
                col = 36,
                colSpan = 12,
                fontSize = 12f
            )
            work.responsibilities.forEach { resp ->
                currentRow = addTextElement(
                    content = "• $resp",
                    row = currentRow,
                    col = 0,
                    colSpan = 48
                )
            }
        }
    }

    // Skills (if visible and not empty)
    if (isSectionVisible(ResumeSectionType.Skills) && skills.isNotEmpty()) {
        currentRow = addTextElement(
            content = "SKILLS",
            row = currentRow,
            col = 0,
            colSpan = 48,
            fontSize = 16f,
            fontWeight = FontWeight.Bold
        )

        currentRow = addTextElement(
            content = skills.joinToString(" • "),
            row = currentRow,
            col = 0,
            colSpan = 48,
            rowSpan = 8
        )
    }

    // Education
    if (isSectionVisible(ResumeSectionType.Education) && education.isNotEmpty()) {
        currentRow = addTextElement(
            content = "EDUCATION",
            row = currentRow,
            col = 0,
            colSpan = 48,
            fontSize = 16f,
            fontWeight = FontWeight.Bold
        )

        education.forEach { edu ->
            currentRow = addTextElement(
                content = "${edu.degree} - ${edu.institution}",
                row = currentRow,
                col = 0,
                colSpan = 36,
                fontSize = 14f,
                fontWeight = FontWeight.SemiBold
            )
            currentRow = addTextElement(
                content = formatDates(edu.startDate, edu.endDate),
                row = currentRow - 1,
                col = 36,
                colSpan = 12,
                fontSize = 12f
            )
        }
    }

    return GridResume(
        userId = "", // Will be set by ViewModel
        name = personalInfo.fullName + "'s Resume",
        pages = listOf(
            ResumePage(
                elements = elements
            )
        ),
        metadata = ResumeMetadata(
            editorType = EditorType.GRID
        )
    )
}

/**
 * Converts a grid-based GridResume back to form-based Resume
 * This is a best-effort conversion that may lose some layout information
 */
fun GridResume.toFormResume(): Resume {
    val textElements = pages.firstOrNull()?.elements
        ?.filterIsInstance<ResumeElement.TextElement>()
        ?.sortedWith(compareBy({ it.position.row }, { it.position.col }))
        ?: emptyList()

    // Basic extraction - this is simplified
    // In a real implementation, you'd use pattern matching or metadata

    return Resume(
        personalInfo = PersonalInfo(
            fullName = textElements.firstOrNull()?.content ?: "",
            email = "", // Would need to parse from combined string
            phone = "",
            location = ""
        ),
        professionalSummary = "",
        workExperiences = emptyList(),
        education = emptyList(),
        skills = emptyList(),
        projects = emptyList(),
        certifications = emptyList(),
        languages = emptyList()
    )
}

// ============================================================================
// Template Types
// ============================================================================

enum class GridTemplateType {
    PROFESSIONAL,
    MODERN,
    MINIMAL,
    CREATIVE,
    ACADEMIC,
    TECHNICAL
}

// ============================================================================
// Helper Extensions
// ============================================================================

/**
 * Get all elements sorted by z-index (bottom to top)
 */
fun ResumePage.elementsByZIndex(): List<ResumeElement> {
    return elements.sortedBy { it.zIndex }
}

/**
 * Get element at specific grid position
 */
fun ResumePage.elementAt(row: Int, col: Int): ResumeElement? {
    return elements.firstOrNull { it.position.contains(row, col) }
}

/**
 * Check if position is occupied
 */
fun ResumePage.isOccupied(position: GridPosition, excludeId: String? = null): Boolean {
    return elements.any {
        it.id != excludeId && it.position.overlaps(position)
    }
}

/**
 * Add element to page
 */
fun ResumePage.addElement(element: ResumeElement): ResumePage {
    return copy(elements = elements + element)
}

/**
 * Update element
 */
fun ResumePage.updateElement(elementId: String, update: (ResumeElement) -> ResumeElement): ResumePage {
    return copy(
        elements = elements.map {
            if (it.id == elementId) update(it) else it
        }
    )
}

/**
 * Remove element
 */
fun ResumePage.removeElement(elementId: String): ResumePage {
    return copy(elements = elements.filter { it.id != elementId })
}
