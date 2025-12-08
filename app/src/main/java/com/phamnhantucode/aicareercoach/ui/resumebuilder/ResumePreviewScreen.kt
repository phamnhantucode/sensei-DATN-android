package com.phamnhantucode.aicareercoach.ui.resumebuilder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumePreviewScreen(
    resume: Resume,
    onBack: () -> Unit,
    onExport: (ResumeExportFormat) -> Unit
) {
    val theme = resume.theme
    val backgroundColor = Color(theme.colorScheme.backgroundColor)
    val textColor = Color(theme.colorScheme.textColor)
    val primaryColor = Color(theme.colorScheme.primaryColor)
    val secondaryTextColor = Color(theme.colorScheme.secondaryTextColor)

    var exportMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resume Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { exportMenuExpanded = true }) {
                            Icon(Icons.Filled.FileDownload, "Export")
                        }
                        DropdownMenu(
                            expanded = exportMenuExpanded,
                            onDismissRequest = { exportMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                onClick = {
                                    exportMenuExpanded = false
                                    onExport(ResumeExportFormat.PDF)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as Markdown") },
                                onClick = {
                                    exportMenuExpanded = false
                                    onExport(ResumeExportFormat.MARKDOWN)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        // Resume content with theme applied
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(theme.layout.spacing.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionSpacing.dp)
        ) {
            // Get visible sections in order
            val visibleSections = resume.sectionConfig
                .filter { it.isVisible }
                .sortedBy { it.order }

            visibleSections.forEach { section ->
                when (section.sectionType) {
                    ResumeSectionType.PersonalInfo -> {
                        ThemedPersonalInfoSection(resume.personalInfo, theme)
                    }
                    ResumeSectionType.Summary -> {
                        if (resume.professionalSummary.isNotBlank()) {
                            ThemedSection(
                                title = "Professional Summary",
                                theme = theme
                            ) {
                                ThemedText(
                                    text = resume.professionalSummary,
                                    color = textColor,
                                    fontSize = theme.typography.bodySize.sp,
                                    fontWeight = FontWeight(theme.typography.bodyWeight)
                                )
                            }
                        }
                    }
                    ResumeSectionType.WorkExperience -> {
                        if (resume.workExperiences.isNotEmpty()) {
                            ThemedSection(
                                title = "Work Experience",
                                theme = theme
                            ) {
                                resume.workExperiences.forEach { exp ->
                                    ThemedWorkExperience(exp, theme)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                    ResumeSectionType.Education -> {
                        if (resume.education.isNotEmpty()) {
                            ThemedSection(
                                title = "Education",
                                theme = theme
                            ) {
                                resume.education.forEach { edu ->
                                    ThemedEducation(edu, theme)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                    ResumeSectionType.Skills -> {
                        if (resume.skills.isNotEmpty()) {
                            ThemedSection(
                                title = "Skills",
                                theme = theme
                            ) {
                                ThemedSkills(resume.skills, theme)
                            }
                        }
                    }
                    ResumeSectionType.Projects -> {
                        if (resume.projects.isNotEmpty()) {
                            ThemedSection(
                                title = "Projects",
                                theme = theme
                            ) {
                                resume.projects.forEach { project ->
                                    ThemedProject(project, theme)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                    ResumeSectionType.Certifications -> {
                        if (resume.certifications.isNotEmpty()) {
                            ThemedSection(
                                title = "Certifications",
                                theme = theme
                            ) {
                                resume.certifications.forEach { cert ->
                                    ThemedCertification(cert, theme)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                    ResumeSectionType.Languages -> {
                        if (resume.languages.isNotEmpty()) {
                            ThemedSection(
                                title = "Languages",
                                theme = theme
                            ) {
                                ThemedLanguages(resume.languages, theme)
                            }
                        }
                    }
                }
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ThemedPersonalInfoSection(info: PersonalInfo, theme: ResumeTheme) {
    val primaryColor = Color(theme.colorScheme.primaryColor)
    val textColor = Color(theme.colorScheme.textColor)
    val secondaryTextColor = Color(theme.colorScheme.secondaryTextColor)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Name
        ThemedText(
            text = info.fullName,
            color = primaryColor,
            fontSize = theme.typography.headerSize.sp,
            fontWeight = FontWeight(theme.typography.headerWeight)
        )

        // Profession
        if (info.profession.isNotBlank()) {
            ThemedText(
                text = info.profession.uppercase(),
                color = textColor,
                fontSize = theme.typography.subHeaderSize.sp,
                fontWeight = FontWeight(theme.typography.headerWeight)
            )
        }

        // Contact Info
        ThemedText(
            text = "${info.email} • ${info.phone}",
            color = secondaryTextColor,
            fontSize = theme.typography.captionSize.sp
        )

        ThemedText(
            text = info.location,
            color = secondaryTextColor,
            fontSize = theme.typography.captionSize.sp
        )

        // Optional links
        if (info.linkedIn.isNotBlank() || info.portfolio.isNotBlank() || info.github.isNotBlank()) {
            val links = listOfNotNull(
                info.linkedIn.takeIf { it.isNotBlank() },
                info.portfolio.takeIf { it.isNotBlank() },
                info.github.takeIf { it.isNotBlank() }
            ).joinToString(" • ")

            ThemedText(
                text = links,
                color = secondaryTextColor,
                fontSize = theme.typography.captionSize.sp
            )
        }
    }
}

@Composable
private fun ThemedSection(
    title: String,
    theme: ResumeTheme,
    content: @Composable () -> Unit
) {
    val sectionHeaderColor = Color(theme.colorScheme.sectionHeaderColor)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Title
        ThemedText(
            text = title.uppercase(),
            color = sectionHeaderColor,
            fontSize = theme.typography.subHeaderSize.sp,
            fontWeight = FontWeight(theme.typography.headerWeight)
        )

        // Section divider/style
        when (theme.layout.sectionStyle) {
            SectionStyle.DIVIDER -> {
                Divider(color = sectionHeaderColor, thickness = 2.dp)
            }
            SectionStyle.BORDERED -> {
                Divider(color = sectionHeaderColor, thickness = 1.dp)
            }
            else -> { /* Minimal or Card - no divider */ }
        }

        // Content
        content()
    }
}

@Composable
private fun ThemedWorkExperience(experience: WorkExperience, theme: ResumeTheme) {
    val textColor = Color(theme.colorScheme.textColor)
    val secondaryTextColor = Color(theme.colorScheme.secondaryTextColor)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Job Title & Company
        ThemedText(
            text = experience.jobTitle,
            color = textColor,
            fontSize = theme.typography.subHeaderSize.sp,
            fontWeight = FontWeight(theme.typography.headerWeight)
        )

        ThemedText(
            text = "${experience.company} • ${experience.location}",
            color = secondaryTextColor,
            fontSize = theme.typography.bodySize.sp
        )

        // Dates
        val dateRange = if (experience.isCurrentRole) {
            "${experience.startDate?.format(dateFormatter) ?: ""} - Present"
        } else {
            "${experience.startDate?.format(dateFormatter) ?: ""} - ${experience.endDate?.format(dateFormatter) ?: ""}"
        }
        ThemedText(
            text = dateRange,
            color = secondaryTextColor,
            fontSize = theme.typography.captionSize.sp
        )

        // Responsibilities
        if (experience.responsibilities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            experience.responsibilities.forEach { resp ->
                ThemedText(
                    text = "• $resp",
                    color = textColor,
                    fontSize = theme.typography.bodySize.sp
                )
            }
        }
    }
}

@Composable
private fun ThemedEducation(education: Education, theme: ResumeTheme) {
    val textColor = Color(theme.colorScheme.textColor)
    val secondaryTextColor = Color(theme.colorScheme.secondaryTextColor)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ThemedText(
            text = education.degree,
            color = textColor,
            fontSize = theme.typography.subHeaderSize.sp,
            fontWeight = FontWeight(theme.typography.headerWeight)
        )

        ThemedText(
            text = "${education.institution} • ${education.location}",
            color = secondaryTextColor,
            fontSize = theme.typography.bodySize.sp
        )

        if (education.startDate != null || education.endDate != null) {
            val dateRange = "${education.startDate?.format(dateFormatter) ?: ""} - ${education.endDate?.format(dateFormatter) ?: ""}"
            ThemedText(
                text = dateRange,
                color = secondaryTextColor,
                fontSize = theme.typography.captionSize.sp
            )
        }

        if (education.gpa.isNotBlank()) {
            ThemedText(
                text = "GPA: ${education.gpa}",
                color = secondaryTextColor,
                fontSize = theme.typography.bodySize.sp
            )
        }
    }
}

@Composable
private fun ThemedSkills(skills: List<String>, theme: ResumeTheme) {
    val textColor = Color(theme.colorScheme.textColor)

    ThemedText(
        text = skills.joinToString(" • "),
        color = textColor,
        fontSize = theme.typography.bodySize.sp
    )
}

@Composable
private fun ThemedProject(project: Project, theme: ResumeTheme) {
    val textColor = Color(theme.colorScheme.textColor)
    val secondaryTextColor = Color(theme.colorScheme.secondaryTextColor)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ThemedText(
            text = project.title,
            color = textColor,
            fontSize = theme.typography.subHeaderSize.sp,
            fontWeight = FontWeight(theme.typography.headerWeight)
        )

        if (project.description.isNotBlank()) {
            ThemedText(
                text = project.description,
                color = textColor,
                fontSize = theme.typography.bodySize.sp
            )
        }

        if (project.technologies.isNotEmpty()) {
            ThemedText(
                text = "Technologies: ${project.technologies.joinToString(", ")}",
                color = secondaryTextColor,
                fontSize = theme.typography.captionSize.sp
            )
        }

        if (project.link.isNotBlank()) {
            ThemedText(
                text = project.link,
                color = secondaryTextColor,
                fontSize = theme.typography.captionSize.sp
            )
        }
    }
}

@Composable
private fun ThemedCertification(certification: Certification, theme: ResumeTheme) {
    val textColor = Color(theme.colorScheme.textColor)
    val secondaryTextColor = Color(theme.colorScheme.secondaryTextColor)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ThemedText(
            text = certification.name,
            color = textColor,
            fontSize = theme.typography.bodySize.sp,
            fontWeight = FontWeight(theme.typography.headerWeight)
        )

        ThemedText(
            text = certification.issuer,
            color = secondaryTextColor,
            fontSize = theme.typography.bodySize.sp
        )

        if (certification.issueDate != null) {
            ThemedText(
                text = "Issued: ${certification.issueDate.format(dateFormatter)}",
                color = secondaryTextColor,
                fontSize = theme.typography.captionSize.sp
            )
        }
    }
}

@Composable
private fun ThemedLanguages(languages: List<Language>, theme: ResumeTheme) {
    val textColor = Color(theme.colorScheme.textColor)
    val secondaryTextColor = Color(theme.colorScheme.secondaryTextColor)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        languages.forEach { language ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThemedText(
                    text = language.name,
                    color = textColor,
                    fontSize = theme.typography.bodySize.sp
                )
                ThemedText(
                    text = language.proficiency.displayName,
                    color = secondaryTextColor,
                    fontSize = theme.typography.bodySize.sp
                )
            }
        }
    }
}

@Composable
private fun ThemedText(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight
    )
}
