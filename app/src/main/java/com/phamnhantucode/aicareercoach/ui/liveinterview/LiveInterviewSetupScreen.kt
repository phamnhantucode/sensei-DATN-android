package com.phamnhantucode.aicareercoach.ui.liveinterview

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.data.interview.InterviewType

private const val TAG = "LiveInterviewSetupScreen"

/**
 * Setup screen for configuring live interview parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveInterviewSetupScreen(
    onBack: () -> Unit,
    onStartInterview: (InterviewConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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

    var selectedType by remember { mutableStateOf(InterviewType.GENERAL) }
    var questionCount by remember { mutableIntStateOf(10) }
    var showPermissionDialog by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(24.dp)
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
                        tint = MaterialTheme.colorScheme.primary
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

            // Interview type selection
            Text(
                text = "Interview Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            InterviewTypeSelector(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it }
            )

            // Question count
            Text(
                text = "Number of Questions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            QuestionCountSelector(
                count = questionCount,
                onCountChanged = { questionCount = it }
            )

            // Interview mode selection
            var useBatchMode by remember { mutableStateOf(true) }
            
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
            Button(
                onClick = {
                    Log.d(TAG, "Start Interview button clicked")
                    Log.d(TAG, "hasAudioPermission=$hasAudioPermission")

                    if (hasAudioPermission) {
                        val user = Clerk.user
                        Log.d(TAG, "Clerk.user=${if (user != null) "ID:${user.id}" else "NULL"}")

                        if (user != null) {
                            val config = InterviewConfig(
                                userId = user.id,
                                interviewType = selectedType,
                                questionCount = questionCount,
                                useBatchMode = useBatchMode
                            )
                            Log.d(TAG, "Calling onStartInterview with config: userId=${config.userId}, type=${config.interviewType}, count=${config.questionCount}, batchMode=${config.useBatchMode}")
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
                enabled = hasAudioPermission
            ) {
                Text(
                    text = "Start Interview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
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

@Composable
private fun InterviewTypeSelector(
    selectedType: InterviewType,
    onTypeSelected: (InterviewType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InterviewType.entries.forEach { type ->
            InterviewTypeCard(
                type = type,
                isSelected = selectedType == type,
                onClick = { onTypeSelected(type) }
            )
        }
    }
}

@Composable
private fun InterviewTypeCard(
    type: InterviewType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, description) = when (type) {
        InterviewType.TECHNICAL -> "Technical" to "Focus on technical skills and problem-solving"
        InterviewType.BEHAVIORAL -> "Behavioral" to "Focus on past experiences and soft skills"
        InterviewType.GENERAL -> "General" to "Mix of technical and behavioral questions"
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuestionCountSelector(
    count: Int,
    onCountChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$count questions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "~${count * 2}-${count * 3} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Slider(
                value = count.toFloat(),
                onValueChange = { onCountChanged(it.toInt()) },
                valueRange = 5f..20f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("5", style = MaterialTheme.typography.bodySmall)
                Text("20", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
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
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
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
                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
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
