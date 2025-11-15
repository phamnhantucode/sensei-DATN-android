package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import com.phamnhantucode.aicareercoach.data.resume.ResumeRepository
import com.phamnhantucode.aicareercoach.ui.resumebuilder.PersonalInfo
import com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume
import kotlinx.coroutines.launch

/**
 * Property panel for editing element properties
 * Shows context-sensitive controls based on element type
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyPanel(
    element: ResumeElement,
    onUpdateElement: (ResumeElement) -> Unit,
    onClose: () -> Unit,
    onRemoveElement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ResumeRepository.getInstance(context) }
    var resume by remember { mutableStateOf<Resume?>(null) }
    var personalInfo by remember { mutableStateOf<PersonalInfo?>(null) }

    // Load resume when panel opens
    LaunchedEffect(Unit) {
        scope.launch {
            val result = repository.getLatestResume()
            result.onSuccess { formResume ->
                resume = formResume
                personalInfo = formResume?.personalInfo
            }
        }
    }

    // Track previous tag to detect changes
    var previousTag by remember { mutableStateOf(element.userInfoTag) }
    
    // Auto-update text elements with user data when personalInfo loads and element has a tag
    LaunchedEffect(personalInfo, element.userInfoTag) {
        // Update content if:
        // 1. Content is empty (initial fill), OR
        // 2. Tag has changed (user changed the tag)
        val tagChanged = previousTag != element.userInfoTag
        previousTag = element.userInfoTag
        
        if (personalInfo != null && element.userInfoTag != null && element.userInfoTag != UserInfoTag.NONE) {
            when (element) {
                is ResumeElement.TextElement -> {
                    if (element.content.isEmpty() || tagChanged) {
                        val content = when (element.userInfoTag) {
                            UserInfoTag.NAME -> personalInfo?.fullName ?: ""
                            UserInfoTag.EMAIL -> personalInfo?.email ?: ""
                            UserInfoTag.PHONE -> personalInfo?.phone ?: ""
                            UserInfoTag.LOCATION -> personalInfo?.location ?: ""
                            UserInfoTag.GITHUB -> personalInfo?.github ?: ""
                            UserInfoTag.LINKEDIN -> personalInfo?.linkedIn ?: ""
                            UserInfoTag.WEBSITE -> personalInfo?.portfolio ?: ""
                            else -> element.content
                        }
                        if (content.isNotEmpty()) {
                            onUpdateElement(element.copy(content = content))
                        }
                    }
                }
                is ResumeElement.ImageElement -> {
                    if ((element.imageUrl.isEmpty() || tagChanged) && element.userInfoTag == UserInfoTag.AVATAR) {
                        val avatar = personalInfo?.avatar ?: ""
                        if (avatar.isNotEmpty()) {
                            onUpdateElement(element.copy(imageUrl = avatar))
                        }
                    }
                }
                else -> { /* No auto-fill for other element types */ }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        Surface(
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Properties",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        Divider()

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Common properties (all elements)
            CommonPropertiesSection(
                element = element,
                onUpdateElement = onUpdateElement,
                personalInfo = personalInfo,
                resume = resume,
                onRefreshUserData = {
                    // Reload resume data
                    scope.launch {
                        val result = repository.getLatestResume()
                        result.onSuccess { formResume ->
                            resume = formResume
                            personalInfo = formResume?.personalInfo
                        }
                    }
                }
            )

            Divider()

            // Type-specific properties
            when (element) {
                is ResumeElement.TextElement -> {
                    TextElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                is ResumeElement.ImageElement -> {
                    ImageElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                is ResumeElement.ShapeElement -> {
                    ShapeElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                is ResumeElement.ChartElement -> {
                    ChartElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                is ResumeElement.ContactElement -> {
                    ContactElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                is ResumeElement.WorkExperienceElement -> {
                    WorkExperienceElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                is ResumeElement.EducationElement -> {
                    EducationElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                is ResumeElement.SkillElement -> {
                    SkillElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                is ResumeElement.ProjectElement -> {
                    ProjectElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                is ResumeElement.CertificationElement -> {
                    CertificationElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                is ResumeElement.LanguageElement -> {
                    LanguageElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                else -> {
                    Text("Properties not yet implemented for this element type")
                }
            }

            Divider()

            // Style properties
            StylePropertiesSection(
                element = element,
                onUpdateElement = onUpdateElement
            )

            Divider()

            // Remove element button
            var showDeleteConfirmation by remember { mutableStateOf(false) }

            Button(
                onClick = { showDeleteConfirmation = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Remove Element")
            }

            // Delete confirmation dialog
            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text("Remove Element?") },
                    text = { Text("Are you sure you want to remove this element? This action cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmation = false
                                onRemoveElement()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Remove")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Common properties section (position, size, z-index, lock)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommonPropertiesSection(
    element: ResumeElement,
    onUpdateElement: (ResumeElement) -> Unit,
    personalInfo: PersonalInfo?,
    resume: Resume?,
    onRefreshUserData: () -> Unit = {}
) {
    PropertySection(title = "Position & Size") {
        // Position
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberField(
                label = "Row",
                value = element.position.row,
                onValueChange = { newRow ->
                    val newPosition = element.position.copy(row = newRow)
                    onUpdateElement(updateElementPosition(element, newPosition))
                },
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "Col",
                value = element.position.col,
                onValueChange = { newCol ->
                    val newPosition = element.position.copy(col = newCol)
                    onUpdateElement(updateElementPosition(element, newPosition))
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Size Mode Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Wrap Width", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = element.position.widthMode == SizeMode.WRAP_CONTENT,
                onCheckedChange = { wrapWidth ->
                    val newMode = if (wrapWidth) SizeMode.WRAP_CONTENT else SizeMode.FIXED
                    val newPosition = element.position.copy(widthMode = newMode)
                    onUpdateElement(updateElementPosition(element, newPosition))
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Wrap Height", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = element.position.heightMode == SizeMode.WRAP_CONTENT,
                onCheckedChange = { wrapHeight ->
                    val newMode = if (wrapHeight) SizeMode.WRAP_CONTENT else SizeMode.FIXED
                    val newPosition = element.position.copy(heightMode = newMode)
                    onUpdateElement(updateElementPosition(element, newPosition))
                }
            )
        }

        // Size (only show when not wrap content)
        if (element.position.widthMode == SizeMode.FIXED || element.position.heightMode == SizeMode.FIXED) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (element.position.heightMode == SizeMode.FIXED) {
                    NumberField(
                        label = "Height",
                        value = element.position.rowSpan,
                        onValueChange = { newHeight ->
                            val newPosition = element.position.copy(rowSpan = newHeight.coerceAtLeast(1))
                            onUpdateElement(updateElementPosition(element, newPosition))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (element.position.widthMode == SizeMode.FIXED) {
                    NumberField(
                        label = "Width",
                        value = element.position.colSpan,
                        onValueChange = { newWidth ->
                            val newPosition = element.position.copy(colSpan = newWidth.coerceAtLeast(1))
                            onUpdateElement(updateElementPosition(element, newPosition))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Z-Index
        NumberField(
            label = "Layer (Z-Index)",
            value = element.zIndex,
            onValueChange = { newZIndex ->
                onUpdateElement(updateElementZIndex(element, newZIndex))
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Lock toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Lock Element")
            Switch(
                checked = element.locked,
                onCheckedChange = { locked ->
                    onUpdateElement(updateElementLocked(element, locked))
                }
            )
        }

        // Template Mode Tag (for TextElement, ImageElement, WorkExperienceElement, and EducationElement)
        if (element is ResumeElement.TextElement ||
            element is ResumeElement.ImageElement ||
            element is ResumeElement.WorkExperienceElement ||
            element is ResumeElement.EducationElement) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            var showTagMenu by remember { mutableStateOf(false) }

            Column {
                Text(
                    text = "Template Tag",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = showTagMenu,
                    onExpandedChange = { showTagMenu = it }
                ) {
                    OutlinedTextField(
                        value = element.userInfoTag?.name ?: "NONE",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("User Info Tag") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTagMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = showTagMenu,
                        onDismissRequest = { showTagMenu = false }
                    ) {
                        UserInfoTag.values().forEach { tag ->
                            // Filter tags based on element type
                            val isApplicable = when (element) {
                                is ResumeElement.ImageElement -> tag == UserInfoTag.AVATAR || tag == UserInfoTag.NONE
                                is ResumeElement.TextElement -> tag != UserInfoTag.AVATAR && tag != UserInfoTag.WORK_EXPERIENCE && tag != UserInfoTag.EDUCATION
                                is ResumeElement.WorkExperienceElement -> tag == UserInfoTag.WORK_EXPERIENCE || tag == UserInfoTag.NONE
                                is ResumeElement.EducationElement -> tag == UserInfoTag.EDUCATION || tag == UserInfoTag.NONE
                                else -> tag != UserInfoTag.AVATAR && tag != UserInfoTag.WORK_EXPERIENCE && tag != UserInfoTag.EDUCATION
                            }

                            if (isApplicable) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = tag.name,
                                            color = if (tag == element.userInfoTag)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        val newTag = if (tag == UserInfoTag.NONE) null else tag
                                        var updatedElement = updateElementTag(element, newTag)

                                        // Auto-apply user data if personalInfo is available and tag is not NONE
                                        if (newTag != null && personalInfo != null) {
                                            updatedElement = when (updatedElement) {
                                                is ResumeElement.TextElement -> {
                                                    val content = when (newTag) {
                                                        UserInfoTag.NAME -> personalInfo.fullName
                                                        UserInfoTag.EMAIL -> personalInfo.email
                                                        UserInfoTag.PHONE -> personalInfo.phone
                                                        UserInfoTag.LOCATION -> personalInfo.location
                                                        UserInfoTag.GITHUB -> personalInfo.github
                                                        UserInfoTag.LINKEDIN -> personalInfo.linkedIn
                                                        UserInfoTag.WEBSITE -> personalInfo.portfolio
                                                        UserInfoTag.AVATAR -> updatedElement.content
                                                        UserInfoTag.WORK_EXPERIENCE -> updatedElement.content
                                                        UserInfoTag.EDUCATION -> updatedElement.content
                                                        UserInfoTag.NONE -> updatedElement.content
                                                    }
                                                    updatedElement.copy(content = content)
                                                }
                                                is ResumeElement.ImageElement -> {
                                                    if (newTag == UserInfoTag.AVATAR && personalInfo.avatar.isNotEmpty()) {
                                                        updatedElement.copy(imageUrl = personalInfo.avatar)
                                                    } else {
                                                        updatedElement
                                                    }
                                                }
                                                is ResumeElement.WorkExperienceElement -> {
                                                    if (newTag == UserInfoTag.WORK_EXPERIENCE && resume != null && resume.workExperiences.isNotEmpty()) {
                                                        // Convert form WorkExperience to grid WorkExperienceItem
                                                        val workExperienceItems = resume.workExperiences.map { work ->
                                                            WorkExperienceItem(
                                                                jobTitle = work.jobTitle,
                                                                company = work.company,
                                                                location = work.location,
                                                                startDate = work.startDate?.format(
                                                                    java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
                                                                ) ?: "",
                                                                endDate = if (work.isCurrentRole) {
                                                                    "Present"
                                                                } else {
                                                                    work.endDate?.format(
                                                                        java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
                                                                    ) ?: ""
                                                                },
                                                                isCurrentRole = work.isCurrentRole,
                                                                responsibilities = work.responsibilities.map { resp ->
                                                                    ResponsibilityItem(text = resp)
                                                                }
                                                            )
                                                        }
                                                        updatedElement.copy(items = workExperienceItems)
                                                    } else {
                                                        updatedElement
                                                    }
                                                }
                                                is ResumeElement.EducationElement -> {
                                                    if (newTag == UserInfoTag.EDUCATION && resume != null && resume.education.isNotEmpty()) {
                                                        // Convert form Education to grid EducationItem
                                                        val educationItems = resume.education.map { edu ->
                                                            EducationItem(
                                                                degree = edu.degree,
                                                                institution = edu.institution,
                                                                location = edu.location,
                                                                startDate = edu.startDate?.format(
                                                                    java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
                                                                ) ?: "",
                                                                endDate = edu.endDate?.format(
                                                                    java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
                                                                ) ?: "",
                                                                gpa = edu.gpa,
                                                                achievements = edu.achievements.map { ach ->
                                                                    AchievementItem(text = ach)
                                                                }
                                                            )
                                                        }
                                                        updatedElement.copy(items = educationItems)
                                                    } else {
                                                        updatedElement
                                                    }
                                                }
                                                else -> updatedElement
                                            }
                                        }

                                        onUpdateElement(updatedElement)
                                        showTagMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (element.userInfoTag != null && element.userInfoTag != UserInfoTag.NONE) {
                    Text(
                        text = if (personalInfo != null) {
                            "Template tag applied - content auto-updated with user data"
                        } else {
                            "Template tag set - waiting for user data from Resume Builder"
                        },
                        fontSize = 12.sp,
                        color = if (personalInfo != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Add a refresh button to manually reload data from user profile
                    if (personalInfo != null) {
                        OutlinedButton(
                            onClick = {
                                // Trigger refresh first, then apply data
                                onRefreshUserData()
                                
                                // Apply tag data immediately with current data (will update once refresh completes)
                                val updatedElement = when (element) {
                                    is ResumeElement.TextElement -> {
                                        val content = when (element.userInfoTag) {
                                            UserInfoTag.NAME -> personalInfo.fullName
                                            UserInfoTag.EMAIL -> personalInfo.email
                                            UserInfoTag.PHONE -> personalInfo.phone
                                            UserInfoTag.LOCATION -> personalInfo.location
                                            UserInfoTag.GITHUB -> personalInfo.github
                                            UserInfoTag.LINKEDIN -> personalInfo.linkedIn
                                            UserInfoTag.WEBSITE -> personalInfo.portfolio
                                            else -> element.content
                                        }
                                        element.copy(content = content)
                                    }
                                    is ResumeElement.ImageElement -> {
                                        if (element.userInfoTag == UserInfoTag.AVATAR && personalInfo.avatar.isNotEmpty()) {
                                            element.copy(imageUrl = personalInfo.avatar)
                                        } else {
                                            element
                                        }
                                    }
                                    is ResumeElement.WorkExperienceElement -> {
                                        if (element.userInfoTag == UserInfoTag.WORK_EXPERIENCE && resume != null && resume.workExperiences.isNotEmpty()) {
                                            val workExperienceItems = resume.workExperiences.map { work ->
                                                WorkExperienceItem(
                                                    jobTitle = work.jobTitle,
                                                    company = work.company,
                                                    location = work.location,
                                                    startDate = work.startDate?.format(
                                                        java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
                                                    ) ?: "",
                                                    endDate = if (work.isCurrentRole) {
                                                        "Present"
                                                    } else {
                                                        work.endDate?.format(
                                                            java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
                                                        ) ?: ""
                                                    },
                                                    isCurrentRole = work.isCurrentRole,
                                                    responsibilities = work.responsibilities.map { resp ->
                                                        ResponsibilityItem(text = resp)
                                                    }
                                                )
                                            }
                                            element.copy(items = workExperienceItems)
                                        } else {
                                            element
                                        }
                                    }
                                    is ResumeElement.EducationElement -> {
                                        if (element.userInfoTag == UserInfoTag.EDUCATION && resume != null && resume.education.isNotEmpty()) {
                                            val educationItems = resume.education.map { edu ->
                                                EducationItem(
                                                    degree = edu.degree,
                                                    institution = edu.institution,
                                                    location = edu.location,
                                                    startDate = edu.startDate?.format(
                                                        java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
                                                    ) ?: "",
                                                    endDate = edu.endDate?.format(
                                                        java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
                                                    ) ?: "",
                                                    gpa = edu.gpa,
                                                    achievements = edu.achievements.map { ach ->
                                                        AchievementItem(text = ach)
                                                    }
                                                )
                                            }
                                            element.copy(items = educationItems)
                                        } else {
                                            element
                                        }
                                    }
                                    else -> element
                                }
                                onUpdateElement(updatedElement)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Refresh from Resume Builder", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Text element specific properties
 */
@Composable
private fun TextElementProperties(
    element: ResumeElement.TextElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Text Content") {
        // Content
        OutlinedTextField(
            value = element.content,
            onValueChange = { newContent ->
                onUpdateElement(element.copy(content = newContent))
            },
            label = { Text("Content") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 8
        )

        // Font size
        SliderField(
            label = "Font Size: ${element.textStyle.fontSize.toInt()}sp",
            value = element.textStyle.fontSize,
            valueRange = 8f..72f,
            onValueChange = { newSize ->
                onUpdateElement(
                    element.copy(
                        textStyle = element.textStyle.copy(fontSize = newSize)
                    )
                )
            }
        )

        // Font weight
        var showFontWeightMenu by remember { mutableStateOf(false) }
        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = showFontWeightMenu,
            onExpandedChange = { showFontWeightMenu = it }
        ) {
            OutlinedTextField(
                value = element.textStyle.fontWeight.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Font Weight") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFontWeightMenu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = showFontWeightMenu,
                onDismissRequest = { showFontWeightMenu = false }
            ) {
                listOf(
                    FontWeight.Thin to "Thin",
                    FontWeight.Light to "Light",
                    FontWeight.Normal to "Normal",
                    FontWeight.Medium to "Medium",
                    FontWeight.SemiBold to "SemiBold",
                    FontWeight.Bold to "Bold",
                    FontWeight.ExtraBold to "ExtraBold"
                ).forEach { (weight, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onUpdateElement(
                                element.copy(
                                    textStyle = element.textStyle.copy(fontWeight = weight)
                                )
                            )
                            showFontWeightMenu = false
                        }
                    )
                }
            }
        }

        // Horizontal text alignment
        Text("Horizontal Alignment", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextAlignment.entries.forEach { alignment ->
                FilterChip(
                    selected = element.alignment == alignment,
                    onClick = {
                        onUpdateElement(element.copy(alignment = alignment))
                    },
                    label = { Text(alignment.name) }
                )
            }
        }

        // Vertical text alignment
        Text("Vertical Alignment", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VerticalTextAlignment.entries.forEach { vAlignment ->
                FilterChip(
                    selected = (element.verticalAlignment ?: VerticalTextAlignment.CENTER) == vAlignment,
                    onClick = {
                        onUpdateElement(element.copy(verticalAlignment = vAlignment))
                    },
                    label = { Text(vAlignment.name) }
                )
            }
        }

        // Text color (simplified color picker)
        ColorPicker(
            label = "Text Color",
            color = Color(element.textStyle.color),
            onColorChange = { newColor ->
                newColor?.let {
                    val colorLong = android.graphics.Color.argb(
                        (it.alpha * 255).toInt(),
                        (it.red * 255).toInt(),
                        (it.green * 255).toInt(),
                        (it.blue * 255).toInt()
                    ).toLong()
                    onUpdateElement(
                        element.copy(
                            textStyle = element.textStyle.copy(color = colorLong)
                        )
                    )
                }
            }
        )
    }
}

/**
 * Image element properties
 */
@Composable
private fun ImageElementProperties(
    element: ResumeElement.ImageElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            // Grant persistent URI permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Permission already granted or not needed
            }

            // Update element with selected image URI
            onUpdateElement(element.copy(imageUrl = it.toString()))
        }
    }

    PropertySection(title = "Image") {
        // Image preview
        if (element.imageUrl.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.LightGray, MaterialTheme.shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                // Try to load and display the image
                val uri = try {
                    Uri.parse(element.imageUrl)
                } catch (e: Exception) {
                    null
                }

                if (uri != null) {
                    androidx.compose.foundation.Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("Invalid image", color = Color.Gray)
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = element.imageUrl,
            onValueChange = { newUrl ->
                onUpdateElement(element.copy(imageUrl = newUrl))
            },
            label = { Text("Image URL or Path") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choose Image")
        }

        // Circle crop toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Circle Crop")
            Switch(
                checked = element.isCircle,
                onCheckedChange = { isCircle ->
                    onUpdateElement(element.copy(isCircle = isCircle))
                }
            )
        }

        // Corner radius (only show if not circle)
        if (!element.isCircle) {
            SliderField(
                label = "Corner Radius: ${element.cornerRadius.toInt()}dp",
                value = element.cornerRadius,
                valueRange = 0f..50f,
                onValueChange = { newRadius ->
                    onUpdateElement(element.copy(cornerRadius = newRadius))
                }
            )
        }

        // Content scale
        var showScaleMenu by remember { mutableStateOf(false) }
        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = showScaleMenu,
            onExpandedChange = { showScaleMenu = it }
        ) {
            OutlinedTextField(
                value = element.contentScale.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Content Scale") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showScaleMenu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = showScaleMenu,
                onDismissRequest = { showScaleMenu = false }
            ) {
                ImageScale.values().forEach { scale ->
                    DropdownMenuItem(
                        text = { Text(scale.name) },
                        onClick = {
                            onUpdateElement(element.copy(contentScale = scale))
                            showScaleMenu = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Shape element properties
 */
@Composable
private fun ShapeElementProperties(
    element: ResumeElement.ShapeElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Shape") {
        // Shape type dropdown
        var showShapeMenu by remember { mutableStateOf(false) }
        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = showShapeMenu,
            onExpandedChange = { showShapeMenu = it }
        ) {
            OutlinedTextField(
                value = element.shapeType.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Shape Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showShapeMenu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = showShapeMenu,
                onDismissRequest = { showShapeMenu = false }
            ) {
                ShapeType.values().forEach { shapeType ->
                    DropdownMenuItem(
                        text = { Text(shapeType.name) },
                        onClick = {
                            onUpdateElement(element.copy(shapeType = shapeType))
                            showShapeMenu = false
                        }
                    )
                }
            }
        }

        // Corner radius
        SliderField(
            label = "Corner Radius: ${element.cornerRadius.toInt()}dp",
            value = element.cornerRadius,
            valueRange = 0f..32f,
            onValueChange = { newRadius ->
                onUpdateElement(element.copy(cornerRadius = newRadius))
            }
        )

        // Custom height (particularly useful for dividers)
        if (element.shapeType == ShapeType.DIVIDER || element.customHeightDp != null) {
            SliderField(
                label = "Custom Height: ${element.customHeightDp?.toInt() ?: 2}dp",
                value = element.customHeightDp ?: 2f,
                valueRange = 1f..24f,
                onValueChange = { newHeight ->
                    onUpdateElement(element.copy(customHeightDp = newHeight))
                }
            )
        }

        // Custom width (optional, for vertical dividers)
        if (element.shapeType == ShapeType.LINE || element.customWidthDp != null) {
            SliderField(
                label = "Custom Width: ${element.customWidthDp?.toInt() ?: 2}dp",
                value = element.customWidthDp ?: 2f,
                valueRange = 1f..24f,
                onValueChange = { newWidth ->
                    onUpdateElement(element.copy(customWidthDp = newWidth))
                }
            )
        }
    }
}

/**
 * Chart element properties (placeholder)
 */
@Composable
private fun ChartElementProperties(
    element: ResumeElement.ChartElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Chart") {
        Text("Chart properties coming soon...")
    }
}

/**
 * Contact element properties
 */
@Composable
private fun ContactElementProperties(
    element: ResumeElement.ContactElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ResumeRepository.getInstance(context) }
    var personalInfo by remember { mutableStateOf<PersonalInfo?>(null) }

    // Load personal info when panel opens
    LaunchedEffect(Unit) {
        scope.launch {
            val result = repository.getLatestResume()
            result.onSuccess { formResume ->
                personalInfo = formResume?.personalInfo
            }
        }
    }

    PropertySection(title = "Contact Items") {
        // Contact items list
        element.items.forEachIndexed { index, item ->
            ContactItemEditor(
                item = item,
                personalInfo = personalInfo,
                onUpdate = { updatedItem ->
                    val updatedItems = element.items.toMutableList()
                    updatedItems[index] = updatedItem
                    onUpdateElement(element.copy(items = updatedItems))
                },
                onRemove = {
                    val updatedItems = element.items.toMutableList()
                    updatedItems.removeAt(index)
                    onUpdateElement(element.copy(items = updatedItems))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add contact item button
        Button(
            onClick = {
                val newItem = ContactItem(
                    type = ContactType.PHONE,
                    label = "Phone:",
                    iconName = "phone"
                )
                onUpdateElement(element.copy(items = element.items + newItem))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Add Contact Item")
        }
    }

    PropertySection(title = "Display Settings") {
        // Icon style toggle
        Text("Icon Style", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ContactIconStyle.entries.forEach { style ->
                FilterChip(
                    selected = element.iconStyle == style,
                    onClick = {
                        onUpdateElement(element.copy(iconStyle = style))
                    },
                    label = { Text(style.name.replace("_", " ")) }
                )
            }
        }

        // Orientation toggle
        Text("Orientation", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ContactOrientation.entries.forEach { orientation ->
                FilterChip(
                    selected = element.orientation == orientation,
                    onClick = {
                        onUpdateElement(element.copy(orientation = orientation))
                    },
                    label = { Text(orientation.name) }
                )
            }
        }

        // Horizontal alignment
        Text("Horizontal Alignment", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HorizontalAlignment.entries.forEach { alignment ->
                FilterChip(
                    selected = (element.horizontalAlignment ?: HorizontalAlignment.START) == alignment,
                    onClick = {
                        onUpdateElement(element.copy(horizontalAlignment = alignment))
                    },
                    label = { Text(alignment.name) }
                )
            }
        }

        // Vertical alignment
        Text("Vertical Alignment", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VerticalAlignment.entries.forEach { alignment ->
                FilterChip(
                    selected = (element.verticalAlignment ?: VerticalAlignment.CENTER) == alignment,
                    onClick = {
                        onUpdateElement(element.copy(verticalAlignment = alignment))
                    },
                    label = { Text(alignment.name) }
                )
            }
        }

        // Spacing slider
        SliderField(
            label = "Item Spacing: ${element.spacing.toInt()}dp",
            value = element.spacing,
            valueRange = 0f..32f,
            onValueChange = { newSpacing ->
                onUpdateElement(element.copy(spacing = newSpacing))
            }
        )

        // Icon size slider (only show if using icons)
        if (element.iconStyle == ContactIconStyle.ICON) {
            SliderField(
                label = "Icon Size: ${element.iconSize.toInt()}dp",
                value = element.iconSize,
                valueRange = 8f..48f,
                onValueChange = { newSize ->
                    onUpdateElement(element.copy(iconSize = newSize))
                }
            )
        }
    }

    PropertySection(title = "Text Style") {
        // Font size
        SliderField(
            label = "Font Size: ${element.textStyle.fontSize.toInt()}sp",
            value = element.textStyle.fontSize,
            valueRange = 8f..48f,
            onValueChange = { newSize ->
                onUpdateElement(
                    element.copy(
                        textStyle = element.textStyle.copy(fontSize = newSize)
                    )
                )
            }
        )

        // Font weight
        var showFontWeightMenu by remember { mutableStateOf(false) }
        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = showFontWeightMenu,
            onExpandedChange = { showFontWeightMenu = it }
        ) {
            OutlinedTextField(
                value = element.textStyle.fontWeight.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Font Weight") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFontWeightMenu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = showFontWeightMenu,
                onDismissRequest = { showFontWeightMenu = false }
            ) {
                listOf(
                    FontWeight.Thin to "Thin",
                    FontWeight.Light to "Light",
                    FontWeight.Normal to "Normal",
                    FontWeight.Medium to "Medium",
                    FontWeight.SemiBold to "SemiBold",
                    FontWeight.Bold to "Bold",
                    FontWeight.ExtraBold to "ExtraBold"
                ).forEach { (weight, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onUpdateElement(
                                element.copy(
                                    textStyle = element.textStyle.copy(fontWeight = weight)
                                )
                            )
                            showFontWeightMenu = false
                        }
                    )
                }
            }
        }

        // Text color
        ColorPicker(
            label = "Text Color",
            color = Color(element.textStyle.color),
            onColorChange = { newColor ->
                newColor?.let {
                    val colorLong = android.graphics.Color.argb(
                        (it.alpha * 255).toInt(),
                        (it.red * 255).toInt(),
                        (it.green * 255).toInt(),
                        (it.blue * 255).toInt()
                    ).toLong()
                    onUpdateElement(
                        element.copy(
                            textStyle = element.textStyle.copy(color = colorLong)
                        )
                    )
                }
            }
        )
    }
}

/**
 * Editor for a single contact item
 */
@Composable
private fun ContactItemEditor(
    item: ContactItem,
    personalInfo: PersonalInfo?,
    onUpdate: (ContactItem) -> Unit,
    onRemove: () -> Unit
) {
    // Track previous tag to detect changes
    var previousTag by remember { mutableStateOf(item.userInfoTag) }
    
    // Auto-update value when personalInfo loads and item has a tag
    LaunchedEffect(personalInfo, item.userInfoTag) {
        val tagChanged = previousTag != item.userInfoTag
        previousTag = item.userInfoTag
        
        if (personalInfo != null && item.userInfoTag != null && item.userInfoTag != UserInfoTag.NONE) {
            // Update value if:
            // 1. Value is empty (initial fill), OR
            // 2. Tag has changed (user changed the tag)
            if (item.value.isEmpty() || tagChanged) {
                val value = when (item.userInfoTag) {
                    UserInfoTag.NAME -> personalInfo.fullName
                    UserInfoTag.EMAIL -> personalInfo.email
                    UserInfoTag.PHONE -> personalInfo.phone
                    UserInfoTag.LOCATION -> personalInfo.location
                    UserInfoTag.GITHUB -> personalInfo.github
                    UserInfoTag.LINKEDIN -> personalInfo.linkedIn
                    UserInfoTag.WEBSITE -> personalInfo.portfolio
                    else -> item.value
                }
                if (value.isNotEmpty()) {
                    onUpdate(item.copy(value = value))
                }
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.type.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contact type dropdown
            var showTypeMenu by remember { mutableStateOf(false) }
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = showTypeMenu,
                onExpandedChange = { showTypeMenu = it }
            ) {
                OutlinedTextField(
                    value = item.type.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showTypeMenu,
                    onDismissRequest = { showTypeMenu = false }
                ) {
                    ContactType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                onUpdate(item.copy(type = type))
                                showTypeMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Value field
            OutlinedTextField(
                value = item.value,
                onValueChange = { onUpdate(item.copy(value = it)) },
                label = { Text("Value") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Label field
            OutlinedTextField(
                value = item.label,
                onValueChange = { onUpdate(item.copy(label = it)) },
                label = { Text("Label (for BOLD_LABEL style)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Template tag dropdown (optional)
            var showTagMenu by remember { mutableStateOf(false) }
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = showTagMenu,
                onExpandedChange = { showTagMenu = it }
            ) {
                OutlinedTextField(
                    value = item.userInfoTag?.name ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Template Tag (optional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTagMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showTagMenu,
                    onDismissRequest = { showTagMenu = false }
                ) {
                    listOf(null).plus(UserInfoTag.entries).forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag?.name ?: "None") },
                            onClick = {
                                var updatedItem = item.copy(userInfoTag = tag)
                                
                                // Auto-apply user data if personalInfo is available and tag is not None
                                if (tag != null && tag != UserInfoTag.NONE && personalInfo != null) {
                                    val value = when (tag) {
                                        UserInfoTag.NAME -> personalInfo.fullName
                                        UserInfoTag.EMAIL -> personalInfo.email
                                        UserInfoTag.PHONE -> personalInfo.phone
                                        UserInfoTag.LOCATION -> personalInfo.location
                                        UserInfoTag.GITHUB -> personalInfo.github
                                        UserInfoTag.LINKEDIN -> personalInfo.linkedIn
                                        UserInfoTag.WEBSITE -> personalInfo.portfolio
                                        else -> updatedItem.value
                                    }
                                    updatedItem = updatedItem.copy(value = value)
                                }
                                
                                onUpdate(updatedItem)
                                showTagMenu = false
                            }
                        )
                    }
                }
            }
            
            // Show status message if tag is set
            if (item.userInfoTag != null && item.userInfoTag != UserInfoTag.NONE) {
                Text(
                    text = if (personalInfo != null) {
                        "✓ Auto-updated with user data"
                    } else {
                        "⏳ Waiting for user data..."
                    },
                    fontSize = 11.sp,
                    color = if (personalInfo != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Work Experience element properties
 */
@Composable
private fun WorkExperienceElementProperties(
    element: ResumeElement.WorkExperienceElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Work Experience Items") {
        // Work experience items list
        element.items.forEachIndexed { index, item ->
            WorkExperienceItemEditor(
                item = item,
                onUpdate = { updatedItem ->
                    val updatedItems = element.items.toMutableList()
                    updatedItems[index] = updatedItem
                    onUpdateElement(element.copy(items = updatedItems))
                },
                onRemove = {
                    val updatedItems = element.items.toMutableList()
                    updatedItems.removeAt(index)
                    onUpdateElement(element.copy(items = updatedItems))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add work experience item button
        Button(
            onClick = {
                val newItem = WorkExperienceItem(
                    jobTitle = "Job Title",
                    company = "Company Name",
                    location = "Location",
                    startDate = "",
                    endDate = "",
                    isCurrentRole = false,
                    responsibilities = emptyList()
                )
                onUpdateElement(element.copy(items = element.items + newItem))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Add Work Experience")
        }
    }

    PropertySection(title = "Display Settings") {
        // Display style
        Text("Display Style", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WorkExperienceDisplayStyle.entries.forEach { style ->
                FilterChip(
                    selected = element.displayStyle == style,
                    onClick = {
                        onUpdateElement(element.copy(displayStyle = style))
                    },
                    label = { Text(style.name.replace("_", " ")) }
                )
            }
        }

        // Orientation toggle
        Text("Orientation", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WorkExperienceOrientation.entries.forEach { orientation ->
                FilterChip(
                    selected = element.orientation == orientation,
                    onClick = {
                        onUpdateElement(element.copy(orientation = orientation))
                    },
                    label = { Text(orientation.name) }
                )
            }
        }

        // Show/Hide options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Location", fontSize = 12.sp)
            Switch(
                checked = element.showLocation,
                onCheckedChange = { onUpdateElement(element.copy(showLocation = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Dates", fontSize = 12.sp)
            Switch(
                checked = element.showDates,
                onCheckedChange = { onUpdateElement(element.copy(showDates = it)) }
            )
        }

        // Horizontal alignment
        Text("Horizontal Alignment", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HorizontalAlignment.entries.forEach { alignment ->
                FilterChip(
                    selected = (element.horizontalAlignment ?: HorizontalAlignment.START) == alignment,
                    onClick = {
                        onUpdateElement(element.copy(horizontalAlignment = alignment))
                    },
                    label = { Text(alignment.name) }
                )
            }
        }

        // Vertical alignment
        Text("Vertical Alignment", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VerticalAlignment.entries.forEach { alignment ->
                FilterChip(
                    selected = (element.verticalAlignment ?: VerticalAlignment.TOP) == alignment,
                    onClick = {
                        onUpdateElement(element.copy(verticalAlignment = alignment))
                    },
                    label = { Text(alignment.name) }
                )
            }
        }

        // Spacing sliders
        SliderField(
            label = "Entry Spacing: ${element.spacing.toInt()}dp",
            value = element.spacing,
            valueRange = 0f..32f,
            onValueChange = { newSpacing ->
                onUpdateElement(element.copy(spacing = newSpacing))
            }
        )

        SliderField(
            label = "Item Spacing: ${element.itemSpacing.toInt()}dp",
            value = element.itemSpacing,
            valueRange = 0f..16f,
            onValueChange = { newSpacing ->
                onUpdateElement(element.copy(itemSpacing = newSpacing))
            }
        )

        SliderField(
            label = "Responsibility Spacing: ${element.responsibilitySpacing.toInt()}dp",
            value = element.responsibilitySpacing,
            valueRange = 0f..12f,
            onValueChange = { newSpacing ->
                onUpdateElement(element.copy(responsibilitySpacing = newSpacing))
            }
        )
    }

    PropertySection(title = "Date & Bullet Settings") {
        // Date format
        Text("Date Format", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DateFormat.entries.forEach { format ->
                FilterChip(
                    selected = element.dateFormat == format,
                    onClick = {
                        onUpdateElement(element.copy(dateFormat = format))
                    },
                    label = { Text(format.name.replace("_", " ")) }
                )
            }
        }

        // Date separator
        OutlinedTextField(
            value = element.dateSeparator,
            onValueChange = { onUpdateElement(element.copy(dateSeparator = it)) },
            label = { Text("Date Separator") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Bullet style
        Text("Bullet Style", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BulletStyle.entries.forEach { style ->
                FilterChip(
                    selected = element.bulletStyle == style,
                    onClick = {
                        onUpdateElement(element.copy(bulletStyle = style))
                    },
                    label = { Text(style.name.replace("_", " ")) }
                )
            }
        }
    }

    // Text styles - collapsible sections to save space
    var showTitleStyle by remember { mutableStateOf(false) }
    var showCompanyStyle by remember { mutableStateOf(false) }
    var showDateStyle by remember { mutableStateOf(false) }
    var showLocationStyle by remember { mutableStateOf(false) }
    var showResponsibilityStyle by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = { showTitleStyle = !showTitleStyle }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Title Style", fontWeight = FontWeight.Medium)
            Icon(
                if (showTitleStyle) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
        if (showTitleStyle) {
            Column(modifier = Modifier.padding(12.dp)) {
                TextStyleControls(
                    textStyle = element.titleStyle,
                    onTextStyleChange = { onUpdateElement(element.copy(titleStyle = it)) }
                )
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = { showCompanyStyle = !showCompanyStyle }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Company Style", fontWeight = FontWeight.Medium)
            Icon(
                if (showCompanyStyle) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
        if (showCompanyStyle) {
            Column(modifier = Modifier.padding(12.dp)) {
                TextStyleControls(
                    textStyle = element.companyStyle,
                    onTextStyleChange = { onUpdateElement(element.copy(companyStyle = it)) }
                )
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = { showDateStyle = !showDateStyle }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Date Style", fontWeight = FontWeight.Medium)
            Icon(
                if (showDateStyle) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
        if (showDateStyle) {
            Column(modifier = Modifier.padding(12.dp)) {
                TextStyleControls(
                    textStyle = element.dateStyle,
                    onTextStyleChange = { onUpdateElement(element.copy(dateStyle = it)) }
                )
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = { showLocationStyle = !showLocationStyle }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Location Style", fontWeight = FontWeight.Medium)
            Icon(
                if (showLocationStyle) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
        if (showLocationStyle) {
            Column(modifier = Modifier.padding(12.dp)) {
                TextStyleControls(
                    textStyle = element.locationStyle,
                    onTextStyleChange = { onUpdateElement(element.copy(locationStyle = it)) }
                )
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = { showResponsibilityStyle = !showResponsibilityStyle }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Responsibility Style", fontWeight = FontWeight.Medium)
            Icon(
                if (showResponsibilityStyle) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
        if (showResponsibilityStyle) {
            Column(modifier = Modifier.padding(12.dp)) {
                TextStyleControls(
                    textStyle = element.responsibilityStyle,
                    onTextStyleChange = { onUpdateElement(element.copy(responsibilityStyle = it)) }
                )
            }
        }
    }
}

/**
 * Editor for a single work experience item
 */
@Composable
private fun WorkExperienceItemEditor(
    item: WorkExperienceItem,
    onUpdate: (WorkExperienceItem) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (item.jobTitle.isNotEmpty()) item.jobTitle else "Work Experience",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Job Title
            OutlinedTextField(
                value = item.jobTitle,
                onValueChange = { onUpdate(item.copy(jobTitle = it)) },
                label = { Text("Job Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Company
            OutlinedTextField(
                value = item.company,
                onValueChange = { onUpdate(item.copy(company = it)) },
                label = { Text("Company") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Location
            OutlinedTextField(
                value = item.location,
                onValueChange = { onUpdate(item.copy(location = it)) },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Start Date
            OutlinedTextField(
                value = item.startDate,
                onValueChange = { onUpdate(item.copy(startDate = it)) },
                label = { Text("Start Date (yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("2020-01-15") }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Current Role Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Current Role", fontSize = 12.sp)
                Checkbox(
                    checked = item.isCurrentRole,
                    onCheckedChange = { onUpdate(item.copy(isCurrentRole = it)) }
                )
            }

            // End Date (only show if not current role)
            if (!item.isCurrentRole) {
                OutlinedTextField(
                    value = item.endDate,
                    onValueChange = { onUpdate(item.copy(endDate = it)) },
                    label = { Text("End Date (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("2022-06-30") }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Responsibilities
            Text("Responsibilities", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))

            item.responsibilities.forEachIndexed { index, responsibility ->
                ResponsibilityItemEditor(
                    responsibility = responsibility,
                    jobTitle = item.jobTitle,
                    company = item.company,
                    otherResponsibilities = item.responsibilities
                        .filterIndexed { i, _ -> i != index }
                        .map { it.text },
                    onUpdate = { updated ->
                        val updatedResponsibilities = item.responsibilities.toMutableList()
                        updatedResponsibilities[index] = updated
                        onUpdate(item.copy(responsibilities = updatedResponsibilities))
                    },
                    onRemove = {
                        val updatedResponsibilities = item.responsibilities.toMutableList()
                        updatedResponsibilities.removeAt(index)
                        onUpdate(item.copy(responsibilities = updatedResponsibilities))
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Add Responsibility Button
            Button(
                onClick = {
                    val newResponsibility = ResponsibilityItem(text = "")
                    onUpdate(item.copy(responsibilities = item.responsibilities + newResponsibility))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Responsibility", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Editor for a single responsibility item
 */
@Composable
private fun ResponsibilityItemEditor(
    responsibility: ResponsibilityItem,
    jobTitle: String = "",
    company: String = "",
    otherResponsibilities: List<String> = emptyList(),
    onUpdate: (ResponsibilityItem) -> Unit,
    onRemove: () -> Unit
) {
    var showAIDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = responsibility.text,
                onValueChange = { onUpdate(responsibility.copy(text = it)) },
                modifier = Modifier.weight(1f),
                singleLine = false,
                maxLines = 3,
                placeholder = { Text("Responsibility description", fontSize = 12.sp) }
            )

            // Improve with AI button
            IconButton(
                onClick = { showAIDialog = true },
                modifier = Modifier.size(32.dp),
                enabled = responsibility.text.isNotBlank() && jobTitle.isNotBlank() && company.isNotBlank()
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Improve with AI",
                    modifier = Modifier.size(18.dp),
                    tint = if (responsibility.text.isNotBlank() && jobTitle.isNotBlank() && company.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(18.dp))
            }
        }
    }

    // AI Improvement Dialog
    if (showAIDialog) {
        ResponsibilityAIDialog(
            currentText = responsibility.text,
            jobTitle = jobTitle,
            company = company,
            otherResponsibilities = otherResponsibilities,
            onDismiss = { showAIDialog = false },
            onSelectSuggestion = { improvedText ->
                onUpdate(responsibility.copy(text = improvedText))
            }
        )
    }
}

/**
 * Reusable text style controls
 */
@Composable
private fun TextStyleControls(
    textStyle: TextStyle,
    onTextStyleChange: (TextStyle) -> Unit
) {
    // Font size
    SliderField(
        label = "Font Size: ${textStyle.fontSize.toInt()}sp",
        value = textStyle.fontSize,
        valueRange = 8f..48f,
        onValueChange = { newSize ->
            onTextStyleChange(textStyle.copy(fontSize = newSize))
        }
    )

    // Font weight
    var showFontWeightMenu by remember { mutableStateOf(false) }
    @OptIn(ExperimentalMaterial3Api::class)
    ExposedDropdownMenuBox(
        expanded = showFontWeightMenu,
        onExpandedChange = { showFontWeightMenu = it }
    ) {
        OutlinedTextField(
            value = textStyle.fontWeight.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Font Weight") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFontWeightMenu) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = showFontWeightMenu,
            onDismissRequest = { showFontWeightMenu = false }
        ) {
            listOf(
                FontWeight.Thin to "Thin",
                FontWeight.Light to "Light",
                FontWeight.Normal to "Normal",
                FontWeight.Medium to "Medium",
                FontWeight.SemiBold to "SemiBold",
                FontWeight.Bold to "Bold",
                FontWeight.ExtraBold to "ExtraBold"
            ).forEach { (weight, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onTextStyleChange(textStyle.copy(fontWeight = weight))
                        showFontWeightMenu = false
                    }
                )
            }
        }
    }

    // Text color
    ColorPicker(
        label = "Text Color",
        color = Color(textStyle.color),
        onColorChange = { newColor ->
            newColor?.let {
                val colorLong = android.graphics.Color.argb(
                    (it.alpha * 255).toInt(),
                    (it.red * 255).toInt(),
                    (it.green * 255).toInt(),
                    (it.blue * 255).toInt()
                ).toLong()
                onTextStyleChange(textStyle.copy(color = colorLong))
            }
        }
    )
}

/**
 * Style properties section (border, background, shadow)
 */
@Composable
private fun StylePropertiesSection(
    element: ResumeElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Style") {
        // Background color
        ColorPicker(
            label = "Background",
            color = element.style.backgroundColor?.let { Color(it) },
            onColorChange = { newColor ->
                val colorLong = newColor?.let {
                    android.graphics.Color.argb(
                        (it.alpha * 255).toInt(),
                        (it.red * 255).toInt(),
                        (it.green * 255).toInt(),
                        (it.blue * 255).toInt()
                    ).toLong()
                }
                onUpdateElement(
                    updateElementStyle(
                        element,
                        element.style.copy(backgroundColor = colorLong)
                    )
                )
            },
            nullable = true
        )

        // Border
        ColorPicker(
            label = "Border Color",
            color = element.style.borderColor?.let { Color(it) },
            onColorChange = { newColor ->
                val colorLong = newColor?.let {
                    android.graphics.Color.argb(
                        (it.alpha * 255).toInt(),
                        (it.red * 255).toInt(),
                        (it.green * 255).toInt(),
                        (it.blue * 255).toInt()
                    ).toLong()
                }
                onUpdateElement(
                    updateElementStyle(
                        element,
                        element.style.copy(borderColor = colorLong)
                    )
                )
            },
            nullable = true
        )

        if (element.style.borderColor != null) {
            SliderField(
                label = "Border Width: ${element.style.borderWidth.toInt()}dp",
                value = element.style.borderWidth,
                valueRange = 0f..8f,
                onValueChange = { newWidth ->
                    onUpdateElement(
                        updateElementStyle(
                            element,
                            element.style.copy(borderWidth = newWidth)
                        )
                    )
                }
            )
        }
    }
}

// ============================================================================
// Helper Components
// ============================================================================

@Composable
private fun PropertySection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { newValue ->
            newValue.toIntOrNull()?.let(onValueChange)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun SliderField(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun ColorPicker(
    label: String,
    color: Color?,
    onColorChange: (Color?) -> Unit,
    nullable: Boolean = false
) {
    var showColorPickerDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color preview box - click to open dialog
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color ?: Color.Transparent, MaterialTheme.shapes.small)
                        .border(1.dp, Color.Gray, MaterialTheme.shapes.small)
                        .clickable {
                            showColorPickerDialog = true
                        }
                )

                if (nullable && color != null) {
                    IconButton(
                        onClick = { onColorChange(null) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear color",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Quick color presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Color.White,
                Color.Black,
                Color.Gray,
                Color.Red,
                Color.Blue,
                Color.Green,
                Color.Yellow
            ).forEach { presetColor ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(presetColor, MaterialTheme.shapes.small)
                        .border(
                            width = if (color == presetColor) 2.dp else 1.dp,
                            color = if (color == presetColor) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { onColorChange(presetColor) }
                )
            }
        }

        // Button to open advanced color picker
        OutlinedButton(
            onClick = { showColorPickerDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Palette, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Advanced Picker")
        }
    }

    // Show color picker dialog
    if (showColorPickerDialog) {
        ColorPickerDialog(
            initialColor = color ?: Color.Black,
            onDismiss = { showColorPickerDialog = false },
            onColorSelected = { newColor ->
                onColorChange(newColor)
            }
        )
    }
}

/**
 * Education element properties
 */
@Composable
private fun EducationElementProperties(
    element: ResumeElement.EducationElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Education Items") {
        // Education items list
        element.items.forEachIndexed { index, item ->
            EducationItemEditor(
                item = item,
                onUpdate = { updatedItem ->
                    val updatedItems = element.items.toMutableList()
                    updatedItems[index] = updatedItem
                    onUpdateElement(element.copy(items = updatedItems))
                },
                onRemove = {
                    val updatedItems = element.items.toMutableList()
                    updatedItems.removeAt(index)
                    onUpdateElement(element.copy(items = updatedItems))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add education item button
        Button(
            onClick = {
                val newItem = EducationItem(
                    degree = "Degree Name",
                    institution = "University Name",
                    location = "Location",
                    startDate = "",
                    endDate = "",
                    gpa = "",
                    achievements = emptyList()
                )
                onUpdateElement(element.copy(items = element.items + newItem))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Add Education")
        }
    }

    PropertySection(title = "Display Settings") {
        // Display style
        Text("Display Style", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EducationDisplayStyle.entries.forEach { style ->
                FilterChip(
                    selected = element.displayStyle == style,
                    onClick = {
                        onUpdateElement(element.copy(displayStyle = style))
                    },
                    label = { Text(style.name.replace("_", " ")) }
                )
            }
        }

        // Orientation toggle
        Text("Orientation", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EducationOrientation.entries.forEach { orientation ->
                FilterChip(
                    selected = element.orientation == orientation,
                    onClick = {
                        onUpdateElement(element.copy(orientation = orientation))
                    },
                    label = { Text(orientation.name) }
                )
            }
        }

        // Show/Hide options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Location", fontSize = 12.sp)
            Switch(
                checked = element.showLocation,
                onCheckedChange = { onUpdateElement(element.copy(showLocation = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Dates", fontSize = 12.sp)
            Switch(
                checked = element.showDates,
                onCheckedChange = { onUpdateElement(element.copy(showDates = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show GPA", fontSize = 12.sp)
            Switch(
                checked = element.showGPA,
                onCheckedChange = { onUpdateElement(element.copy(showGPA = it)) }
            )
        }

        // Horizontal alignment
        Text("Horizontal Alignment", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HorizontalAlignment.entries.forEach { alignment ->
                FilterChip(
                    selected = (element.horizontalAlignment ?: HorizontalAlignment.START) == alignment,
                    onClick = {
                        onUpdateElement(element.copy(horizontalAlignment = alignment))
                    },
                    label = { Text(alignment.name) }
                )
            }
        }

        // Vertical alignment
        Text("Vertical Alignment", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VerticalAlignment.entries.forEach { alignment ->
                FilterChip(
                    selected = (element.verticalAlignment ?: VerticalAlignment.TOP) == alignment,
                    onClick = {
                        onUpdateElement(element.copy(verticalAlignment = alignment))
                    },
                    label = { Text(alignment.name) }
                )
            }
        }

        // Spacing sliders
        SliderField(
            label = "Entry Spacing: ${element.spacing.toInt()}dp",
            value = element.spacing,
            valueRange = 0f..32f,
            onValueChange = { newSpacing ->
                onUpdateElement(element.copy(spacing = newSpacing))
            }
        )

        SliderField(
            label = "Item Spacing: ${element.itemSpacing.toInt()}dp",
            value = element.itemSpacing,
            valueRange = 0f..16f,
            onValueChange = { newSpacing ->
                onUpdateElement(element.copy(itemSpacing = newSpacing))
            }
        )

        SliderField(
            label = "Achievement Spacing: ${element.achievementSpacing.toInt()}dp",
            value = element.achievementSpacing,
            valueRange = 0f..12f,
            onValueChange = { newSpacing ->
                onUpdateElement(element.copy(achievementSpacing = newSpacing))
            }
        )
    }

    PropertySection(title = "Date & Bullet Settings") {
        // Date format
        Text("Date Format", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DateFormat.entries.forEach { format ->
                FilterChip(
                    selected = element.dateFormat == format,
                    onClick = {
                        onUpdateElement(element.copy(dateFormat = format))
                    },
                    label = { Text(format.name.replace("_", " ")) }
                )
            }
        }

        // Date separator
        OutlinedTextField(
            value = element.dateSeparator,
            onValueChange = { onUpdateElement(element.copy(dateSeparator = it)) },
            label = { Text("Date Separator") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Bullet style
        Text("Bullet Style", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BulletStyle.entries.forEach { style ->
                FilterChip(
                    selected = element.bulletStyle == style,
                    onClick = {
                        onUpdateElement(element.copy(bulletStyle = style))
                    },
                    label = { Text(style.name.replace("_", " ")) }
                )
            }
        }
    }
}

/**
 * Editor for a single education item
 */
@Composable
private fun EducationItemEditor(
    item: EducationItem,
    onUpdate: (EducationItem) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (item.degree.isNotEmpty()) item.degree else "Education",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Degree
            OutlinedTextField(
                value = item.degree,
                onValueChange = { onUpdate(item.copy(degree = it)) },
                label = { Text("Degree") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Institution
            OutlinedTextField(
                value = item.institution,
                onValueChange = { onUpdate(item.copy(institution = it)) },
                label = { Text("Institution") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Location
            OutlinedTextField(
                value = item.location,
                onValueChange = { onUpdate(item.copy(location = it)) },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Start Date
            OutlinedTextField(
                value = item.startDate,
                onValueChange = { onUpdate(item.copy(startDate = it)) },
                label = { Text("Start Date (yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("2016-09-01") }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // End Date
            OutlinedTextField(
                value = item.endDate,
                onValueChange = { onUpdate(item.copy(endDate = it)) },
                label = { Text("End Date (yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("2020-05-31") }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // GPA
            OutlinedTextField(
                value = item.gpa,
                onValueChange = { onUpdate(item.copy(gpa = it)) },
                label = { Text("GPA") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("3.8") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Achievements
            Text("Achievements", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))

            item.achievements.forEachIndexed { index, achievement ->
                AchievementItemEditor(
                    achievement = achievement,
                    onUpdate = { updated ->
                        val updatedAchievements = item.achievements.toMutableList()
                        updatedAchievements[index] = updated
                        onUpdate(item.copy(achievements = updatedAchievements))
                    },
                    onRemove = {
                        val updatedAchievements = item.achievements.toMutableList()
                        updatedAchievements.removeAt(index)
                        onUpdate(item.copy(achievements = updatedAchievements))
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Add Achievement Button
            Button(
                onClick = {
                    val newAchievement = AchievementItem(text = "")
                    onUpdate(item.copy(achievements = item.achievements + newAchievement))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Achievement", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Editor for a single achievement item
 */
@Composable
private fun AchievementItemEditor(
    achievement: AchievementItem,
    onUpdate: (AchievementItem) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = achievement.text,
            onValueChange = { onUpdate(achievement.copy(text = it)) },
            modifier = Modifier.weight(1f),
            singleLine = false,
            maxLines = 3,
            placeholder = { Text("Achievement description", fontSize = 12.sp) }
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Skill element properties
 */
@Composable
private fun SkillElementProperties(
    element: ResumeElement.SkillElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Skill Items") {
        // Skill items list
        element.items.forEachIndexed { index, item ->
            SkillItemEditor(
                item = item,
                onUpdate = { updatedItem ->
                    val updatedItems = element.items.toMutableList()
                    updatedItems[index] = updatedItem
                    onUpdateElement(element.copy(items = updatedItems))
                },
                onRemove = {
                    val updatedItems = element.items.toMutableList()
                    updatedItems.removeAt(index)
                    onUpdateElement(element.copy(items = updatedItems))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add skill item button
        Button(
            onClick = {
                val newItem = SkillItem(
                    name = "New Skill",
                    category = "",
                    proficiency = 0.5f,
                    proficiencyLabel = ""
                )
                onUpdateElement(element.copy(items = element.items + newItem))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Add Skill")
        }
    }

    PropertySection(title = "Display Settings") {
        // Display style
        Text("Display Style", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SkillDisplayStyle.entries.forEach { style ->
                FilterChip(
                    selected = element.displayStyle == style,
                    onClick = {
                        onUpdateElement(element.copy(displayStyle = style))
                    },
                    label = { Text(style.name.replace("_", " ")) }
                )
            }
        }

        // Spacing
        SliderField(
            label = "Spacing",
            value = element.spacing,
            valueRange = 0f..32f,
            onValueChange = { onUpdateElement(element.copy(spacing = it)) }
        )
    }
}

/**
 * Editor for a single skill item
 */
@Composable
private fun SkillItemEditor(
    item: SkillItem,
    onUpdate: (SkillItem) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header with skill name and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name.ifEmpty { "Skill" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                }
            }

            // Skill name
            OutlinedTextField(
                value = item.name,
                onValueChange = { onUpdate(item.copy(name = it)) },
                label = { Text("Skill Name", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Category
            OutlinedTextField(
                value = item.category,
                onValueChange = { onUpdate(item.copy(category = it)) },
                label = { Text("Category (for grouped style)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Proficiency label
            OutlinedTextField(
                value = item.proficiencyLabel,
                onValueChange = { onUpdate(item.copy(proficiencyLabel = it)) },
                label = { Text("Proficiency Label (e.g., 'Expert')", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Proficiency slider
            Text("Proficiency: ${(item.proficiency ?: 0.5f) * 100}%", fontSize = 12.sp)
            Slider(
                value = item.proficiency ?: 0.5f,
                onValueChange = { onUpdate(item.copy(proficiency = it)) },
                valueRange = 0f..1f
            )
        }
    }
}

/**
 * Project element properties
 */
@Composable
private fun ProjectElementProperties(
    element: ResumeElement.ProjectElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Project Items") {
        // Project items list
        element.items.forEachIndexed { index, item ->
            ProjectItemEditor(
                item = item,
                onUpdate = { updatedItem ->
                    val updatedItems = element.items.toMutableList()
                    updatedItems[index] = updatedItem
                    onUpdateElement(element.copy(items = updatedItems))
                },
                onRemove = {
                    val updatedItems = element.items.toMutableList()
                    updatedItems.removeAt(index)
                    onUpdateElement(element.copy(items = updatedItems))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add project item button
        Button(
            onClick = {
                val newItem = ProjectItem(
                    name = "New Project",
                    description = "",
                    startDate = "",
                    endDate = "",
                    isOngoing = false,
                    technologies = "",
                    link = "",
                    highlights = emptyList()
                )
                onUpdateElement(element.copy(items = element.items + newItem))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Add Project")
        }
    }

    PropertySection(title = "Display Settings") {
        // Display style
        Text("Display Style", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProjectDisplayStyle.entries.forEach { style ->
                FilterChip(
                    selected = element.displayStyle == style,
                    onClick = {
                        onUpdateElement(element.copy(displayStyle = style))
                    },
                    label = { Text(style.name) }
                )
            }
        }

        // Show/Hide options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Dates", fontSize = 12.sp)
            Switch(
                checked = element.showDates,
                onCheckedChange = { onUpdateElement(element.copy(showDates = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Technologies", fontSize = 12.sp)
            Switch(
                checked = element.showTechnologies,
                onCheckedChange = { onUpdateElement(element.copy(showTechnologies = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Link", fontSize = 12.sp)
            Switch(
                checked = element.showLink,
                onCheckedChange = { onUpdateElement(element.copy(showLink = it)) }
            )
        }
    }
}

/**
 * Editor for a single project item
 */
@Composable
private fun ProjectItemEditor(
    item: ProjectItem,
    onUpdate: (ProjectItem) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name.ifEmpty { "Project" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                }
            }

            // Project name
            OutlinedTextField(
                value = item.name,
                onValueChange = { onUpdate(item.copy(name = it)) },
                label = { Text("Project Name", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            OutlinedTextField(
                value = item.description,
                onValueChange = { onUpdate(item.copy(description = it)) },
                label = { Text("Description", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Technologies
            OutlinedTextField(
                value = item.technologies,
                onValueChange = { onUpdate(item.copy(technologies = it)) },
                label = { Text("Technologies (comma-separated)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Link
            OutlinedTextField(
                value = item.link,
                onValueChange = { onUpdate(item.copy(link = it)) },
                label = { Text("Project Link/URL", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Dates
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = item.startDate,
                    onValueChange = { onUpdate(item.copy(startDate = it)) },
                    label = { Text("Start Date", fontSize = 10.sp) },
                    placeholder = { Text("yyyy-MM-dd", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = item.endDate,
                    onValueChange = { onUpdate(item.copy(endDate = it)) },
                    label = { Text("End Date", fontSize = 10.sp) },
                    placeholder = { Text("yyyy-MM-dd", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ongoing", fontSize = 12.sp)
                Switch(
                    checked = item.isOngoing,
                    onCheckedChange = { onUpdate(item.copy(isOngoing = it)) }
                )
            }

            // Highlights
            Text("Highlights", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            item.highlights.forEachIndexed { highlightIndex, highlight ->
                ProjectHighlightEditor(
                    highlight = highlight,
                    onUpdate = { updatedHighlight ->
                        val updatedHighlights = item.highlights.toMutableList()
                        updatedHighlights[highlightIndex] = updatedHighlight
                        onUpdate(item.copy(highlights = updatedHighlights))
                    },
                    onRemove = {
                        val updatedHighlights = item.highlights.toMutableList()
                        updatedHighlights.removeAt(highlightIndex)
                        onUpdate(item.copy(highlights = updatedHighlights))
                    }
                )
            }

            Button(
                onClick = {
                    val newHighlight = ProjectHighlight(text = "")
                    onUpdate(item.copy(highlights = item.highlights + newHighlight))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add Highlight")
            }
        }
    }
}

/**
 * Editor for a single project highlight
 */
@Composable
private fun ProjectHighlightEditor(
    highlight: ProjectHighlight,
    onUpdate: (ProjectHighlight) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = highlight.text,
            onValueChange = { onUpdate(highlight.copy(text = it)) },
            modifier = Modifier.weight(1f),
            singleLine = false,
            maxLines = 3,
            placeholder = { Text("Highlight description", fontSize = 12.sp) }
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Certification element properties
 */
@Composable
private fun CertificationElementProperties(
    element: ResumeElement.CertificationElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Certification Items") {
        // Certification items list
        element.items.forEachIndexed { index, item ->
            CertificationItemEditor(
                item = item,
                onUpdate = { updatedItem ->
                    val updatedItems = element.items.toMutableList()
                    updatedItems[index] = updatedItem
                    onUpdateElement(element.copy(items = updatedItems))
                },
                onRemove = {
                    val updatedItems = element.items.toMutableList()
                    updatedItems.removeAt(index)
                    onUpdateElement(element.copy(items = updatedItems))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add certification item button
        Button(
            onClick = {
                val newItem = CertificationItem(
                    name = "New Certification",
                    issuer = "",
                    issueDate = "",
                    expiryDate = "",
                    credentialId = "",
                    verificationLink = ""
                )
                onUpdateElement(element.copy(items = element.items + newItem))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Add Certification")
        }
    }

    PropertySection(title = "Display Settings") {
        // Display style
        Text("Display Style", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CertificationDisplayStyle.entries.forEach { style ->
                FilterChip(
                    selected = element.displayStyle == style,
                    onClick = {
                        onUpdateElement(element.copy(displayStyle = style))
                    },
                    label = { Text(style.name) }
                )
            }
        }

        // Show/Hide options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Issue Date", fontSize = 12.sp)
            Switch(
                checked = element.showIssueDate,
                onCheckedChange = { onUpdateElement(element.copy(showIssueDate = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Expiry Date", fontSize = 12.sp)
            Switch(
                checked = element.showExpiryDate,
                onCheckedChange = { onUpdateElement(element.copy(showExpiryDate = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Credential ID", fontSize = 12.sp)
            Switch(
                checked = element.showCredentialId,
                onCheckedChange = { onUpdateElement(element.copy(showCredentialId = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Expiry Status Badge", fontSize = 12.sp)
            Switch(
                checked = element.showExpiryStatus,
                onCheckedChange = { onUpdateElement(element.copy(showExpiryStatus = it)) }
            )
        }
    }
}

/**
 * Editor for a single certification item
 */
@Composable
private fun CertificationItemEditor(
    item: CertificationItem,
    onUpdate: (CertificationItem) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name.ifEmpty { "Certification" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                }
            }

            // Certification name
            OutlinedTextField(
                value = item.name,
                onValueChange = { onUpdate(item.copy(name = it)) },
                label = { Text("Certification Name", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Issuer
            OutlinedTextField(
                value = item.issuer,
                onValueChange = { onUpdate(item.copy(issuer = it)) },
                label = { Text("Issuer/Organization", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Dates
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = item.issueDate,
                    onValueChange = { onUpdate(item.copy(issueDate = it)) },
                    label = { Text("Issue Date", fontSize = 10.sp) },
                    placeholder = { Text("yyyy-MM-dd", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = item.expiryDate,
                    onValueChange = { onUpdate(item.copy(expiryDate = it)) },
                    label = { Text("Expiry (optional)", fontSize = 10.sp) },
                    placeholder = { Text("yyyy-MM-dd", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Credential ID
            OutlinedTextField(
                value = item.credentialId,
                onValueChange = { onUpdate(item.copy(credentialId = it)) },
                label = { Text("Credential ID", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Verification Link
            OutlinedTextField(
                value = item.verificationLink,
                onValueChange = { onUpdate(item.copy(verificationLink = it)) },
                label = { Text("Verification Link", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Language element properties
 */
@Composable
private fun LanguageElementProperties(
    element: ResumeElement.LanguageElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Language Items") {
        // Language items list
        element.items.forEachIndexed { index, item ->
            LanguageItemEditor(
                item = item,
                onUpdate = { updatedItem ->
                    val updatedItems = element.items.toMutableList()
                    updatedItems[index] = updatedItem
                    onUpdateElement(element.copy(items = updatedItems))
                },
                onRemove = {
                    val updatedItems = element.items.toMutableList()
                    updatedItems.removeAt(index)
                    onUpdateElement(element.copy(items = updatedItems))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add language item button
        Button(
            onClick = {
                val newItem = LanguageItem(
                    name = "New Language",
                    proficiency = 0.5f,
                    proficiencyLabel = "",
                    cefrLevel = null
                )
                onUpdateElement(element.copy(items = element.items + newItem))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Add Language")
        }
    }

    PropertySection(title = "Display Settings") {
        // Display style
        Text("Display Style", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LanguageDisplayStyle.entries.forEach { style ->
                FilterChip(
                    selected = element.displayStyle == style,
                    onClick = {
                        onUpdateElement(element.copy(displayStyle = style))
                    },
                    label = { Text(style.name.replace("_", " ")) }
                )
            }
        }

        // Proficiency type
        Text("Proficiency Type", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LanguageProficiencyType.entries.forEach { type ->
                FilterChip(
                    selected = element.proficiencyType == type,
                    onClick = {
                        onUpdateElement(element.copy(proficiencyType = type))
                    },
                    label = { Text(type.name) }
                )
            }
        }
    }
}

/**
 * Editor for a single language item
 */
@Composable
private fun LanguageItemEditor(
    item: LanguageItem,
    onUpdate: (LanguageItem) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name.ifEmpty { "Language" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                }
            }

            // Language name
            OutlinedTextField(
                value = item.name,
                onValueChange = { onUpdate(item.copy(name = it)) },
                label = { Text("Language Name", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Proficiency label
            OutlinedTextField(
                value = item.proficiencyLabel,
                onValueChange = { onUpdate(item.copy(proficiencyLabel = it)) },
                label = { Text("Proficiency Label (e.g., 'Native', 'Fluent')", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // CEFR Level
            OutlinedTextField(
                value = item.cefrLevel ?: "",
                onValueChange = { onUpdate(item.copy(cefrLevel = it.ifEmpty { null })) },
                label = { Text("CEFR Level (e.g., 'C2', 'B2')", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Proficiency slider
            Text("Proficiency: ${(item.proficiency * 100).toInt()}%", fontSize = 12.sp)
            Slider(
                value = item.proficiency,
                onValueChange = { onUpdate(item.copy(proficiency = it)) },
                valueRange = 0f..1f
            )
        }
    }
}

// Helper functions to update elements immutably
private fun updateElementPosition(element: ResumeElement, newPosition: GridPosition): ResumeElement {
    return when (element) {
        is ResumeElement.TextElement -> element.copy(position = newPosition)
        is ResumeElement.ImageElement -> element.copy(position = newPosition)
        is ResumeElement.ShapeElement -> element.copy(position = newPosition)
        is ResumeElement.ChartElement -> element.copy(position = newPosition)
        is ResumeElement.ContainerElement -> element.copy(position = newPosition)
        is ResumeElement.IconElement -> element.copy(position = newPosition)
        is ResumeElement.ContactElement -> element.copy(position = newPosition)
        is ResumeElement.WorkExperienceElement -> element.copy(position = newPosition)
        is ResumeElement.EducationElement -> element.copy(position = newPosition)
        is ResumeElement.SkillElement -> element.copy(position = newPosition)
        is ResumeElement.ProjectElement -> element.copy(position = newPosition)
        is ResumeElement.CertificationElement -> element.copy(position = newPosition)
        is ResumeElement.LanguageElement -> element.copy(position = newPosition)
    }
}

private fun updateElementZIndex(element: ResumeElement, newZIndex: Int): ResumeElement {
    return when (element) {
        is ResumeElement.TextElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.ImageElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.ShapeElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.ChartElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.ContainerElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.IconElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.ContactElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.WorkExperienceElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.EducationElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.SkillElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.ProjectElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.CertificationElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.LanguageElement -> element.copy(zIndex = newZIndex)
    }
}

private fun updateElementLocked(element: ResumeElement, locked: Boolean): ResumeElement {
    return when (element) {
        is ResumeElement.TextElement -> element.copy(locked = locked)
        is ResumeElement.ImageElement -> element.copy(locked = locked)
        is ResumeElement.ShapeElement -> element.copy(locked = locked)
        is ResumeElement.ChartElement -> element.copy(locked = locked)
        is ResumeElement.ContainerElement -> element.copy(locked = locked)
        is ResumeElement.IconElement -> element.copy(locked = locked)
        is ResumeElement.ContactElement -> element.copy(locked = locked)
        is ResumeElement.WorkExperienceElement -> element.copy(locked = locked)
        is ResumeElement.EducationElement -> element.copy(locked = locked)
        is ResumeElement.SkillElement -> element.copy(locked = locked)
        is ResumeElement.ProjectElement -> element.copy(locked = locked)
        is ResumeElement.CertificationElement -> element.copy(locked = locked)
        is ResumeElement.LanguageElement -> element.copy(locked = locked)
    }
}

private fun updateElementStyle(element: ResumeElement, newStyle: ElementStyle): ResumeElement {
    return when (element) {
        is ResumeElement.TextElement -> element.copy(style = newStyle)
        is ResumeElement.ImageElement -> element.copy(style = newStyle)
        is ResumeElement.ShapeElement -> element.copy(style = newStyle)
        is ResumeElement.ChartElement -> element.copy(style = newStyle)
        is ResumeElement.ContainerElement -> element.copy(style = newStyle)
        is ResumeElement.IconElement -> element.copy(style = newStyle)
        is ResumeElement.ContactElement -> element.copy(style = newStyle)
        is ResumeElement.WorkExperienceElement -> element.copy(style = newStyle)
        is ResumeElement.EducationElement -> element.copy(style = newStyle)
        is ResumeElement.SkillElement -> element.copy(style = newStyle)
        is ResumeElement.ProjectElement -> element.copy(style = newStyle)
        is ResumeElement.CertificationElement -> element.copy(style = newStyle)
        is ResumeElement.LanguageElement -> element.copy(style = newStyle)
    }
}

private fun updateElementTag(element: ResumeElement, tag: UserInfoTag?): ResumeElement {
    return when (element) {
        is ResumeElement.TextElement -> element.copy(userInfoTag = tag)
        is ResumeElement.ImageElement -> element.copy(userInfoTag = tag)
        is ResumeElement.ShapeElement -> element.copy(userInfoTag = tag)
        is ResumeElement.ChartElement -> element.copy(userInfoTag = tag)
        is ResumeElement.ContainerElement -> element.copy(userInfoTag = tag)
        is ResumeElement.IconElement -> element.copy(userInfoTag = tag)
        is ResumeElement.ContactElement -> element.copy(userInfoTag = tag)
        is ResumeElement.WorkExperienceElement -> element.copy(userInfoTag = tag)
        is ResumeElement.EducationElement -> element.copy(userInfoTag = tag)
        is ResumeElement.SkillElement -> element.copy(userInfoTag = tag)
        is ResumeElement.ProjectElement -> element.copy(userInfoTag = tag)
        is ResumeElement.CertificationElement -> element.copy(userInfoTag = tag)
        is ResumeElement.LanguageElement -> element.copy(userInfoTag = tag)
    }
}
