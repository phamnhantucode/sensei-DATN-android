package com.phamnhantucode.aicareercoach.ui.resumebuilder

import androidx.compose.ui.graphics.Color

/**
 * Pre-defined resume themes.
 */
object ResumeTemplates {

    val Professional = ResumeTheme(
        templateId = "professional",
        colorScheme = ColorScheme(
            primaryColor = 0xFF1976D2, // Blue 700
            accentColor = 0xFF0288D1, // Light Blue 700
            textColor = 0xFF212121, // Grey 900
            backgroundColor = 0xFFFFFFFF, // White
            sectionHeaderColor = 0xFF1976D2, // Blue 700
            secondaryTextColor = 0xFF757575 // Grey 600
        ),
        typography = TypographyScheme(
            fontFamily = "Default",
            headerSize = 24f,
            subHeaderSize = 18f,
            bodySize = 14f,
            captionSize = 12f,
            headerWeight = 700,
            bodyWeight = 400
        ),
        layout = LayoutConfig(
            type = LayoutType.SINGLE_COLUMN,
            spacing = 16,
            sectionSpacing = 24,
            sectionStyle = SectionStyle.DIVIDER
        )
    )

    val Creative = ResumeTheme(
        templateId = "creative",
        colorScheme = ColorScheme(
            primaryColor = 0xFFE91E63, // Pink 500
            accentColor = 0xFFFF4081, // Pink A200
            textColor = 0xFF212121, // Grey 900
            backgroundColor = 0xFFFFFBFE, // Light Pink
            sectionHeaderColor = 0xFFE91E63, // Pink 500
            secondaryTextColor = 0xFF616161 // Grey 700
        ),
        typography = TypographyScheme(
            fontFamily = "Default",
            headerSize = 28f,
            subHeaderSize = 20f,
            bodySize = 14f,
            captionSize = 12f,
            headerWeight = 800,
            bodyWeight = 400
        ),
        layout = LayoutConfig(
            type = LayoutType.TWO_COLUMN,
            spacing = 20,
            sectionSpacing = 28,
            sectionStyle = SectionStyle.CARD
        )
    )

    val Modern = ResumeTheme(
        templateId = "modern",
        colorScheme = ColorScheme(
            primaryColor = 0xFF6200EE, // Purple 500
            accentColor = 0xFF3700B3, // Purple 700
            textColor = 0xFF000000, // Black
            backgroundColor = 0xFFFAFAFA, // Light Grey
            sectionHeaderColor = 0xFF6200EE, // Purple 500
            secondaryTextColor = 0xFF424242 // Grey 800
        ),
        typography = TypographyScheme(
            fontFamily = "Default",
            headerSize = 26f,
            subHeaderSize = 19f,
            bodySize = 14f,
            captionSize = 12f,
            headerWeight = 700,
            bodyWeight = 400
        ),
        layout = LayoutConfig(
            type = LayoutType.MODERN,
            spacing = 18,
            sectionSpacing = 26,
            sectionStyle = SectionStyle.MINIMAL
        )
    )

    val Minimal = ResumeTheme(
        templateId = "minimal",
        colorScheme = ColorScheme(
            primaryColor = 0xFF000000, // Black
            accentColor = 0xFF424242, // Grey 800
            textColor = 0xFF212121, // Grey 900
            backgroundColor = 0xFFFFFFFF, // White
            sectionHeaderColor = 0xFF000000, // Black
            secondaryTextColor = 0xFF9E9E9E // Grey 500
        ),
        typography = TypographyScheme(
            fontFamily = "Default",
            headerSize = 22f,
            subHeaderSize = 17f,
            bodySize = 13f,
            captionSize = 11f,
            headerWeight = 600,
            bodyWeight = 400
        ),
        layout = LayoutConfig(
            type = LayoutType.SINGLE_COLUMN,
            spacing = 14,
            sectionSpacing = 20,
            sectionStyle = SectionStyle.MINIMAL
        )
    )

    val Academic = ResumeTheme(
        templateId = "academic",
        colorScheme = ColorScheme(
            primaryColor = 0xFF0D47A1, // Dark Blue 900
            accentColor = 0xFF1565C0, // Blue 800
            textColor = 0xFF000000, // Black
            backgroundColor = 0xFFFFFFFF, // White
            sectionHeaderColor = 0xFF0D47A1, // Dark Blue 900
            secondaryTextColor = 0xFF546E7A // Blue Grey 600
        ),
        typography = TypographyScheme(
            fontFamily = "Default",
            headerSize = 24f,
            subHeaderSize = 18f,
            bodySize = 13f,
            captionSize = 11f,
            headerWeight = 700,
            bodyWeight = 400
        ),
        layout = LayoutConfig(
            type = LayoutType.SINGLE_COLUMN,
            spacing = 14,
            sectionSpacing = 22,
            sectionStyle = SectionStyle.DIVIDER
        )
    )

    val Tech = ResumeTheme(
        templateId = "tech",
        colorScheme = ColorScheme(
            primaryColor = 0xFF00C853, // Green A700
            accentColor = 0xFF00E676, // Green A400
            textColor = 0xFF263238, // Blue Grey 900
            backgroundColor = 0xFFECEFF1, // Blue Grey 50
            sectionHeaderColor = 0xFF00C853, // Green A700
            secondaryTextColor = 0xFF455A64 // Blue Grey 700
        ),
        typography = TypographyScheme(
            fontFamily = "Default",
            headerSize = 25f,
            subHeaderSize = 19f,
            bodySize = 14f,
            captionSize = 12f,
            headerWeight = 700,
            bodyWeight = 400
        ),
        layout = LayoutConfig(
            type = LayoutType.SIDEBAR,
            spacing = 16,
            sectionSpacing = 24,
            sectionStyle = SectionStyle.BORDERED
        )
    )

    /**
     * Get all available templates.
     */
    fun getAllTemplates(): List<TemplateInfo> = listOf(
        TemplateInfo(
            id = "professional",
            name = "Professional",
            description = "Classic and clean design suitable for corporate environments",
            theme = Professional
        ),
        TemplateInfo(
            id = "creative",
            name = "Creative",
            description = "Bold and vibrant design for creative professionals",
            theme = Creative
        ),
        TemplateInfo(
            id = "modern",
            name = "Modern",
            description = "Contemporary design with a modern aesthetic",
            theme = Modern
        ),
        TemplateInfo(
            id = "minimal",
            name = "Minimal",
            description = "Simple and elegant design focusing on content",
            theme = Minimal
        ),
        TemplateInfo(
            id = "academic",
            name = "Academic",
            description = "Traditional design ideal for academic positions",
            theme = Academic
        ),
        TemplateInfo(
            id = "tech",
            name = "Tech",
            description = "Modern tech-focused design with vibrant accents",
            theme = Tech
        )
    )

    /**
     * Get a template by ID.
     */
    fun getTemplateById(id: String): ResumeTheme? {
        return when (id) {
            "professional" -> Professional
            "creative" -> Creative
            "modern" -> Modern
            "minimal" -> Minimal
            "academic" -> Academic
            "tech" -> Tech
            else -> null
        }
    }
}

/**
 * Template UI model.
 */
data class TemplateInfo(
    val id: String,
    val name: String,
    val description: String,
    val theme: ResumeTheme
)
