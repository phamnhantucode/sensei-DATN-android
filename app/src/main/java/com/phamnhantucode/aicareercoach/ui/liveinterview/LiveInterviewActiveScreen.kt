package com.phamnhantucode.aicareercoach.ui.liveinterview

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.data.audio.RecordingState

/**
 * Main screen for active live interview with push-to-talk and real-time feedback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveInterviewActiveScreen(
    viewModel: LiveInterviewViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val currentFeedback by viewModel.currentFeedback.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val interviewDuration by viewModel.interviewDuration.collectAsState()
    val interviewSession by viewModel.interviewSession.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }
    var showPauseDialog by remember { mutableStateOf(false) }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    interviewSession?.let { session ->
                        Text("Live Interview (${session.interviewType.name})")
                    }
                },
                actions = {
                    // Timer
                    TimerDisplay(durationSeconds = interviewDuration)

                    Spacer(modifier = Modifier.width(16.dp))

                    // Pause button
                    IconButton(onClick = { showPauseDialog = true }) {
                        Icon(Icons.Filled.Pause, contentDescription = "Pause")
                    }

                    // Exit button
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.Filled.Close, contentDescription = "Exit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is LiveInterviewUiState.Starting -> {
                    LoadingView(message = "Starting interview...")
                }
                is LiveInterviewUiState.ActiveQuestion -> {
                    currentQuestion?.let { question ->
                        interviewSession?.let { session ->
                            QuestionView(
                                question = question,
                                questionNumber = session.currentQuestionIndex + 1,
                                totalQuestions = session.targetQuestionCount,
                                recordingState = recordingState,
                                onStartRecording = { viewModel.startRecording() },
                                onStopRecording = { viewModel.stopRecordingAndProcess() }
                            )
                        }
                    }
                }
                is LiveInterviewUiState.Processing -> {
                    LoadingView(message = "Processing your answer...")
                }
                is LiveInterviewUiState.ViewingFeedback -> {
                    currentFeedback?.let { feedback ->
                        interviewSession?.let { session ->
                            val isLastQuestion = session.currentQuestionIndex >= session.targetQuestionCount - 1
                            FeedbackView(
                                feedback = feedback,
                                isLastQuestion = isLastQuestion,
                                onContinue = {
                                    if (isLastQuestion) {
                                        viewModel.completeInterview()
                                    } else {
                                        viewModel.continueToNextQuestion()
                                    }
                                }
                            )
                        }
                    }
                }
                is LiveInterviewUiState.LoadingNextQuestion -> {
                    LoadingView(message = "Loading next question...")
                }
                is LiveInterviewUiState.GeneratingSummary -> {
                    LoadingView(message = "Generating interview summary...")
                }
                is LiveInterviewUiState.Paused -> {
                    PausedView(
                        onResume = { viewModel.resumeInterview() },
                        onExit = { viewModel.abandonInterview(); onExit() }
                    )
                }
                else -> {
                    // Handle other states if needed
                }
            }
        }
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Interview?") },
            text = { Text("Your progress will be lost if you exit now. Are you sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        viewModel.abandonInterview()
                        onExit()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continue Interview")
                }
            }
        )
    }

    // Pause confirmation dialog
    if (showPauseDialog) {
        AlertDialog(
            onDismissRequest = { showPauseDialog = false },
            title = { Text("Pause Interview?") },
            text = { Text("You can resume the interview from where you left off.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPauseDialog = false
                        viewModel.pauseInterview()
                    }
                ) {
                    Text("Pause")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPauseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QuestionView(
    question: com.phamnhantucode.aicareercoach.data.interview.LiveQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    recordingState: RecordingState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Progress indicator
        InterviewProgressIndicator(
            currentQuestion = questionNumber,
            totalQuestions = totalQuestions
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Question card
        QuestionCard(
            question = question,
            questionNumber = questionNumber,
            totalQuestions = totalQuestions
        )

        // Recording hint
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (recordingState == RecordingState.RECORDING) {
                    "🎤 Recording... Speak clearly and release when done"
                } else {
                    "💡 Tip: Think about your answer, then hold the microphone button to speak"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Push-to-talk button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PushToTalkButton(
                isRecording = recordingState == RecordingState.RECORDING,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                enabled = recordingState != RecordingState.STOPPED
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FeedbackView(
    feedback: QuestionFeedback,
    isLastQuestion: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Feedback card
        FeedbackCard(
            transcription = feedback.transcription,
            feedback = feedback.feedback,
            rating = feedback.rating
        )

        Spacer(modifier = Modifier.weight(1f))

        // Continue button
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isLastQuestion) "Complete Interview" else "Continue to Next Question",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PausedView(
    onResume: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Interview Paused",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Take your time. Resume when you're ready.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onResume,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Resume Interview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Exit Interview",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun LoadingView(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
