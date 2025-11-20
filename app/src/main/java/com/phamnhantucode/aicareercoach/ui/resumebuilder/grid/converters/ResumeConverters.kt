package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.converters

import androidx.compose.ui.text.font.FontWeight
import com.phamnhantucode.aicareercoach.ui.resumebuilder.PersonalInfo
import com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.ResumeSectionType
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.EditorType
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ElementStyle
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridConfig
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridPosition
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridResume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridTemplateType
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeMetadata
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumePage
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ShapeType
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.TextStyle
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.UserInfoTag
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    fun formatDates(startDate: LocalDate?, endDate: LocalDate?, isCurrent: Boolean = false): String {
        val start = startDate?.format(DateTimeFormatter.ofPattern("MMM yyyy")) ?: ""
        val end = if (isCurrent) "Present" else endDate?.format(DateTimeFormatter.ofPattern("MMM yyyy")) ?: ""
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
private fun parseDate(dateString: String): LocalDate? {
    if (dateString.isBlank() || dateString == "Present") return null
    return try {
        LocalDate.parse(
            "01 $dateString",
            DateTimeFormatter.ofPattern("dd MMM yyyy")
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
