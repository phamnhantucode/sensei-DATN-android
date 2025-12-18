package com.phamnhantucode.aicareercoach.ui.resumebuilder

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

data class Resume(
    val id: String = java.util.UUID.randomUUID().toString(),
    val personalInfo: PersonalInfo = PersonalInfo(),
    val professionalSummary: String = "",
    val workExperiences: List<WorkExperience> = emptyList(),
    val education: List<Education> = emptyList(),
    val skills: List<String> = emptyList(),
    val projects: List<Project> = emptyList(),
    val certifications: List<Certification> = emptyList(),
    val languages: List<Language> = emptyList(),
    val theme: ResumeTheme = ResumeTheme(),
    val sectionConfig: List<SectionConfig> = getDefaultSectionConfig()
)

data class PersonalInfo(
    val fullName: String = "",
    val profession: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val linkedIn: String = "",
    val portfolio: String = "",
    val github: String = "",
    val avatar: String = ""
)

data class WorkExperience(
    val id: String = java.util.UUID.randomUUID().toString(),
    val jobTitle: String = "",
    val company: String = "",
    val location: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isCurrentRole: Boolean = false,
    val responsibilities: List<String> = emptyList()
)

data class Education(
    val id: String = java.util.UUID.randomUUID().toString(),
    val degree: String = "",
    val institution: String = "",
    val location: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val gpa: String = "",
    val achievements: List<String> = emptyList()
)

data class Project(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val technologies: List<String> = emptyList(),
    val link: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

data class Certification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val issuer: String = "",
    val issueDate: LocalDate? = null,
    val expiryDate: LocalDate? = null,
    val credentialId: String = ""
)

data class Language(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val proficiency: LanguageProficiency = LanguageProficiency.INTERMEDIATE
)

enum class LanguageProficiency(val displayName: String) {
    ELEMENTARY("Elementary"),
    INTERMEDIATE("Intermediate"),
    PROFICIENT("Proficient"),
    FLUENT("Fluent"),
    NATIVE("Native")
}



data class ResumeTheme(
    val templateId: String = "professional",
    val colorScheme: ColorScheme = ColorScheme(),
    val typography: TypographyScheme = TypographyScheme(),
    val layout: LayoutConfig = LayoutConfig()
)

data class ColorScheme(
    val primaryColor: Long = 0xFF1976D2, // Blue 700
    val accentColor: Long = 0xFF0288D1, // Light Blue 700
    val textColor: Long = 0xFF212121, // Grey 900
    val backgroundColor: Long = 0xFFFFFFFF, // White
    val sectionHeaderColor: Long = 0xFF1976D2, // Blue 700
    val secondaryTextColor: Long = 0xFF757575 // Grey 600
)

data class TypographyScheme(
    val fontFamily: String = "Default",
    val headerSize: Float = 24f,
    val subHeaderSize: Float = 18f,
    val bodySize: Float = 14f,
    val captionSize: Float = 12f,
    val headerWeight: Int = 700,
    val bodyWeight: Int = 400
)

data class LayoutConfig(
    val type: LayoutType = LayoutType.SINGLE_COLUMN,
    val spacing: Int = 16,
    val sectionSpacing: Int = 24,
    val sectionStyle: SectionStyle = SectionStyle.CARD
)

enum class LayoutType(val displayName: String) {
    SINGLE_COLUMN("Single Column"),
    TWO_COLUMN("Two Column"),
    SIDEBAR("Sidebar"),
    MODERN("Modern")
}

enum class SectionStyle(val displayName: String) {
    CARD("Card"),
    DIVIDER("Divider"),
    MINIMAL("Minimal"),
    BORDERED("Bordered")
}



sealed class ResumeSectionType(val id: String, val displayName: String, val isRequired: Boolean) {
    object PersonalInfo : ResumeSectionType("personal_info", "Personal Information", true)
    object Summary : ResumeSectionType("summary", "Professional Summary", false)
    object WorkExperience : ResumeSectionType("work_experience", "Work Experience", false)
    object Education : ResumeSectionType("education", "Education", false)
    object Skills : ResumeSectionType("skills", "Skills", false)
    object Projects : ResumeSectionType("projects", "Projects", false)
    object Certifications : ResumeSectionType("certifications", "Certifications", false)
    object Languages : ResumeSectionType("languages", "Languages", false)

    companion object {
        fun fromId(id: String): ResumeSectionType? = when (id) {
            "personal_info" -> PersonalInfo
            "summary" -> Summary
            "work_experience" -> WorkExperience
            "education" -> Education
            "skills" -> Skills
            "projects" -> Projects
            "certifications" -> Certifications
            "languages" -> Languages
            else -> null
        }

        fun getAll(): List<ResumeSectionType> = listOf(
            PersonalInfo, Summary, WorkExperience, Education,
            Skills, Projects, Certifications, Languages
        )
    }
}

data class SectionConfig(
    val sectionType: ResumeSectionType,
    val isVisible: Boolean = true,
    val order: Int
)

fun getDefaultSectionConfig(): List<SectionConfig> = listOf(
    SectionConfig(ResumeSectionType.PersonalInfo, isVisible = true, order = 0),
    SectionConfig(ResumeSectionType.Summary, isVisible = true, order = 1),
    SectionConfig(ResumeSectionType.WorkExperience, isVisible = true, order = 2),
    SectionConfig(ResumeSectionType.Education, isVisible = true, order = 3),
    SectionConfig(ResumeSectionType.Skills, isVisible = true, order = 4),
    SectionConfig(ResumeSectionType.Projects, isVisible = true, order = 5),
    SectionConfig(ResumeSectionType.Certifications, isVisible = true, order = 6),
    SectionConfig(ResumeSectionType.Languages, isVisible = true, order = 7)
)
