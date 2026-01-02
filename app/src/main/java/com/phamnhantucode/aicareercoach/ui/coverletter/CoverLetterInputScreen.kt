package com.phamnhantucode.aicareercoach.ui.coverletter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamnhantucode.aicareercoach.data.coverletter.EmailType
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverLetterInputScreen(
    type: String,
    onBack: () -> Unit = {},
    onCoverLetterGenerated: (CoverLetterEntry) -> Unit = {},
    viewModel: CoverLetterViewModel = viewModel()
) {
    val emailType = try {
        EmailType.valueOf(type)
    } catch (e: IllegalArgumentException) {
        EmailType.APPLICATION
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val generationState by viewModel.generationState.collectAsState()

    var companyName by rememberSaveable { mutableStateOf("") }
    var recipientName by rememberSaveable { mutableStateOf("") }
    var jobTitle by rememberSaveable { mutableStateOf("") }
    var jobDescription by rememberSaveable { mutableStateOf("") }
    var context by rememberSaveable { mutableStateOf("") }
    var additionalInfo by rememberSaveable { mutableStateOf("") }

    val isFormValid = companyName.isNotBlank() && when(emailType) {
        EmailType.APPLICATION -> jobTitle.isNotBlank() && jobDescription.isNotBlank()
        EmailType.PROSPECTING -> context.isNotBlank()
        EmailType.REFERRAL -> recipientName.isNotBlank() && context.isNotBlank()
        EmailType.THANK_YOU -> jobTitle.isNotBlank() && recipientName.isNotBlank()
    }

    LaunchedEffect(generationState) {
        when (val state = generationState) {
            is GenerationState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetGenerationState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(emailType.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Provide details for ${emailType.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Name ${if(emailType != EmailType.PROSPECTING) "*" else ""}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val recipientLabel = when(emailType) {
                EmailType.APPLICATION -> "Hiring Manager (Optional)"
                EmailType.PROSPECTING -> "Recipient Name (Optional)"
                EmailType.REFERRAL -> "Contact Name *"
                EmailType.THANK_YOU -> "Interviewer Name *"
            }

            OutlinedTextField(
                value = recipientName,
                onValueChange = { recipientName = it },
                label = { Text(recipientLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = if(emailType == EmailType.REFERRAL || emailType == EmailType.THANK_YOU) {
                    { Icon(Icons.Default.Person, contentDescription = null) }
                } else null
            )

            when (emailType) {
                EmailType.APPLICATION -> {
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("Job Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = { jobDescription = it },
                        label = { Text("Job Description *") },
                        minLines = 5,
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
                        label = { Text("Connection / Context *") },
                        placeholder = { Text("Why are you contacting them?") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                EmailType.REFERRAL -> {
                    OutlinedTextField(
                        value = context,
                        onValueChange = { context = it },
                        label = { Text("Relationship *") },
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
                        label = { Text("Job Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = context,
                        onValueChange = { context = it },
                        label = { Text("Key Discussion Topic *") },
                        placeholder = { Text("Something memorable discussed...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val inputs = mutableMapOf(
                        "companyName" to companyName.trim(),
                        "recipientName" to recipientName.trim()
                    )
                    when (emailType) {
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
                    viewModel.generateEmail(emailType, inputs, onSuccess = { entry ->
                        onCoverLetterGenerated(entry)
                    })
                },
                enabled = isFormValid && generationState !is GenerationState.Generating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (generationState is GenerationState.Generating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Generating...")
                } else {
                    Text("Generate Email")
                }
            }
        }
    }
}
