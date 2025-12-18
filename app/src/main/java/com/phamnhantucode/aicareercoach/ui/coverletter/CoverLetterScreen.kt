package com.phamnhantucode.aicareercoach.ui.coverletter


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverLetterScreen(
    onBack: () -> Unit = {},
    onOpenEditor: (CoverLetterEntry) -> Unit = {},
    viewModel: CoverLetterViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val generationState by viewModel.generationState.collectAsState()
    val resumes by viewModel.resumes.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var letterToDelete by remember { mutableStateOf<CoverLetterEntry?>(null) }

    val hasExistingLetters = when (val state = uiState) {
        is CoverLetterUiState.Success -> state.coverLetters.isNotEmpty()
        else -> false
    }

    // Handle generation state
    LaunchedEffect(generationState) {
        when (generationState) {
            is GenerationState.Error -> {
                snackbarHostState.showSnackbar((generationState as GenerationState.Error).message)
                viewModel.resetGenerationState()
            }
            else -> {}
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with back button (Sticky)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            top = 12.dp + WindowInsets.systemBars.asPaddingValues()
                                .calculateTopPadding(),
                            bottom = 12.dp,
                            end = 16.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column {
                            Text(
                                text = "AI Cover Letters",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Generate tailored cover letters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (hasExistingLetters) {
                        Button(
                            onClick = {
                                showCreateDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Create new cover letter"
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(text = "Create")
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        is CoverLetterUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is CoverLetterUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Failed to load cover letters",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(onClick = { viewModel.loadCoverLetters() }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                        is CoverLetterUiState.Success -> {
                            if (state.coverLetters.isEmpty()) {
                                EmptyCoverLetterState(
                                    modifier = Modifier.fillMaxSize(),
                                    onCreateNew = { showCreateDialog = true }
                                )
                            } else {
                                CoverLetterList(
                                    items = state.coverLetters,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
                                    onOpen = { entry -> onOpenEditor(entry) },
                                    onDelete = { entry ->
                                        letterToDelete = entry
                                    }
                                )
                            }
                        }
                    }

                    if (letterToDelete != null) {
                        AlertDialog(
                            onDismissRequest = { letterToDelete = null },
                            title = { Text("Delete Cover Letter?") },
                            text = { Text("This action cannot be undone.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        letterToDelete?.let { entry ->
                                            viewModel.deleteCoverLetter(entry)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("\"${entry.jobTitle}\" removed")
                                            }
                                        }
                                        letterToDelete = null
                                    }
                                ) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { letterToDelete = null }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // Show loading indicator at top when generating
                    if (generationState is GenerationState.Generating) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        )
                    }

                    // Snackbar host
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            if (showCreateDialog) {
                CreateCoverLetterDialog(
                    isGenerating = generationState is GenerationState.Generating,
                    resumes = resumes,
                    onDismiss = { showCreateDialog = false },
                    onCreate = { companyName, jobTitle, jobDescription, tone, resume ->
                        showCreateDialog = false
                        viewModel.generateCoverLetter(
                            companyName = companyName,
                            jobTitle = jobTitle,
                            jobDescription = jobDescription,
                            tone = tone,
                            resume = resume,
                            onSuccess = { entry ->
                                onOpenEditor(entry)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Cover letter generated!")
                                }
                            }
                        )
                    }
                )
            }
        }
    }

@Composable
private fun CoverLetterList(
    items: List<CoverLetterEntry>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onOpen: (CoverLetterEntry) -> Unit,
    onDelete: (CoverLetterEntry) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = contentPadding
    ) {
        items(items, key = { it.id }) { entry ->
            CoverLetterCard(
                entry = entry,
                onOpen = { onOpen(entry) },
                onDelete = { onDelete(entry) }
            )
        }
    }
}

