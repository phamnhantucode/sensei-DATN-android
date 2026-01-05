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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
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
import kotlinx.coroutines.launch

private const val TAG = "LiveInterviewSetupScreen"

// Interview setup screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveInterviewSetupScreen(
    onBack: () -> Unit,
    onStartInterview: (InterviewConfig) -> Unit,
    onViewFeedback: (com.phamnhantucode.aicareercoach.data.interview.LiveMockInterviewSession) -> Unit,
    interviewHistory: List<com.phamnhantucode.aicareercoach.data.interview.LiveMockInterviewSession> = emptyList(),
    isHistoryLoading: Boolean = false,
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

    // Hidden but preserved fields (fixed values)
    val selectedType = InterviewType.GENERAL
    val questionCount = 5

    val useBatchMode = true
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showSetupDialog by remember { mutableStateOf(false) }

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



            // Start button
            val canStart = hasAudioPermission
            
            Button(
                onClick = {
                    Log.d(TAG, "Start Interview button clicked")
                    Log.d(TAG, "hasAudioPermission=$hasAudioPermission")

                    if (hasAudioPermission) {
                        val user = Clerk.user
                        Log.d(TAG, "Clerk.user=${if (user != null) "ID:${user.id}" else "NULL"}")

                        if (user != null) {
                            showSetupDialog = true
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

            // Previous Interviews Section
            if (isHistoryLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Previous Interviews",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                repeat(3) {
                    InterviewHistoryShimmerItem()
                }
            } else if (interviewHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Previous Interviews",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                interviewHistory.forEach { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {

                                    
                                    Column {
                                        Text(
                                            text = session.jobTitle.ifBlank { "General Interview" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Last Tried: ${session.startedAt?.let { java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "Unknown Date"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }


                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Score display (if completed)
                                if (session.status == com.phamnhantucode.aicareercoach.data.interview.InterviewStatus.COMPLETED && session.overallScore != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                          Icon(Icons.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                          Text(
                                              text = "Score: ${String.format("%.1f", session.overallScore)}/10",
                                              style = MaterialTheme.typography.labelMedium,
                                              color = MaterialTheme.colorScheme.secondary,
                                              fontWeight = FontWeight.SemiBold
                                          )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp)) // Spacer to keep layout balanced
                                }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Feedback Button
                                if (session.status == com.phamnhantucode.aicareercoach.data.interview.InterviewStatus.COMPLETED) {
                                    OutlinedButton(
                                        onClick = { onViewFeedback(session) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Feedback", style = MaterialTheme.typography.labelLarge)
                                    }
                                }

                                // Start Button
                                Button(
                                    onClick = {
                                        val user = Clerk.user
                                        if (user != null) {
                                            val config = InterviewConfig(
                                                userId = user.id,
                                                interviewType = session.interviewType,
                                                questionCount = session.targetQuestionCount,
                                                useBatchMode = useBatchMode,
                                                jobTitle = session.jobTitle,
                                                jobDescription = session.jobDescription,
                                                resumeId = null,
                                                resumeContent = null,
                                                previousSessionId = session.id
                                            )
                                            onStartInterview(config)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Start", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            }
                        }
                    }
                }
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

    if (showSetupDialog) {
        InterviewDetailsDialog(
            onDismiss = { showSetupDialog = false },
            onConfirm = { title, desc ->
                showSetupDialog = false
                val user = Clerk.user
                if (user != null) {
                    val config = InterviewConfig(
                        userId = user.id,
                        interviewType = selectedType,
                        questionCount = questionCount,
                        useBatchMode = useBatchMode,
                        jobTitle = title,
                        jobDescription = desc,
                        resumeId = null,
                        resumeContent = null
                    )
                    onStartInterview(config)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewDetailsDialog(
    initialJobTitle: String = "",
    initialJobDescription: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var jobTitle by remember(initialJobTitle) { mutableStateOf(initialJobTitle) }
    var jobDescription by remember(initialJobDescription) { mutableStateOf(initialJobDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Interview Details") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("Job Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = jobDescription,
                    onValueChange = { jobDescription = it },
                    label = { Text("Job Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(jobTitle, jobDescription)
                },
                enabled = jobTitle.isNotBlank()
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}




