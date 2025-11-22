package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models

import androidx.compose.ui.text.font.FontWeight
import java.util.UUID

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
        val maxLines: Int? = null,
        val padding: Padding? = Padding(8f, 8f, 8f, 8f)
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
        val description: String = "", // Alt text
        val padding: Padding? = Padding(0f, 0f, 0f, 0f)
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
        val customWidthDp: Float? = null,  // Custom width if needed
        val padding: Padding? = Padding(0f, 0f, 0f, 0f)
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
        val data: ChartData = ChartData(),
        val padding: Padding? = Padding(8f, 8f, 8f, 8f)
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
        val clipContent: Boolean = false,
        val layoutMode: LayoutMode? = LayoutMode.GRID // How children are positioned
    ) : ResumeElement() {
        val effectiveLayoutMode: LayoutMode
            get() = layoutMode ?: LayoutMode.GRID
    }

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
        val iconType: IconType = IconType.MATERIAL,
        val padding: Padding? = Padding(8f, 8f, 8f, 8f)
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
        val verticalAlignment: VerticalAlignment? = VerticalAlignment.CENTER,
        val padding: Padding? = Padding(8f, 8f, 8f, 8f)
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
        val bulletStyle: BulletStyle = BulletStyle.DISC,
        val padding: Padding? = Padding(8f, 8f, 8f, 8f)
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
        val bulletStyle: BulletStyle = BulletStyle.DISC,
        val padding: Padding? = Padding(8f, 8f, 8f, 8f)
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
        val dotSize: Float = 8f,
        val padding: Padding? = Padding(8f, 8f, 8f, 8f)
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
        val technologyTagCornerRadius: Float = 12f,
        val padding: Padding? = Padding(8f, 8f, 8f, 8f)
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
        val expiredStatusColor: Long = 0xFFFF5722, // Red for expired
        val padding: Padding? = Padding(8f, 8f, 8f, 8f)
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
        val dotSize: Float = 8f,
        val padding: Padding? = Padding(8f, 8f, 8f, 8f)
    ) : ResumeElement()
}

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
