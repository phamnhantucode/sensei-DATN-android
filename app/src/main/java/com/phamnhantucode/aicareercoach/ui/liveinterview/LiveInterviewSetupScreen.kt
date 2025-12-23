package com.phamnhantucode.aicareercoach.ui.liveinterview

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.data.interview.InterviewType
import com.phamnhantucode.aicareercoach.data.local.GridResumeEntity
import com.phamnhantucode.aicareercoach.data.resume.GridResumeRepository
import kotlinx.coroutines.launch

private const val TAG = "LiveInterviewSetupScreen"

// Interview setup screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveInterviewSetupScreen(
    onBack: () -> Unit,
    onStartInterview: (InterviewConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    // New fields for job-based interview
    var jobTitle by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var selectedResume by remember { mutableStateOf<GridResumeEntity?>(null) }
    var resumes by remember { mutableStateOf<List<GridResumeEntity>>(emptyList()) }
    var showResumeDropdown by remember { mutableStateOf(false) }
    var isLoadingResumes by remember { mutableStateOf(true) }
    
    // Hidden but preserved fields (fixed values)
    val selectedType = InterviewType.GENERAL
    val questionCount = 5
    var useBatchMode by remember { mutableStateOf(true) }
    
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Load user's resumes
    LaunchedEffect(Unit) {
        try {
            val repository = GridResumeRepository.getInstance(context)
            val result = repository.getAllDesigns()
            if (result.isSuccess) {
                resumes = result.getOrNull() ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load resumes", e)
        } finally {
            isLoadingResumes = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Interview Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Voice Interview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Practice with AI interviewer using speech",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Job Title Input
            Text(
                text = "Job Title *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = jobTitle,
                onValueChange = { jobTitle = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., Senior Software Engineer") },
                leadingIcon = {
                    Icon(Icons.Filled.Work, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Job Description Input
            Text(
                text = "Job Description *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = jobDescription,
                onValueChange = { jobDescription = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = { Text("Paste the job description here...") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Description, 
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 80.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )

            // Resume Selection (Optional)
            Text(
                text = "Select Resume (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            ExposedDropdownMenuBox(
                expanded = showResumeDropdown,
                onExpandedChange = { showResumeDropdown = it }
            ) {
                // Build display text for selected resume
                val displayText = selectedResume?.let { resume ->
                    val workExp = resume.designData.pages
                        .flatMap { it.elements }
                        .filterIsInstance<com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement.WorkExperienceElement>()
                        .firstOrNull()
                        ?.items
                        ?.firstOrNull()
                    if (workExp != null && workExp.jobTitle.isNotBlank()) {
                        "${resume.name} (${workExp.jobTitle})"
                    } else {
                        resume.name
                    }
                } ?: "No resume selected"
                
                OutlinedTextField(
                    value = displayText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showResumeDropdown)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                
                ExposedDropdownMenu(
                    expanded = showResumeDropdown,
                    onDismissRequest = { showResumeDropdown = false }
                ) {
                    // Option to clear selection
                    DropdownMenuItem(
                        text = { Text("No resume selected") },
                        onClick = {
                            selectedResume = null
                            showResumeDropdown = false
                        }
                    )
                    
                    if (isLoadingResumes) {
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    Text("Loading resumes...")
                                }
                            },
                            onClick = {}
                        )
                    } else if (resumes.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No resumes available") },
                            onClick = { showResumeDropdown = false }
                        )
                    } else {
                        resumes.forEach { resume ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(
                                            text = resume.name,
                                            fontWeight = FontWeight.Medium
                                        )
                                        // Show first work experience if available
                                        val workExp = resume.designData.pages
                                            .flatMap { it.elements }
                                            .filterIsInstance<com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement.WorkExperienceElement>()
                                            .firstOrNull()
                                            ?.items
                                            ?.firstOrNull()
                                        if (workExp != null && workExp.jobTitle.isNotBlank()) {
                                            Text(
                                                text = "${workExp.jobTitle} at ${workExp.company}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedResume = resume
                                    showResumeDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Interview mode selection
            Text(
                text = "Interview Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            InterviewModeSelector(
                useBatchMode = useBatchMode,
                onModeChanged = { useBatchMode = it }
            )

            // Permission check
            if (!hasAudioPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Microphone Permission Required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Live interview requires microphone access to record your answers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Start button
            val canStart = hasAudioPermission && jobTitle.isNotBlank() && jobDescription.isNotBlank()
            
            Button(
                onClick = {
                    Log.d(TAG, "Start Interview button clicked")
                    Log.d(TAG, "hasAudioPermission=$hasAudioPermission")

                    if (hasAudioPermission) {
                        val user = Clerk.user
                        Log.d(TAG, "Clerk.user=${if (user != null) "ID:${user.id}" else "NULL"}")

                        if (user != null) {
                            // Extract resume content if selected
                            val resumeContent = selectedResume?.let { resume ->
                                extractResumeContent(resume)
                            }
                            
                            val config = InterviewConfig(
                                userId = user.id,
                                interviewType = selectedType,
                                questionCount = questionCount,
                                useBatchMode = useBatchMode,
                                jobTitle = jobTitle,
                                jobDescription = jobDescription,
                                resumeId = selectedResume?.id,
                                resumeContent = resumeContent
                            )
                            Log.d(TAG, "Calling onStartInterview with config: userId=${config.userId}, jobTitle=${config.jobTitle}, hasResume=${config.resumeId != null}")
                            onStartInterview(config)
                            Log.d(TAG, "onStartInterview callback invoked")
                        } else {
                            Log.e(TAG, "Clerk.user is null - cannot start interview")
                        }
                    } else {
                        Log.d(TAG, "Audio permission not granted, showing permission dialog")
                        showPermissionDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = canStart
            ) {
                Text(
                    text = "Start Interview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (!canStart && hasAudioPermission) {
                Text(
                    text = "Please enter job title and job description to continue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Permission denied dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = { Text("Microphone permission is required for live interviews. Please grant the permission to continue.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Extracts relevant content from a resume for interview context.
 */
private fun extractResumeContent(resume: GridResumeEntity): String {
    val parts = mutableListOf<String>()
    
    try {
        val designData = resume.designData
        
        // Extract text from all pages
        designData.pages.forEach { page ->
            page.elements.forEach { element ->
                when (element) {
                    is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement.TextElement -> {
                        if (element.content.isNotBlank()) {
                            parts.add(element.content)
                        }
                    }
                    is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement.WorkExperienceElement -> {
                        element.items.forEach { item ->
                            val exp = buildString {
                                append("Work: ${item.jobTitle} at ${item.company}")
                                val responsibilities = item.responsibilities.map { it.text }.filter { it.isNotBlank() }
                                if (responsibilities.isNotEmpty()) {
                                    append(" - ${responsibilities.joinToString("; ")}")
                                }
                            }
                            parts.add(exp)
                        }
                    }
                    is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement.EducationElement -> {
                        element.items.forEach { item ->
                            parts.add("Education: ${item.degree} from ${item.institution}")
                        }
                    }
                    is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement.SkillElement -> {
                        val skills = element.items.map { it.name }.filter { it.isNotBlank() }
                        if (skills.isNotEmpty()) {
                            parts.add("Skills: ${skills.joinToString(", ")}")
                        }
                    }
                    is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement.ProjectElement -> {
                        element.items.forEach { item ->
                            parts.add("Project: ${item.name} - ${item.description}")
                        }
                    }
                    else -> { /* Skip other element types */ }
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error extracting resume content", e)
    }
    
    return parts.joinToString(" | ").take(2000) // Limit to 2000 chars
}

@Composable
private fun InterviewModeSelector(
    useBatchMode: Boolean,
    onModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Batch Mode Card
        Card(
            onClick = { onModeChanged(true) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (useBatchMode)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            border = if (useBatchMode)
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else
                null
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = useBatchMode,
                    onClick = { onModeChanged(true) }
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📝 Batch Mode (Recommended)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Answer all questions first, then get comprehensive feedback for everything",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Immediate Mode Card
        Card(
            onClick = { onModeChanged(false) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (!useBatchMode)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            border = if (!useBatchMode)
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else
                null
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = !useBatchMode,
                    onClick = { onModeChanged(false) }
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "⚡ Immediate Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Get feedback after each individual question",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
