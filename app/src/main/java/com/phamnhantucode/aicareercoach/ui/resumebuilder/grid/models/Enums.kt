package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models

/**
 * Editor type enum
 */
enum class EditorType {
    FORM,  // Traditional form-based editor
    GRID   // New grid-based editor
}

/**
 * Element type enum for adding new elements
 */
enum class ElementType {
    TEXT,
    IMAGE,
    SHAPE,
    DIVIDER,
    CHART,
    ICON,
    CONTAINER,
    CONTACT,
    WORK_EXPERIENCE,
    EDUCATION,
    SKILL,
    PROJECT,
    CERTIFICATION,
    LANGUAGE
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

enum class GridTemplateType {
    PROFESSIONAL,
    MODERN,
    MINIMAL,
    CREATIVE,
    ACADEMIC,
    TECHNICAL
}

/**
 * Layout mode for container elements
 * Determines how child elements are positioned and whether they can overlap
 */
enum class LayoutMode {
    FREE,  // Elements can freely overlap, no collision detection within container
    GRID   // Elements snap to grid and cannot overlap (default behavior)
}
