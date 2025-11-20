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
    val cellSizeDp: Float = CELL_SIZE_FOR_A4,  // Calculated to match A4 dimensions exactly
    val showGrid: Boolean = true,
    val snapToGrid: Boolean = true,
    val snapThreshold: Float = 0.3f // 30% of cell size
) {
    companion object {
        /**
         * Optimal cell size calculated to make a 48x68 grid match A4 paper dimensions exactly.
         *
         * A4 at 72 DPI = 595 x 842 points
         * For 68 rows: 842 / 68 = 12.382353 points per row (height-based)
         *
         * Using height-based calculation ensures:
         * - Grid height = 68 * 12.382353 = 842 points (exact match)
         * - Grid width = 48 * 12.382353 = 594.35 points (fits within 595 with minimal centering)
         * - Uniform scale factor = 1.0 (no distortion, perfect 1:1 rendering)
         *
         * This eliminates scaling artifacts and ensures pixel-perfect consistency
         * between the editor canvas and exported PDF.
         */
        const val CELL_SIZE_FOR_A4 = 12.382353f
    }
}

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
    NONE,                 // No tag - regular element
    NAME,                 // Full name
    EMAIL,                // Email address
    PHONE,                // Phone number
    LOCATION,             // Location/address
    GITHUB,               // GitHub URL
    LINKEDIN,             // LinkedIn URL
    WEBSITE,              // Portfolio/website URL
    AVATAR,               // Profile picture
    PROFESSIONAL_SUMMARY, // Professional summary/bio
    WORK_EXPERIENCE,      // Work experience data
    EDUCATION,            // Education data
    SKILLS,               // Skills data
    PROJECTS,             // Projects data
    CERTIFICATIONS,       // Certifications data
    LANGUAGES             // Languages data
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
    abstract val isVisible: Boolean

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
        override val isVisible: Boolean = true,
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
        override val isVisible: Boolean = true,
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
        override val isVisible: Boolean = true,
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
        override val isVisible: Boolean = true,
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
        override val isVisible: Boolean = true,
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
        override val isVisible: Boolean = true,
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
        override val isVisible: Boolean = true,
        val items: List<ContactItem> = emptyList(),
        val iconStyle: ContactIconStyle = ContactIconStyle.ICON,
        val spacing: Float = 8f, // dp between items
        val orientation: ContactOrientation = ContactOrientation.VERTICAL,
        val textStyle: TextStyle = TextStyle(),
        val iconSize: Float = 16f, // Size of icons in dp
        val horizontalAlignment: HorizontalAlignment? = HorizontalAlignment.START,
        val verticalAlignment: VerticalAlignment? = VerticalAlignment.CENTER
    ) : ResumeElement()

    /**
     * Work Experience element - pre-composed component for employment history
     * Displays a list of work experiences with job titles, companies, dates, and responsibilities
     */
    data class WorkExperienceElement(
        override val id: String = UUID.randomUUID().toString(),
        override val position: GridPosition,
        override val style: ElementStyle = ElementStyle(),
        override val zIndex: Int = 0,
        override val locked: Boolean = false,
        override val userInfoTag: UserInfoTag? = null,
        override val isVisible: Boolean = true,
        val items: List<WorkExperienceItem> = emptyList(),
        val displayStyle: WorkExperienceDisplayStyle = WorkExperienceDisplayStyle.STANDARD,
        val orientation: WorkExperienceOrientation = WorkExperienceOrientation.VERTICAL,
        val showLocation: Boolean = true,
        val showDates: Boolean = true,
        val spacing: Float = 16f, // dp between work experience entries
        val itemSpacing: Float = 4f, // dp between fields within an entry
        val responsibilitySpacing: Float = 4f, // dp between responsibility bullets
        val horizontalAlignment: HorizontalAlignment? = HorizontalAlignment.START,
        val verticalAlignment: VerticalAlignment? = VerticalAlignment.TOP,
        val titleStyle: TextStyle = TextStyle(fontSize = 16f, fontWeight = FontWeight.Bold),
        val companyStyle: TextStyle = TextStyle(fontSize = 14f, fontWeight = FontWeight.SemiBold),
        val dateStyle: TextStyle = TextStyle(fontSize = 12f),
        val locationStyle: TextStyle = TextStyle(fontSize = 12f),
        val responsibilityStyle: TextStyle = TextStyle(fontSize = 12f),
        val dateFormat: DateFormat = DateFormat.MMM_YYYY,
        val dateSeparator: String = " - ",
        val bulletStyle: BulletStyle = BulletStyle.DISC
    ) : ResumeElement()

    /**
     * Education element - pre-composed component for educational background
     * Displays a list of education entries with degrees, institutions, dates, and achievements
     */
    data class EducationElement(
        override val id: String = UUID.randomUUID().toString(),
        override val position: GridPosition,
        override val style: ElementStyle = ElementStyle(),
        override val zIndex: Int = 0,
        override val locked: Boolean = false,
        override val userInfoTag: UserInfoTag? = null,
        override val isVisible: Boolean = true,
        val items: List<EducationItem> = emptyList(),
        val displayStyle: EducationDisplayStyle = EducationDisplayStyle.STANDARD,
        val orientation: EducationOrientation = EducationOrientation.VERTICAL,
        val showLocation: Boolean = true,
        val showDates: Boolean = true,
        val showGPA: Boolean = true,
        val spacing: Float = 16f, // dp between education entries
        val itemSpacing: Float = 4f, // dp between fields within an entry
        val achievementSpacing: Float = 4f, // dp between achievement bullets
        val horizontalAlignment: HorizontalAlignment? = HorizontalAlignment.START,
        val verticalAlignment: VerticalAlignment? = VerticalAlignment.TOP,
        val degreeStyle: TextStyle = TextStyle(fontSize = 16f, fontWeight = FontWeight.Bold),
        val institutionStyle: TextStyle = TextStyle(fontSize = 14f, fontWeight = FontWeight.SemiBold),
        val dateStyle: TextStyle = TextStyle(fontSize = 12f),
        val locationStyle: TextStyle = TextStyle(fontSize = 12f),
        val gpaStyle: TextStyle = TextStyle(fontSize = 12f),
        val achievementStyle: TextStyle = TextStyle(fontSize = 12f),
        val dateFormat: DateFormat = DateFormat.MMM_YYYY,
        val dateSeparator: String = " - ",
        val bulletStyle: BulletStyle = BulletStyle.DISC
    ) : ResumeElement()

    /**
     * Skill element - pre-composed component for skills section
     * Displays a list of skills with various visual styles (tags, bars, list, etc.)
     */
    data class SkillElement(
        override val id: String = UUID.randomUUID().toString(),
        override val position: GridPosition,
        override val style: ElementStyle = ElementStyle(),
        override val zIndex: Int = 0,
        override val locked: Boolean = false,
        override val userInfoTag: UserInfoTag? = null,
        override val isVisible: Boolean = true,
        val items: List<SkillItem> = emptyList(),
        val displayStyle: SkillDisplayStyle = SkillDisplayStyle.LIST,
        val spacing: Float = 8f, // dp between skill items
        val groupSpacing: Float = 16f, // dp between skill groups (for GROUPED style)
        val horizontalAlignment: HorizontalAlignment? = HorizontalAlignment.START,
        val verticalAlignment: VerticalAlignment? = VerticalAlignment.TOP,
        val skillStyle: TextStyle = TextStyle(fontSize = 14f),
        val categoryStyle: TextStyle = TextStyle(fontSize = 16f, fontWeight = FontWeight.Bold),
        val proficiencyLabelStyle: TextStyle = TextStyle(fontSize = 10f),
        val showBullets: Boolean = true,
        val bulletStyle: BulletStyle = BulletStyle.DISC,
        val showProficiencyLabel: Boolean = false,
        // Tag style properties
        val tagBackgroundColor: Long? = 0xFFE3F2FD,
        val tagBorderColor: Long? = null,
        val tagBorderWidth: Float = 0f,
        val tagCornerRadius: Float = 16f,
        // Progress bar properties
        val progressBarHeight: Float = 8f,
        val progressBarCornerRadius: Float = 4f,
        val progressBarColor: Long? = 0xFF2196F3,
        val progressBarBackgroundColor: Long? = 0xFFE0E0E0,
        // Dot rating properties
        val maxDots: Int = 5,
        val dotSize: Float = 8f
    ) : ResumeElement()

    /**
     * Project element - pre-composed component for projects section
     * Displays a list of projects with name, description, technologies, highlights, and links
     */
    data class ProjectElement(
        override val id: String = UUID.randomUUID().toString(),
        override val position: GridPosition,
        override val style: ElementStyle = ElementStyle(),
        override val zIndex: Int = 0,
        override val locked: Boolean = false,
        override val userInfoTag: UserInfoTag? = null,
        override val isVisible: Boolean = true,
        val items: List<ProjectItem> = emptyList(),
        val displayStyle: ProjectDisplayStyle = ProjectDisplayStyle.STANDARD,
        val showDates: Boolean = true,
        val showTechnologies: Boolean = true,
        val showLink: Boolean = true,
        val showDescription: Boolean = true,
        val spacing: Float = 16f, // dp between project entries
        val itemSpacing: Float = 4f, // dp between fields within an entry
        val highlightSpacing: Float = 4f, // dp between highlight bullets
        val technologySpacing: Float = 6f, // dp between technology tags
        val horizontalAlignment: HorizontalAlignment? = HorizontalAlignment.START,
        val verticalAlignment: VerticalAlignment? = VerticalAlignment.TOP,
        val nameStyle: TextStyle = TextStyle(fontSize = 16f, fontWeight = FontWeight.Bold),
        val descriptionStyle: TextStyle = TextStyle(fontSize = 12f),
        val dateStyle: TextStyle = TextStyle(fontSize = 12f),
        val technologyStyle: TextStyle = TextStyle(fontSize = 11f),
        val highlightStyle: TextStyle = TextStyle(fontSize = 12f),
        val linkStyle: TextStyle = TextStyle(fontSize = 11f, color = 0xFF2196F3),
        val dateFormat: DateFormat = DateFormat.MMM_YYYY,
        val dateSeparator: String = " - ",
        val bulletStyle: BulletStyle = BulletStyle.DISC,
        // Technology tag styling
        val technologyTagBackgroundColor: Long? = 0xFFE3F2FD,
        val technologyTagBorderColor: Long? = null,
        val technologyTagBorderWidth: Float = 0f,
        val technologyTagCornerRadius: Float = 12f
    ) : ResumeElement()

    /**
     * Certification element - pre-composed component for certifications section
     * Displays a list of certifications with name, issuer, dates, credential ID, and verification link
     */
    data class CertificationElement(
        override val id: String = UUID.randomUUID().toString(),
        override val position: GridPosition,
        override val style: ElementStyle = ElementStyle(),
        override val zIndex: Int = 0,
        override val locked: Boolean = false,
        override val userInfoTag: UserInfoTag? = null,
        override val isVisible: Boolean = true,
        val items: List<CertificationItem> = emptyList(),
        val displayStyle: CertificationDisplayStyle = CertificationDisplayStyle.STANDARD,
        val showIssueDate: Boolean = true,
        val showExpiryDate: Boolean = true,
        val showCredentialId: Boolean = true,
        val showVerificationLink: Boolean = true,
        val showExpiryStatus: Boolean = true,
        val spacing: Float = 16f, // dp between certification entries
        val itemSpacing: Float = 4f, // dp between fields within an entry
        val horizontalAlignment: HorizontalAlignment? = HorizontalAlignment.START,
        val verticalAlignment: VerticalAlignment? = VerticalAlignment.TOP,
        val nameStyle: TextStyle = TextStyle(fontSize = 16f, fontWeight = FontWeight.Bold),
        val issuerStyle: TextStyle = TextStyle(fontSize = 14f, fontWeight = FontWeight.SemiBold),
        val dateStyle: TextStyle = TextStyle(fontSize = 12f),
        val credentialIdStyle: TextStyle = TextStyle(fontSize = 11f),
        val linkStyle: TextStyle = TextStyle(fontSize = 11f, color = 0xFF2196F3),
        val expiryStatusStyle: TextStyle = TextStyle(fontSize = 11f, fontWeight = FontWeight.Medium),
        val dateFormat: DateFormat = DateFormat.MMM_YYYY,
        val activeStatusColor: Long = 0xFF4CAF50, // Green for active
        val expiredStatusColor: Long = 0xFFFF5722 // Red for expired
    ) : ResumeElement()

    /**
     * Language element - pre-composed component for languages section
     * Displays a list of languages with proficiency levels in various visual styles
     */
    data class LanguageElement(
        override val id: String = UUID.randomUUID().toString(),
        override val position: GridPosition,
        override val style: ElementStyle = ElementStyle(),
        override val zIndex: Int = 0,
        override val locked: Boolean = false,
        override val userInfoTag: UserInfoTag? = null,
        override val isVisible: Boolean = true,
        val items: List<LanguageItem> = emptyList(),
        val displayStyle: LanguageDisplayStyle = LanguageDisplayStyle.TEXT_LABELS,
        val proficiencyType: LanguageProficiencyType = LanguageProficiencyType.TEXT,
        val spacing: Float = 8f, // dp between language items
        val horizontalAlignment: HorizontalAlignment? = HorizontalAlignment.START,
        val verticalAlignment: VerticalAlignment? = VerticalAlignment.TOP,
        val languageStyle: TextStyle = TextStyle(fontSize = 14f, fontWeight = FontWeight.Medium),
        val proficiencyLabelStyle: TextStyle = TextStyle(fontSize = 12f),
        // Tag style properties (for TAGS display style)
        val tagBackgroundColor: Long? = 0xFFE3F2FD,
        val tagBorderColor: Long? = null,
        val tagBorderWidth: Float = 0f,
        val tagCornerRadius: Float = 16f,
        // Progress bar properties (for PROGRESS_BARS display style)
        val progressBarHeight: Float = 8f,
        val progressBarCornerRadius: Float = 4f,
        val progressBarColor: Long? = 0xFF2196F3,
        val progressBarBackgroundColor: Long? = 0xFFE0E0E0,
        // Dot rating properties (for DOTS display style)
        val maxDots: Int = 5,
        val dotSize: Float = 8f
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
 * @param widthMode How to calculate width (FIXED or WRAP_CONTENT)
 * @param heightMode How to calculate height (FIXED or WRAP_CONTENT)
 */
data class GridPosition(
    val row: Int = 0,
    val col: Int = 0,
    val rowSpan: Int = 1,
    val colSpan: Int = 1,
    val widthMode: SizeMode = SizeMode.FIXED,
    val heightMode: SizeMode = SizeMode.FIXED
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

/**
 * Individual work experience item within a WorkExperienceElement
 */
data class WorkExperienceItem(
    val id: String = UUID.randomUUID().toString(),
    val jobTitle: String = "",
    val company: String = "",
    val location: String = "",
    val startDate: String = "", // ISO format or formatted string
    val endDate: String = "", // ISO format or formatted string
    val isCurrentRole: Boolean = false,
    val responsibilities: List<ResponsibilityItem> = emptyList(),
    val userInfoTag: UserInfoTag? = null // For template mode
)

/**
 * Individual responsibility/bullet point within a WorkExperienceItem
 */
data class ResponsibilityItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val customBullet: String? = null // Optional custom bullet icon/character
)

/**
 * Individual education item within an EducationElement
 */
data class EducationItem(
    val id: String = UUID.randomUUID().toString(),
    val degree: String = "",
    val institution: String = "",
    val location: String = "",
    val startDate: String = "", // ISO format or formatted string
    val endDate: String = "",
    val gpa: String = "",
    val achievements: List<AchievementItem> = emptyList(),
    val userInfoTag: UserInfoTag? = null // For template mode
)

/**
 * Individual achievement/bullet point within an EducationItem
 */
data class AchievementItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val customBullet: String? = null // Optional custom bullet icon/character
)

