package com.phamnhantucode.aicareercoach.ui.resumebuilder

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamnhantucode.aicareercoach.ui.components.InsetAwareColumn
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme
import kotlinx.coroutines.launch
import com.phamnhantucode.aicareercoach.ui.components.MonthYearPickerDialog
import java.time.YearMonth
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class ResumeBuilderTab(val title: String) {
    FORM("Form"),
    DESIGN("Design"),
    MARKDOWN("Markdown")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeBuilderScreen(
    resumeId: String? = null,
    onBack: () -> Unit = {},
    onNavigateToGridEditor: (designId: String?, templateAssetPath: String?, isNewDesign: Boolean, linkedResumeId: String?) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val viewModel: ResumeBuilderViewModel = viewModel { ResumeBuilderViewModel(context, resumeId) }
    val coroutineScope = rememberCoroutineScope()

    val resume by viewModel.resume.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val latestContext by rememberUpdatedState(context)
    
    val tabs = ResumeBuilderTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    // Handle export events
    LaunchedEffect(viewModel) {
        viewModel.exportEvents.collect { event ->
            when (event) {
                is ResumeExportResult.Success -> {
                    val message = "${event.format.displayName} saved to Downloads as ${event.fileName}"
                    Toast.makeText(latestContext, message, Toast.LENGTH_LONG).show()
                }
                is ResumeExportResult.Error -> {
                    val error = event.throwable.localizedMessage ?: "Unknown error"
                    val message = "Failed to export ${event.format.displayName}: $error"
                    Toast.makeText(latestContext, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Handle save events
    LaunchedEffect(viewModel) {
        viewModel.saveEvents.collect { event ->
            when (event) {
                is SaveResult.Success -> {
                    Toast.makeText(latestContext, event.message, Toast.LENGTH_SHORT).show()
                }
                is SaveResult.Error -> {
                    Toast.makeText(latestContext, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Save data when leaving the screen (lifecycle-aware save)
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.saveResume(showToast = false)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with back button and save
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp + WindowInsets.systemBars.asPaddingValues().calculateTopPadding(),
                            bottom = 0.dp
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Resume Builder",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isSaving) "Saving..." else if (isLoading) "Loading..." else "Auto-save enabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Save Button
                        Button(
                            onClick = { viewModel.saveResume(showToast = true) },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            enabled = !isSaving
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Save",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSaving) "Saving..." else "Save")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Tab Row
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        edgePadding = 0.dp,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        // Auto-save when switching to Design tab to ensure Resume exists in DB
                                        if (tab == ResumeBuilderTab.DESIGN && pagerState.currentPage != index) {
                                            viewModel.ensureResumeSaved()
                                        }
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                text = {
                                    Text(
                                        text = tab.title,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Auto-save when switching to Design tab (handles swipe navigation)
            LaunchedEffect(pagerState.currentPage) {
                if (tabs[pagerState.currentPage] == ResumeBuilderTab.DESIGN) {
                    viewModel.ensureResumeSaved()
                }
            }

            // Tab Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (tabs[page]) {
                    ResumeBuilderTab.FORM -> {
                        ResumeFormContent(
                            viewModel = viewModel,
                            resume = resume
                        )
                    }
                    ResumeBuilderTab.DESIGN -> {
                        ResumeDesignContent(
                            resumeId = resume.id,
                            onNavigateToGridEditor = onNavigateToGridEditor
                        )
                    }
                    ResumeBuilderTab.MARKDOWN -> {
                        ResumeMarkdownContent()
                    }
                }
            }
        }
    }
}

/**
 * Form tab content - displays all resume form sections
 */
@Composable
private fun ResumeFormContent(
    viewModel: ResumeBuilderViewModel,
    resume: Resume
) {
    var expandedSection by remember { mutableStateOf<ResumeSection?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Helper function to get section visibility
        fun getSectionVisibility(type: ResumeSectionType): Boolean {
            return resume.sectionConfig.find { it.sectionType == type }?.isVisible ?: true
        }

        // Personal Information Section (Required - no visibility toggle)
        ResumeSectionCard(
            title = "Personal Information",
            icon = Icons.Outlined.Person,
            isExpanded = expandedSection == ResumeSection.PERSONAL_INFO,
            isComplete = resume.personalInfo.isComplete(),
            isVisible = getSectionVisibility(ResumeSectionType.PersonalInfo),
            canToggleVisibility = false,
            onToggle = {
                expandedSection = if (expandedSection == ResumeSection.PERSONAL_INFO) {
                    null
                } else {
                    ResumeSection.PERSONAL_INFO
                }
            }
        ) {
            PersonalInfoForm(
                personalInfo = resume.personalInfo,
                onUpdate = viewModel::updatePersonalInfo,
                onAutofillFromProfile = {
                    coroutineScope.launch {
                        viewModel.autofillFromUserProfile()
                    }
                }
            )
        }

        // Professional Summary
        ResumeSectionCard(
            title = "Professional Summary",
            icon = Icons.Outlined.WorkOutline,
            isExpanded = expandedSection == ResumeSection.SUMMARY,
            isComplete = resume.professionalSummary.isNotBlank(),
            isVisible = getSectionVisibility(ResumeSectionType.Summary),
            canToggleVisibility = true,
            onToggleVisibility = { viewModel.toggleSectionVisibility(ResumeSectionType.Summary) },
            onToggle = {
                expandedSection = if (expandedSection == ResumeSection.SUMMARY) {
                    null
                } else {
                    ResumeSection.SUMMARY
                }
            }
        ) {
            ProfessionalSummaryForm(
                summary = resume.professionalSummary,
                onUpdate = viewModel::updateProfessionalSummary
            )
        }

        // Work Experience
        ResumeSectionCard(
            title = "Work Experience",
            icon = Icons.Outlined.Business,
            isExpanded = expandedSection == ResumeSection.WORK_EXPERIENCE,
            isComplete = resume.workExperiences.isNotEmpty(),
            itemCount = resume.workExperiences.size,
            isVisible = getSectionVisibility(ResumeSectionType.WorkExperience),
            canToggleVisibility = true,
            onToggleVisibility = { viewModel.toggleSectionVisibility(ResumeSectionType.WorkExperience) },
            onToggle = {
                expandedSection = if (expandedSection == ResumeSection.WORK_EXPERIENCE) {
                    null
                } else {
                    ResumeSection.WORK_EXPERIENCE
                }
            }
        ) {
            WorkExperienceSection(
                experiences = resume.workExperiences,
                onAdd = { viewModel.addWorkExperience(WorkExperience()) },
                onUpdate = viewModel::updateWorkExperience,
                onRemove = viewModel::removeWorkExperience
            )
        }

        // Education
        ResumeSectionCard(
            title = "Education",
            icon = Icons.Outlined.School,
            isExpanded = expandedSection == ResumeSection.EDUCATION,
            isComplete = resume.education.isNotEmpty(),
            itemCount = resume.education.size,
            isVisible = getSectionVisibility(ResumeSectionType.Education),
            canToggleVisibility = true,
            onToggleVisibility = { viewModel.toggleSectionVisibility(ResumeSectionType.Education) },
            onToggle = {
                expandedSection = if (expandedSection == ResumeSection.EDUCATION) {
                    null
                } else {
                    ResumeSection.EDUCATION
                }
            }
        ) {
            EducationSection(
                education = resume.education,
                onAdd = { viewModel.addEducation(Education()) },
                onUpdate = viewModel::updateEducation,
                onRemove = viewModel::removeEducation
            )
        }

        // Skills
        ResumeSectionCard(
            title = "Skills",
            icon = Icons.Outlined.Language,
            isExpanded = expandedSection == ResumeSection.SKILLS,
            isComplete = resume.skills.isNotEmpty(),
            itemCount = resume.skills.size,
            isVisible = getSectionVisibility(ResumeSectionType.Skills),
            canToggleVisibility = true,
            onToggleVisibility = { viewModel.toggleSectionVisibility(ResumeSectionType.Skills) },
            onToggle = {
                expandedSection = if (expandedSection == ResumeSection.SKILLS) {
                    null
                } else {
                    ResumeSection.SKILLS
                }
            }
        ) {
            SkillsSection(
                skills = resume.skills,
                onAdd = viewModel::addSkill,
                onRemove = viewModel::removeSkill
            )
        }

        // Projects
        ResumeSectionCard(
            title = "Projects",
            icon = Icons.Outlined.WorkOutline,
            isExpanded = expandedSection == ResumeSection.PROJECTS,
            isComplete = resume.projects.isNotEmpty(),
            itemCount = resume.projects.size,
            isVisible = getSectionVisibility(ResumeSectionType.Projects),
            canToggleVisibility = true,
            onToggleVisibility = { viewModel.toggleSectionVisibility(ResumeSectionType.Projects) },
            onToggle = {
                expandedSection = if (expandedSection == ResumeSection.PROJECTS) {
                    null
                } else {
                    ResumeSection.PROJECTS
                }
            }
        ) {
            ProjectsSection(
                projects = resume.projects,
                onAdd = { viewModel.addProject(Project()) },
                onUpdate = viewModel::updateProject,
                onRemove = viewModel::removeProject
            )
        }

        // Certifications
        ResumeSectionCard(
            title = "Certifications",
            icon = Icons.Outlined.School,
            isExpanded = expandedSection == ResumeSection.CERTIFICATIONS,
            isComplete = resume.certifications.isNotEmpty(),
            itemCount = resume.certifications.size,
            isVisible = getSectionVisibility(ResumeSectionType.Certifications),
            canToggleVisibility = true,
            onToggleVisibility = { viewModel.toggleSectionVisibility(ResumeSectionType.Certifications) },
            onToggle = {
                expandedSection = if (expandedSection == ResumeSection.CERTIFICATIONS) {
                    null
                } else {
                    ResumeSection.CERTIFICATIONS
                }
            }
        ) {
            CertificationsSection(
                certifications = resume.certifications,
                onAdd = { viewModel.addCertification(Certification()) },
                onUpdate = viewModel::updateCertification,
                onRemove = viewModel::removeCertification
            )
        }

        // Languages
        ResumeSectionCard(
            title = "Languages",
            icon = Icons.Outlined.Language,
            isExpanded = expandedSection == ResumeSection.LANGUAGES,
            isComplete = resume.languages.isNotEmpty(),
            itemCount = resume.languages.size,
            isVisible = getSectionVisibility(ResumeSectionType.Languages),
            canToggleVisibility = true,
            onToggleVisibility = { viewModel.toggleSectionVisibility(ResumeSectionType.Languages) },
            onToggle = {
                expandedSection = if (expandedSection == ResumeSection.LANGUAGES) {
                    null
                } else {
                    ResumeSection.LANGUAGES
                }
            }
        ) {
            LanguagesSection(
                languages = resume.languages,
                onAdd = { viewModel.addLanguage(Language()) },
                onUpdate = viewModel::updateLanguage,
                onRemove = viewModel::removeLanguage
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Design tab content - embeds ResumeDesignScreen content
 */
@Composable
private fun ResumeDesignContent(
    resumeId: String,
    onNavigateToGridEditor: (designId: String?, templateAssetPath: String?, isNewDesign: Boolean, linkedResumeId: String?) -> Unit
) {
    com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumeDesignContent(
        resumeId = resumeId,
        onNavigateToGridEditor = onNavigateToGridEditor
    )
}

/**
 * Markdown tab content - embeds ResumeMarkdownScreen content
 */
@Composable
private fun ResumeMarkdownContent() {
    ResumeMarkdownContentInternal()
}

@Composable
private fun ResumeSectionCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    isComplete: Boolean,
    itemCount: Int? = null,
    onToggle: () -> Unit,
    isVisible: Boolean = true,
    canToggleVisibility: Boolean = false,
    onToggleVisibility: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!isVisible) {
                                Text(
                                    text = "Hidden",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.errorContainer,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (itemCount != null) {
                            Text(
                                text = "$itemCount ${if (itemCount == 1) "item" else "items"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Visibility toggle
                    if (canToggleVisibility && onToggleVisibility != null) {
//                        Switch(
//                            checked = isVisible,
//                            onCheckedChange = { onToggleVisibility() },
//                            modifier = Modifier.clickable(onClick = onToggleVisibility)
//                        )
                    }

                    if (isComplete) {
//                        Icon(
//                            imageVector = Icons.Filled.CheckCircle,
//                            contentDescription = "Complete",
//                            tint = MaterialTheme.colorScheme.primary,
//                            modifier = Modifier.size(20.dp)
//                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            // Content
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalInfoForm(
    personalInfo: PersonalInfo,
    onUpdate: (PersonalInfo) -> Unit,
    onAutofillFromProfile: () -> Unit = {}
) {
    var editedInfo by remember(personalInfo) { mutableStateOf(personalInfo) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Auto-fill button
        OutlinedButton(
            onClick = onAutofillFromProfile,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("✨ Auto-fill from My Profile")
        }

        OutlinedTextField(
            value = editedInfo.fullName,
            onValueChange = {
                editedInfo = editedInfo.copy(fullName = it)
                onUpdate(editedInfo)
            },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = editedInfo.profession,
            onValueChange = {
                editedInfo = editedInfo.copy(profession = it)
                onUpdate(editedInfo)
            },
            label = { Text("Profession / Job Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )



        OutlinedTextField(
            value = editedInfo.email,
            onValueChange = {
                editedInfo = editedInfo.copy(email = it)
                onUpdate(editedInfo)
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = editedInfo.phone,
            onValueChange = {
                editedInfo = editedInfo.copy(phone = it)
                onUpdate(editedInfo)
            },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = editedInfo.location,
            onValueChange = {
                editedInfo = editedInfo.copy(location = it)
                onUpdate(editedInfo)
            },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = editedInfo.linkedIn,
            onValueChange = {
                editedInfo = editedInfo.copy(linkedIn = it)
                onUpdate(editedInfo)
            },
            label = { Text("LinkedIn URL (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = editedInfo.portfolio,
            onValueChange = {
                editedInfo = editedInfo.copy(portfolio = it)
                onUpdate(editedInfo)
            },
            label = { Text("Website URL (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfessionalSummaryForm(
    summary: String,
    onUpdate: (String) -> Unit
) {
    OutlinedTextField(
        value = summary,
        onValueChange = onUpdate,
        label = { Text("Professional Summary") },
        placeholder = { Text("Write a brief summary of your professional background, key skills, and career objectives...") },
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        maxLines = 6
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkExperienceSection(
    experiences: List<WorkExperience>,
    onAdd: () -> Unit,
    onUpdate: (WorkExperience) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        experiences.forEach { experience ->
            WorkExperienceItem(
                experience = experience,
                onUpdate = onUpdate,
                onRemove = { onRemove(experience.id) }
            )
        }

        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add work experience",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Work Experience")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkExperienceItem(
    experience: WorkExperience,
    onUpdate: (WorkExperience) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    var editedExperience by remember(experience) { mutableStateOf(experience) }
    var showDatePicker by remember { mutableStateOf<DatePickerType?>(null) }
    var showAIDialog by remember { mutableStateOf(false) }
    var responsibilitiesText by remember(experience) {
        mutableStateOf(experience.responsibilities.joinToString("\n"))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editedExperience.jobTitle.isBlank()) "New Position" else editedExperience.jobTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = editedExperience.jobTitle,
                onValueChange = {
                    editedExperience = editedExperience.copy(jobTitle = it)
                    onUpdate(editedExperience)
                },
                label = { Text("Job Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editedExperience.company,
                onValueChange = {
                    editedExperience = editedExperience.copy(company = it)
                    onUpdate(editedExperience)
                },
                label = { Text("Company") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editedExperience.location,
                onValueChange = {
                    editedExperience = editedExperience.copy(location = it)
                    onUpdate(editedExperience)
                },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date fields
            val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = editedExperience.startDate?.format(dateFormatter) ?: "",
                    onValueChange = { },
                    label = { Text("Start Date") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = DatePickerType.START }) {
                            Icon(Icons.Filled.Edit, "Select date")
                        }
                    },
                    placeholder = { Text("Click to select") }
                )

                OutlinedTextField(
                    value = if (editedExperience.isCurrentRole) "Present" else (editedExperience.endDate?.format(dateFormatter) ?: ""),
                    onValueChange = { },
                    label = { Text("End Date") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    enabled = !editedExperience.isCurrentRole,
                    trailingIcon = {
                        if (!editedExperience.isCurrentRole) {
                            IconButton(onClick = { showDatePicker = DatePickerType.END }) {
                                Icon(Icons.Filled.Edit, "Select date")
                            }
                        }
                    },
                    placeholder = { Text("Click to select") }
                )
            }

            // Current Role Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = editedExperience.isCurrentRole,
                    onCheckedChange = {
                        editedExperience = editedExperience.copy(isCurrentRole = it)
                        onUpdate(editedExperience)
                    }
                )
                Text("I currently work here")
            }

            // Responsibilities field with AI button
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Experience in Company",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Button(
                        onClick = {
                            showAIDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        enabled = responsibilitiesText.isNotBlank() &&
                                 editedExperience.jobTitle.isNotBlank() &&
                                 editedExperience.company.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,    
                            contentDescription = "Improve with AI",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Improve with AI", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = responsibilitiesText,
                    onValueChange = {
                        responsibilitiesText = it
                        val responsibilities = it.split("\n").filter { line -> line.isNotBlank() }
                        editedExperience = editedExperience.copy(responsibilities = responsibilities)
                        onUpdate(editedExperience)
                    },
                    label = { Text("Responsibilities (one per line)") },
                    placeholder = { Text("• Led team of 5 engineers\n• Improved performance by 40%\n• Implemented new features") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker != null) {
        val initialDate = when (showDatePicker) {
            DatePickerType.START -> editedExperience.startDate
            DatePickerType.END -> editedExperience.endDate
            else -> null
        }

        val initialMillis = initialDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = null },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()

                            editedExperience = when (showDatePicker) {
                                DatePickerType.START -> editedExperience.copy(startDate = selectedDate)
                                DatePickerType.END -> editedExperience.copy(endDate = selectedDate)
                                else -> editedExperience
                            }
                            onUpdate(editedExperience)
                        }
                        showDatePicker = null
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDatePicker = null }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // AI Improvement Dialog
    if (showAIDialog) {
        ResponsibilitiesAIDialog(
            currentText = responsibilitiesText,
            jobTitle = editedExperience.jobTitle,
            company = editedExperience.company,
            onDismiss = { showAIDialog = false },
            onSelectText = { improvedText ->
                responsibilitiesText = improvedText
                val responsibilities = improvedText.split("\n").filter { line -> line.isNotBlank() }
                editedExperience = editedExperience.copy(responsibilities = responsibilities)
                onUpdate(editedExperience)
            }
        )
    }
}

private enum class DatePickerType {
    START, END
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EducationSection(
    education: List<Education>,
    onAdd: () -> Unit,
    onUpdate: (Education) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        education.forEach { edu ->
            EducationItem(
                education = edu,
                onUpdate = onUpdate,
                onRemove = { onRemove(edu.id) }
            )
        }

        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add education",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Education")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EducationItem(
    education: Education,
    onUpdate: (Education) -> Unit,
    onRemove: () -> Unit
) {
    var editedEducation by remember(education) { mutableStateOf(education) }
    // Date formatter for yyyy-MM
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM", Locale.getDefault()) }
    var showDatePicker by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editedEducation.degree.isBlank()) "New Education" else editedEducation.degree,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = editedEducation.degree,
                onValueChange = {
                    editedEducation = editedEducation.copy(degree = it)
                    onUpdate(editedEducation)
                },
                label = { Text("Degree") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editedEducation.institution,
                onValueChange = {
                    editedEducation = editedEducation.copy(institution = it)
                    onUpdate(editedEducation)
                },
                label = { Text("Institution") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Location field removed as per request
            // Start Date field removed as per request

            // Graduated Date (yyyy-MM)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = editedEducation.endDate?.format(dateFormatter) ?: "",
                    onValueChange = { },
                    label = { Text("Graduated Date (yyyy-MM)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Click to select") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.Edit, "Select date")
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(end = 48.dp) // Exclude trailing icon area
                        .clickable { showDatePicker = true }
                )
            }

            OutlinedTextField(
                value = editedEducation.gpa,
                onValueChange = {
                    editedEducation = editedEducation.copy(gpa = it)
                    onUpdate(editedEducation)
                },
                label = { Text("GPA (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }

    if (showDatePicker) {
        val initialDate = if (editedEducation.endDate != null) {
            YearMonth.from(editedEducation.endDate)
        } else {
            YearMonth.now()
        }

        MonthYearPickerDialog(
            initialDate = initialDate,
            onDateSelected = { selected ->
                showDatePicker = false
                editedEducation = editedEducation.copy(endDate = selected.atDay(1))
                onUpdate(editedEducation)
            },
            onDismissRequest = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SkillsSection(
    skills: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var newSkill by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newSkill,
                onValueChange = { newSkill = it },
                label = { Text("Add Skill") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Button(
                onClick = {
                    if (newSkill.isNotBlank()) {
                        onAdd(newSkill)
                        newSkill = ""
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (skills.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skills.forEach { skill ->
                    SkillChip(
                        skill = skill,
                        onRemove = { onRemove(skill) }
                    )
                }
            }
        } else {
            Text(
                text = "No skills added yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun SkillChip(
    skill: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = skill,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove skill",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectsSection(
    projects: List<Project>,
    onAdd: () -> Unit,
    onUpdate: (Project) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Project",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Project")
        }

        if (projects.isEmpty()) {
            Text(
                text = "No projects added yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        projects.forEach { project ->
            ProjectItem(
                project = project,
                onUpdate = onUpdate,
                onRemove = { onRemove(project.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProjectItem(
    project: Project,
    onUpdate: (Project) -> Unit,
    onRemove: () -> Unit
) {
    var editedProject by remember { mutableStateOf(project) }
    var showDatePicker by remember { mutableStateOf<DatePickerType?>(null) }
    val context = LocalContext.current

    LaunchedEffect(project) {
        editedProject = project
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = editedProject.title,
                onValueChange = {
                    editedProject = editedProject.copy(title = it)
                    onUpdate(editedProject)
                },
                label = { Text("Project Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editedProject.description,
                onValueChange = {
                    editedProject = editedProject.copy(description = it)
                    onUpdate(editedProject)
                },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4
            )

//            OutlinedTextField(
//                value = editedProject.link,
//                onValueChange = {
//                    editedProject = editedProject.copy(link = it)
//                    onUpdate(editedProject)
//                },
//                label = { Text("Project Link (Optional)") },
//                modifier = Modifier.fillMaxWidth(),
//                singleLine = true,
//                placeholder = { Text("https://github.com/...") }
//            )

            // Technologies as chips
            var newTech by remember { mutableStateOf("") }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Technologies Used",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTech,
                        onValueChange = { newTech = it },
                        label = { Text("Add Technology") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (newTech.isNotBlank() && newTech !in editedProject.technologies) {
                                editedProject = editedProject.copy(
                                    technologies = editedProject.technologies + newTech.trim()
                                )
                                onUpdate(editedProject)
                                newTech = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (editedProject.technologies.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        editedProject.technologies.forEach { tech ->
                            SkillChip(
                                skill = tech,
                                onRemove = {
                                    editedProject = editedProject.copy(
                                        technologies = editedProject.technologies.filter { it != tech }
                                    )
                                    onUpdate(editedProject)
                                }
                            )
                        }
                    }
                }
            }

            // Date fields (optional)
            val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()) }
            Text(
                text = "Project Duration (Optional)",
                style = MaterialTheme.typography.labelLarge
            )
//            // Date fields
//            val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()) }
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                OutlinedTextField(
//                    value = editedProject.startDate?.format(dateFormatter) ?: "",
//                    onValueChange = { },
//                    label = { Text("Start Date") },
//                    modifier = Modifier.weight(1f),
//                    readOnly = true,
//                    trailingIcon = {
//                        IconButton(onClick = { showDatePicker = DatePickerType.START }) {
//                            Icon(Icons.Filled.Edit, "Select date")
//                        }
//                    },
//                    placeholder = { Text("Click to select") }
//                )
//
//                OutlinedTextField(
//                    value = editedProject.endDate?.format(dateFormatter) ?: "",
//                    onValueChange = { },
//                    label = { Text("End Date") },
//                    modifier = Modifier.weight(1f),
//                    readOnly = true,
//                    trailingIcon = {
//                        IconButton(onClick = { showDatePicker = DatePickerType.END }) {
//                            Icon(Icons.Filled.Edit, "Select date")
//                        }
//                    },
//                    placeholder = { Text("Click to select") }
//                )
//            }
        }
    }

//    // Date Picker Dialog
//    if (showDatePicker != null) {
//        val initialDate = when (showDatePicker) {
//            DatePickerType.START -> editedProject.startDate
//            DatePickerType.END -> editedProject.endDate
//            else -> null
//        }
//
//        val initialMillis = initialDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
//        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
//
//        DatePickerDialog(
//            onDismissRequest = { showDatePicker = null },
//            confirmButton = {
//                Button(
//                    onClick = {
//                        datePickerState.selectedDateMillis?.let { millis ->
//                            val selectedDate = Instant.ofEpochMilli(millis)
//                                .atZone(ZoneId.systemDefault())
//                                .toLocalDate()
//
//                            editedProject = when (showDatePicker) {
//                                DatePickerType.START -> editedProject.copy(startDate = selectedDate)
//                                DatePickerType.END -> editedProject.copy(endDate = selectedDate)
//                                else -> editedProject
//                            }
//                            onUpdate(editedProject)
//                        }
//                        showDatePicker = null
//                    }
//                ) {
//                    Text("OK")
//                }
//            },
//            dismissButton = {
//                OutlinedButton(onClick = { showDatePicker = null }) {
//                    Text("Cancel")
//                }
//            }
//        ) {
//            DatePicker(state = datePickerState)
//        }
//    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CertificationsSection(
    certifications: List<Certification>,
    onAdd: () -> Unit,
    onUpdate: (Certification) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Certification",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Certification")
        }

        if (certifications.isEmpty()) {
            Text(
                text = "No certifications added yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        certifications.forEach { certification ->
            CertificationItem(
                certification = certification,
                onUpdate = onUpdate,
                onRemove = { onRemove(certification.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CertificationItem(
    certification: Certification,
    onUpdate: (Certification) -> Unit,
    onRemove: () -> Unit
) {
    var editedCert by remember { mutableStateOf(certification) }
    var showDatePicker by remember { mutableStateOf<CertDatePickerType?>(null) }

    LaunchedEffect(certification) {
        editedCert = certification
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = editedCert.name,
                onValueChange = {
                    editedCert = editedCert.copy(name = it)
                    onUpdate(editedCert)
                },
                label = { Text("Certification Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editedCert.issuer,
                onValueChange = {
                    editedCert = editedCert.copy(issuer = it)
                    onUpdate(editedCert)
                },
                label = { Text("Issuing Organization") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editedCert.credentialId,
                onValueChange = {
                    editedCert = editedCert.copy(credentialId = it)
                    onUpdate(editedCert)
                },
                label = { Text("Credential ID (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date fields
            val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = editedCert.issueDate?.format(dateFormatter) ?: "",
                    onValueChange = { },
                    label = { Text("Issue Date") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = CertDatePickerType.ISSUE }) {
                            Icon(Icons.Filled.Edit, "Select date")
                        }
                    },
                    placeholder = { Text("Click to select") }
                )

                OutlinedTextField(
                    value = editedCert.expiryDate?.format(dateFormatter) ?: "",
                    onValueChange = { },
                    label = { Text("Expiry Date (Optional)") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = CertDatePickerType.EXPIRY }) {
                            Icon(Icons.Filled.Edit, "Select date")
                        }
                    },
                    placeholder = { Text("Never expires") }
                )
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker != null) {
        val initialDate = when (showDatePicker) {
            CertDatePickerType.ISSUE -> editedCert.issueDate
            CertDatePickerType.EXPIRY -> editedCert.expiryDate
            else -> null
        }

        val initialMillis = initialDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = null },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()

                            editedCert = when (showDatePicker) {
                                CertDatePickerType.ISSUE -> editedCert.copy(issueDate = selectedDate)
                                CertDatePickerType.EXPIRY -> editedCert.copy(expiryDate = selectedDate)
                                else -> editedCert
                            }
                            onUpdate(editedCert)
                        }
                        showDatePicker = null
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDatePicker = null }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private enum class CertDatePickerType {
    ISSUE, EXPIRY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagesSection(
    languages: List<Language>,
    onAdd: () -> Unit,
    onUpdate: (Language) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Language",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Language")
        }

        if (languages.isEmpty()) {
            Text(
                text = "No languages added yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        languages.forEach { language ->
            LanguageItem(
                language = language,
                onUpdate = onUpdate,
                onRemove = { onRemove(language.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageItem(
    language: Language,
    onUpdate: (Language) -> Unit,
    onRemove: () -> Unit
) {
    var editedLanguage by remember { mutableStateOf(language) }
    var proficiencyExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(language) {
        editedLanguage = language
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = editedLanguage.name,
                onValueChange = {
                    editedLanguage = editedLanguage.copy(name = it)
                    onUpdate(editedLanguage)
                },
                label = { Text("Language") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g., English, Spanish, Mandarin") }
            )

            // Proficiency Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = editedLanguage.proficiency.displayName,
                    onValueChange = { },
                    label = { Text("Proficiency Level") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { proficiencyExpanded = true }) {
                            Icon(
                                imageVector = if (proficiencyExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Select proficiency"
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = proficiencyExpanded,
                    onDismissRequest = { proficiencyExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LanguageProficiency.values().forEach { proficiency ->
                        DropdownMenuItem(
                            text = { Text(proficiency.displayName) },
                            onClick = {
                                editedLanguage = editedLanguage.copy(proficiency = proficiency)
                                onUpdate(editedLanguage)
                                proficiencyExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private enum class ResumeSection {
    PERSONAL_INFO,
    SUMMARY,
    WORK_EXPERIENCE,
    EDUCATION,
    SKILLS,
    PROJECTS,
    CERTIFICATIONS,
    LANGUAGES
}

private fun PersonalInfo.isComplete(): Boolean {
    return fullName.isNotBlank() &&
           email.isNotBlank() &&
           phone.isNotBlank() &&
           location.isNotBlank()
}

@Preview(showBackground = true)
@Composable
private fun ResumeBuilderScreenPreview() {
    ResumeBuilderScreen()
}