@Composable
private fun CoverLetterCard(
    entry: CoverLetterEntry,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.jobTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    RowWithIconText(
                        icon = Icons.Filled.WorkOutline,
                        text = entry.companyName,
                        contentDescription = "Company name",
                        maxLines = 1
                    )
                    RowWithIconText(
                        icon = Icons.Filled.Description,
                        text = entry.jobDescription,
                        contentDescription = "Job description",
                        maxLines = 2
                    )
                }
                FilledTonalIconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete cover letter"
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            RowWithIconText(
                icon = Icons.Filled.Schedule,
                text = formatTimestamp(entry.createdAt),
                contentDescription = "Creation time",
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RowWithIconText(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    contentDescription: String,
    maxLines: Int = Int.MAX_VALUE
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(4.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyCoverLetterState(
    modifier: Modifier = Modifier,
    onCreateNew: () -> Unit = {}
) {
    Box(
        modifier = modifier.padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Create your first AI cover letter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Spin up tailored cover letters and track every role you apply for.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onCreateNew) {
                Text("Start drafting")
            }
        }
    }
}

private fun formatTimestamp(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • h:mm a", Locale.getDefault())
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCoverLetterDialog(
    isGenerating: Boolean = false,
    resumes: List<com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume>,
    onDismiss: () -> Unit,
    onCreate: (companyName: String, jobTitle: String, jobDescription: String, tone: String, resume: com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume?) -> Unit
) {
    var companyName by rememberSaveable { mutableStateOf("") }
    var jobTitle by rememberSaveable { mutableStateOf("") }
    var jobDescription by rememberSaveable { mutableStateOf("") }
    var selectedResume by remember { mutableStateOf<com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume?>(null) }
    
    // Tone State
    val tones = listOf("Professional", "Enthusiastic", "Confident", "Concise", "Creative")
    var selectedTone by rememberSaveable { mutableStateOf(tones.first()) }
    var showToneDropdown by remember { mutableStateOf(false) }

    var showResumeDropdown by remember { mutableStateOf(false) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // If there's only one resume, select it automatically? No, keep it optional but perhaps suggest it if wanted. 
    // User requested "Use existing resume (optional)". So default to null is correct.

    val isCreateEnabled = !isGenerating && companyName.isNotBlank() && jobTitle.isNotBlank() && jobDescription.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create cover letter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Provide information about the position you're applying for",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Resume Selection
                if (resumes.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedResume?.personalInfo?.fullName?.let { "$it (Resume)" } ?: "No resume selected (Default)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Use Resume Data (Optional)") },
                            trailingIcon = {
                                androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = showResumeDropdown
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResumeDropdown = !showResumeDropdown },
                            colors = androidx.compose.material3.ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        // Invisible overlay to capture clicks
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showResumeDropdown = !showResumeDropdown }
                        )

                        androidx.compose.material3.DropdownMenu(
                            expanded = showResumeDropdown,
                            onDismissRequest = { showResumeDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("No resume selected") },
                                onClick = {
                                    selectedResume = null
                                    showResumeDropdown = false
                                }
                            )
                            resumes.forEach { resume ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(resume.personalInfo.fullName.ifBlank { "Untitled Resume" })
                                            if (resume.workExperiences.isNotEmpty()) {
                                                Text(
                                                    text = "${resume.workExperiences.first().jobTitle} at ${resume.workExperiences.first().company}",
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

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("Job title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = jobDescription,
                    onValueChange = { jobDescription = it },
                    label = { Text("Job description") },
                    minLines = 3,
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let {
                                jobDescription = it
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    }
                )

                // Tone Selection
                ExposedDropdownMenuBox(
                    expanded = showToneDropdown,
                    onExpandedChange = { showToneDropdown = !showToneDropdown }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = selectedTone,
                        onValueChange = {},
                        label = { Text("Tone") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showToneDropdown) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = showToneDropdown,
                        onDismissRequest = { showToneDropdown = false },
                    ) {
                        tones.forEach { tone ->
                            DropdownMenuItem(
                                text = { Text(tone) },
                                onClick = {
                                    selectedTone = tone
                                    showToneDropdown = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        companyName.trim(),
                        jobTitle.trim(),
                        jobDescription.trim(),
                        selectedTone,
                        selectedResume
                    )
                },
                enabled = isCreateEnabled
            ) {
                if (isGenerating) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Generating...")
                    }
                } else {
                    Text("Generate")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun CreateCoverLetterDialogPreview() {
    AppTheme {
        CreateCoverLetterDialog(
            onDismiss = {},
            resumes = emptyList(),
            onCreate = { _, _, _, _, _ -> }
        )
    }
}