/**
 * Individual skill item within a SkillElement
 */
data class SkillItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "", // For grouped display style
    val proficiency: Float? = null, // 0.0 to 1.0 for bar/dot displays
    val proficiencyLabel: String = "", // e.g., "Expert", "Advanced", "Intermediate"
    val userInfoTag: UserInfoTag? = null // For template mode
)

/**
 * Individual project item within a ProjectElement
 */
data class ProjectItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val startDate: String = "", // ISO format or formatted string
    val endDate: String = "",
    val isOngoing: Boolean = false,
    val technologies: String = "", // Comma-separated technologies (e.g., "React, TypeScript, Node.js")
    val link: String = "", // Project URL/GitHub link
    val highlights: List<ProjectHighlight> = emptyList(),
    val userInfoTag: UserInfoTag? = null // For template mode
)

/**
 * Individual highlight/bullet point within a ProjectItem
 */
data class ProjectHighlight(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val customBullet: String? = null // Optional custom bullet icon/character
)

/**
 * Individual certification item within a CertificationElement
 */
data class CertificationItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val issuer: String = "",
    val issueDate: String = "", // ISO format or formatted string
    val expiryDate: String = "", // ISO format or formatted string (empty if no expiry)
    val credentialId: String = "",
    val verificationLink: String = "",
    val userInfoTag: UserInfoTag? = null // For template mode
) {
    /**
     * Check if certification is expired
     */
    val isExpired: Boolean
        get() {
            if (expiryDate.isEmpty()) return false
            return try {
                val expiry = java.time.LocalDate.parse(expiryDate)
                expiry.isBefore(java.time.LocalDate.now())
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Check if certification is active (not expired)
     */
    val isActive: Boolean
        get() = !isExpired || expiryDate.isEmpty()
}

/**
 * Individual language item within a LanguageElement
 */
data class LanguageItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "", // Language name (e.g., "English", "Spanish")
    val proficiency: Float = 0.5f, // 0.0 to 1.0 for visual displays
    val proficiencyLabel: String = "", // e.g., "Native", "Fluent", "Intermediate"
    val cefrLevel: String? = null, // e.g., "C2", "B2", "A1" (Common European Framework of Reference)
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

enum class HorizontalAlignment {
    START,   // Left alignment
    CENTER,  // Center alignment
    END      // Right alignment
}

enum class VerticalAlignment {
    TOP,     // Top alignment
    CENTER,  // Center alignment
    BOTTOM   // Bottom alignment
}

enum class WorkExperienceDisplayStyle {
    STANDARD,  // Traditional layout: Title | Company on separate lines
    COMPACT,   // Condensed: Title + Company on same line
    DETAILED   // Expanded with all fields prominently displayed
}

enum class WorkExperienceOrientation {
    VERTICAL,   // Stack entries vertically
    HORIZONTAL  // Arrange entries in columns (side by side)
}

enum class EducationDisplayStyle {
    STANDARD,  // Traditional layout: Degree | Institution on separate lines
    COMPACT,   // Condensed: Degree + Institution on same line
    DETAILED   // Expanded with all fields prominently displayed
}

enum class EducationOrientation {
    VERTICAL,   // Stack entries vertically
    HORIZONTAL  // Arrange entries in columns (side by side)
}

enum class DateFormat {
    MMM_YYYY,   // Jan 2020
    MM_YYYY,    // 01/2020
    FULL,       // January 2020
    SHORT,      // 1/20
    YYYY        // 2020
}

enum class BulletStyle {
    DISC,         // •
    DASH,         // -
    ARROW,        // →
    CHEVRON,      // ›
    NUMBERED,     // 1. 2. 3.
    CUSTOM_ICON,  // Use custom icon from responsibilityItem
    NONE          // No bullet
}

enum class SkillDisplayStyle {
    LIST,           // Simple bulleted list
    TAGS,           // Rounded tag/chip style
    PROGRESS_BARS,  // Skills with proficiency bars
    DOTS,           // Skills with dot-based proficiency rating
    GROUPED         // Skills organized by category
}

enum class ProjectDisplayStyle {
    STANDARD,  // Traditional layout: Name on top, description below
    COMPACT,   // Condensed: Minimal spacing, shorter descriptions
    DETAILED   // Expanded with all fields prominently displayed
}

enum class CertificationDisplayStyle {
    STANDARD,  // Traditional layout: Name | Issuer on separate lines
    COMPACT,   // Condensed: Name + Issuer on same line
    DETAILED   // Expanded with all fields prominently displayed
}

enum class LanguageDisplayStyle {
    TEXT_LABELS,   // Simple text layout (Language - Proficiency)
    PROGRESS_BARS, // Visual bars showing proficiency
    DOTS,          // Dot indicators (like Skills)
    TAGS           // Chip-style tags with proficiency
}

enum class LanguageProficiencyType {
    TEXT,    // Text labels (Native, Fluent, etc.)
    CEFR,    // Common European Framework (A1-C2)
    NUMERIC  // Numeric scale (percentage or 0.0-1.0)
}

enum class SizeMode {
    FIXED,         // Use rowSpan/colSpan for size
    WRAP_CONTENT   // Auto-size to fit content
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
 *
 * @param existingResumeId The existing resume ID to preserve (if updating existing resume)
 *                         If null, a new ID will be generated
 */
fun GridResume.toFormResume(existingResumeId: String? = null): Resume {
    val page = pages.firstOrNull()

    // Extract data from tagged elements instead of guessing
    val textElements = page?.elements?.filterIsInstance<ResumeElement.TextElement>() ?: emptyList()
    val contactElements = page?.elements?.filterIsInstance<ResumeElement.ContactElement>() ?: emptyList()
    val workExperienceElements = page?.elements?.filterIsInstance<ResumeElement.WorkExperienceElement>() ?: emptyList()
    val educationElements = page?.elements?.filterIsInstance<ResumeElement.EducationElement>() ?: emptyList()
    val skillElements = page?.elements?.filterIsInstance<ResumeElement.SkillElement>() ?: emptyList()
    val projectElements = page?.elements?.filterIsInstance<ResumeElement.ProjectElement>() ?: emptyList()
    val certificationElements = page?.elements?.filterIsInstance<ResumeElement.CertificationElement>() ?: emptyList()
    val languageElements = page?.elements?.filterIsInstance<ResumeElement.LanguageElement>() ?: emptyList()
    val imageElements = page?.elements?.filterIsInstance<ResumeElement.ImageElement>() ?: emptyList()

    // Extract personal info from tagged elements
    val nameText = textElements.find { it.userInfoTag == UserInfoTag.NAME }?.content ?: ""
    val emailFromContact = contactElements.flatMap { it.items }.find { it.userInfoTag == UserInfoTag.EMAIL }?.value ?: ""
    val phoneFromContact = contactElements.flatMap { it.items }.find { it.userInfoTag == UserInfoTag.PHONE }?.value ?: ""
    val locationFromContact = contactElements.flatMap { it.items }.find { it.userInfoTag == UserInfoTag.LOCATION }?.value ?: ""
    val linkedInFromContact = contactElements.flatMap { it.items }.find { it.userInfoTag == UserInfoTag.LINKEDIN }?.value ?: ""
    val githubFromContact = contactElements.flatMap { it.items }.find { it.userInfoTag == UserInfoTag.GITHUB }?.value ?: ""
    val websiteFromContact = contactElements.flatMap { it.items }.find { it.userInfoTag == UserInfoTag.WEBSITE }?.value ?: ""
    val avatarImage = imageElements.find { it.userInfoTag == UserInfoTag.AVATAR }?.imageUrl ?: ""

    // Extract work experiences from WorkExperienceElement
    val workExperiences = workExperienceElements
        .flatMap { element -> element.items }
        .map { item ->
            com.phamnhantucode.aicareercoach.ui.resumebuilder.WorkExperience(
                jobTitle = item.jobTitle,
                company = item.company,
                location = item.location,
                startDate = parseDate(item.startDate),
                endDate = if (item.isCurrentRole) null else parseDate(item.endDate),
                isCurrentRole = item.isCurrentRole,
                responsibilities = item.responsibilities.map { it.text }
            )
        }

    // Extract education from EducationElement
    val education = educationElements
        .flatMap { element -> element.items }
        .map { item ->
            com.phamnhantucode.aicareercoach.ui.resumebuilder.Education(
                degree = item.degree,
                institution = item.institution,
                location = item.location,
                startDate = parseDate(item.startDate),
                endDate = parseDate(item.endDate),
                gpa = item.gpa,
                achievements = item.achievements.map { it.text }
            )
        }

    // Extract skills from SkillElement
    val skills = skillElements.flatMap { element -> element.items.map { it.name } }

    // Extract projects from ProjectElement
    val projects = projectElements
        .flatMap { element -> element.items }
        .map { item ->
            com.phamnhantucode.aicareercoach.ui.resumebuilder.Project(
                title = item.name,
                description = item.description,
                technologies = item.technologies.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                link = item.link,
                startDate = parseDate(item.startDate),
                endDate = parseDate(item.endDate)
            )
        }

    // Extract certifications from CertificationElement
    val certifications = certificationElements
        .flatMap { element -> element.items }
        .map { item ->
            com.phamnhantucode.aicareercoach.ui.resumebuilder.Certification(
                name = item.name,
                issuer = item.issuer,
                issueDate = parseDate(item.issueDate),
                expiryDate = parseDate(item.expiryDate),
                credentialId = item.credentialId
            )
        }

    // Extract languages from LanguageElement
    val languages = languageElements
        .flatMap { element -> element.items }
        .map { item ->
            com.phamnhantucode.aicareercoach.ui.resumebuilder.Language(
                name = item.name,
                proficiency = proficiencyFloatToEnum(item.proficiency)
            )
        }

    return Resume(
        id = existingResumeId ?: java.util.UUID.randomUUID().toString(), // CRITICAL: Use existing ID if provided!
        personalInfo = PersonalInfo(
            fullName = nameText,
            email = emailFromContact,
            phone = phoneFromContact,
            location = locationFromContact,
            linkedIn = linkedInFromContact,
            portfolio = websiteFromContact,
            github = githubFromContact,
            avatar = avatarImage
        ),
        professionalSummary = "", // Grid editor doesn't have a specific field for this yet
        workExperiences = workExperiences,
        education = education,
        skills = skills,
        projects = projects,
        certifications = certifications,
        languages = languages
    )
}

/**
 * Helper function to parse date strings (MMM yyyy format)
 */
private fun parseDate(dateString: String): java.time.LocalDate? {
    if (dateString.isBlank() || dateString == "Present") return null
    return try {
        java.time.LocalDate.parse(
            "01 $dateString",
            java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * Helper function to convert proficiency float to enum
 */
private fun proficiencyFloatToEnum(proficiency: Float): com.phamnhantucode.aicareercoach.ui.resumebuilder.LanguageProficiency {
    return when {
        proficiency >= 0.95f -> com.phamnhantucode.aicareercoach.ui.resumebuilder.LanguageProficiency.NATIVE
        proficiency >= 0.8f -> com.phamnhantucode.aicareercoach.ui.resumebuilder.LanguageProficiency.FLUENT
        proficiency >= 0.65f -> com.phamnhantucode.aicareercoach.ui.resumebuilder.LanguageProficiency.PROFICIENT
        proficiency >= 0.4f -> com.phamnhantucode.aicareercoach.ui.resumebuilder.LanguageProficiency.INTERMEDIATE
        else -> com.phamnhantucode.aicareercoach.ui.resumebuilder.LanguageProficiency.ELEMENTARY
    }
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
// AI Improvement Models
// ============================================================================

/**
 * Options for AI-powered responsibility improvement
 * @param makeProfessional Enhance language to be more formal and business-appropriate
 * @param addMetrics Suggest quantifiable achievements (e.g., "increased by 20%")
 * @param makeConcise Shorten the text while keeping key points
 * @param makeDetailed Expand with more context and specific actions
 */
data class ResponsibilityImprovementOptions(
    val makeProfessional: Boolean = false,
    val addMetrics: Boolean = false,
    val makeConcise: Boolean = false,
    val makeDetailed: Boolean = false,
    val formatAsBullets: Boolean = true
) {
    fun hasAnySelected(): Boolean = makeProfessional || addMetrics || makeConcise || makeDetailed

    fun toPromptInstructions(): List<String> {
        val instructions = mutableListOf<String>()
        if (makeProfessional) instructions.add("Use professional and business-appropriate language with strong action verbs")
        if (addMetrics) instructions.add("Include specific metrics, numbers, or quantifiable achievements where possible")
        if (makeConcise) instructions.add("Make it concise and impactful (1-2 lines maximum)")
        if (makeDetailed) instructions.add("Expand with more context, specific actions, and detailed accomplishments")
        return instructions
    }

    fun getBulletFormatInstruction(): String {
        return if (formatAsBullets) {
            "Format the output as bullet points using the '•' character at the start of each line"
        } else {
            "Format the output as plain text without bullet points or special characters at the start"
        }
    }
}

/**
 * Result of AI improvement containing multiple suggestions
 * @param suggestions List of 3 improved responsibility variations
 * @param originalText The original responsibility text
 */
data class ResponsibilityImprovementResult(
    val suggestions: List<String>,
    val originalText: String
)

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
