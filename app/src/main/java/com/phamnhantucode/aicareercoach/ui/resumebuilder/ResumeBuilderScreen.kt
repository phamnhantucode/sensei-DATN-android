package com.phamnhantucode.aicareercoach.ui.resumebuilder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamnhantucode.aicareercoach.ui.components.InsetAwareColumn
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme

@Composable
fun ResumeBuilderScreen(
    onBack: () -> Unit = {},
    viewModel: ResumeBuilderViewModel = viewModel()
) {
    val resume by viewModel.resume.collectAsState()
    var expandedSection by remember { mutableStateOf<ResumeSection?>(null) }

    AppTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            InsetAwareColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with back button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            Column {
                                Text(
                                    text = "Resume Builder",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Build your professional resume",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { /* Export resume */ },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FileDownload,
                                contentDescription = "Export",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export")
                        }
                    }
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Personal Information Section
                    ResumeSectionCard(
                        title = "Personal Information",
                        icon = Icons.Outlined.Person,
                        isExpanded = expandedSection == ResumeSection.PERSONAL_INFO,
                        isComplete = resume.personalInfo.isComplete(),
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
                            onUpdate = viewModel::updatePersonalInfo
                        )
                    }

                    // Professional Summary
                    ResumeSectionCard(
                        title = "Professional Summary",
                        icon = Icons.Outlined.WorkOutline,
                        isExpanded = expandedSection == ResumeSection.SUMMARY,
                        isComplete = resume.professionalSummary.isNotBlank(),
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

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun ResumeSectionCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    isComplete: Boolean,
    itemCount: Int? = null,
    onToggle: () -> Unit,
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
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
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
                    if (isComplete) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Complete",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
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
    onUpdate: (PersonalInfo) -> Unit
) {
    var editedInfo by remember(personalInfo) { mutableStateOf(personalInfo) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            label = { Text("Portfolio URL (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = editedInfo.github,
            onValueChange = {
                editedInfo = editedInfo.copy(github = it)
                onUpdate(editedInfo)
            },
            label = { Text("GitHub URL (Optional)") },
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
    var editedExperience by remember(experience) { mutableStateOf(experience) }

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
        }
    }
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

            OutlinedTextField(
                value = editedEducation.location,
                onValueChange = {
                    editedEducation = editedEducation.copy(location = it)
                    onUpdate(editedEducation)
                },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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
