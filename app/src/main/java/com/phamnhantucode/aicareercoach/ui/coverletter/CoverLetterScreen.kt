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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamnhantucode.aicareercoach.data.coverletter.EmailType
import com.phamnhantucode.aicareercoach.ui.components.CreditExhaustedDialog
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
    val showCreditDialog by viewModel.showCreditDialog.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var letterToDelete by remember { mutableStateOf<CoverLetterEntry?>(null) }

    val hasExistingLetters = when (val state = uiState) {
        is CoverLetterUiState.Success -> state.coverLetters.isNotEmpty()
        else -> false
    }


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
                                text = "Job Search Emails",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Assistant for all your career emails",
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
                                contentDescription = "Create new email"
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
                                        text = "Failed to load emails",
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
                            title = { Text("Delete Email?") },
                            text = { Text("This action cannot be undone.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        letterToDelete?.let { entry ->
                                            viewModel.deleteCoverLetter(entry)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Email removed")
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


                    if (generationState is GenerationState.Generating) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        )
                    }


                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            if (showCreditDialog) {
                CreditExhaustedDialog(
                    onDismiss = { viewModel.dismissCreditDialog() },
                    onPurchase = { viewModel.openPurchaseScreen() }
                )
            }

            if (showCreateDialog) {
                CreateEmailDialog(
                    isGenerating = generationState is GenerationState.Generating,
                    onDismiss = { showCreateDialog = false },
                    onCreate = { type, inputs ->
                        showCreateDialog = false
                        viewModel.generateEmail(
                            type = type,
                            inputs = inputs,
                            onSuccess = { entry ->
                                onOpenEditor(entry)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("${type.displayName} generated!")
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

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = entry.type.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    
                    Text(
                        text = entry.jobTitle.takeIf { it.isNotBlank() } ?: "No Title",
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
                    if (entry.jobDescription.isNotBlank()) {
                         RowWithIconText(
                            icon = Icons.Filled.Description,
                            text = entry.jobDescription,
                            contentDescription = "Details",
                            maxLines = 2
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete email"
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
    icon: ImageVector,
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
                text = "Boost your job search communication",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Create tailored Cover Letters, Connection Requests, and Thank You notes in seconds.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onCreateNew) {
                Text("Start Writing")
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
private fun CreateEmailDialog(
    isGenerating: Boolean = false,
    onDismiss: () -> Unit,
    onCreate: (type: EmailType, inputs: Map<String, String>) -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf(EmailType.APPLICATION) }
    

    var companyName by rememberSaveable { mutableStateOf("") }
    var recipientName by rememberSaveable { mutableStateOf("") }
    

    var jobTitle by rememberSaveable { mutableStateOf("") }
    var jobDescription by rememberSaveable { mutableStateOf("") }
    var context by rememberSaveable { mutableStateOf("") }
    var additionalInfo by rememberSaveable { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current

    val isCreateEnabled = !isGenerating && companyName.isNotBlank() && when(selectedType) {
        EmailType.APPLICATION -> jobTitle.isNotBlank() && jobDescription.isNotBlank()
        EmailType.PROSPECTING -> context.isNotBlank()
        EmailType.REFERRAL -> recipientName.isNotBlank() && context.isNotBlank()
        EmailType.THANK_YOU -> jobTitle.isNotBlank() && recipientName.isNotBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Job Search Email") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    items(EmailType.values()) { type ->
                        FilterChip(
                            selected = type == selectedType,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                
                Divider()


                Text(
                    text = "Provide details for ${selectedType.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )


                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )


                val recipientLabel = when(selectedType) {
                    EmailType.APPLICATION -> "Hiring Manager (Optional)"
                    EmailType.PROSPECTING -> "Recipient Name (Optional)"
                    EmailType.REFERRAL -> "Contact Name (Required)"
                    EmailType.THANK_YOU -> "Interviewer Name (Required)"
                }
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text(recipientLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = if(selectedType == EmailType.REFERRAL || selectedType == EmailType.THANK_YOU) {
                        { Icon(Icons.Default.Person, contentDescription = null) }
                    } else null
                )


                when (selectedType) {
                    EmailType.APPLICATION -> {
                        OutlinedTextField(
                            value = jobTitle,
                            onValueChange = { jobTitle = it },
                            label = { Text("Job Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = jobDescription,
                            onValueChange = { jobDescription = it },
                            label = { Text("Job Description") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    clipboardManager.getText()?.text?.let { jobDescription = it }
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                                }
                            }
                        )
                    }
                    EmailType.PROSPECTING -> {
                        OutlinedTextField(
                            value = additionalInfo,
                            onValueChange = { additionalInfo = it },
                            label = { Text("Target Role (Optional)") },
                            placeholder = { Text("e.g. Senior Android Dev") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = context,
                            onValueChange = { context = it },
                            label = { Text("Connection / Context") },
                            placeholder = { Text("Why are you contacting them?") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    EmailType.REFERRAL -> {
                         OutlinedTextField(
                            value = context,
                            onValueChange = { context = it },
                            label = { Text("Relationship") },
                            placeholder = { Text("e.g. Ex-colleague, Alumni") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = additionalInfo,
                            onValueChange = { additionalInfo = it },
                            label = { Text("Target Job Link/ID (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    EmailType.THANK_YOU -> {
                        OutlinedTextField(
                            value = jobTitle,
                            onValueChange = { jobTitle = it },
                            label = { Text("Job Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = context,
                            onValueChange = { context = it },
                            label = { Text("Key Discussion Topic") },
                            placeholder = { Text("Something memorable discussed...") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val inputs = mutableMapOf(
                        "companyName" to companyName.trim(),
                        "recipientName" to recipientName.trim()
                    )
                    when (selectedType) {
                        EmailType.APPLICATION -> {
                            inputs["jobTitle"] = jobTitle.trim()
                            inputs["jobDescription"] = jobDescription.trim()
                        }
                        EmailType.PROSPECTING -> {
                            inputs["targetRole"] = additionalInfo.trim()
                            inputs["context"] = context.trim()
                        }
                        EmailType.REFERRAL -> {
                            inputs["relationship"] = context.trim()
                            inputs["targetJob"] = additionalInfo.trim()
                        }
                        EmailType.THANK_YOU -> {
                            inputs["jobTitle"] = jobTitle.trim()
                            inputs["topic"] = context.trim()
                        }
                    }
                    onCreate(selectedType, inputs)
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
private fun CreateEmailDialogPreview() {
    AppTheme {
        CreateEmailDialog(
            onDismiss = {},
            onCreate = { _, _ -> }
        )
    }
}
