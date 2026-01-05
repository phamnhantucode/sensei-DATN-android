package com.phamnhantucode.aicareercoach.ui.liveinterview

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import com.phamnhantucode.aicareercoach.data.audio.RecordingState

// Active interview screen
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
    val audioAmplitude by viewModel.audioAmplitude.collectAsState()
    val interviewDuration by viewModel.interviewDuration.collectAsState()
    val interviewSession by viewModel.interviewSession.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()



    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Exit confirmation dialog
    var showExitDialog by remember { mutableStateOf(false) }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(text = "Exit Interview?") },
            text = { Text(text = "Are you sure you want to end this interview? Your progress will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        viewModel.abandonInterview()
                        onExit()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    interviewSession?.let { session ->
                        val title = if (session.jobTitle.isNotBlank()) {
                            "Live Interview (${session.jobTitle})"
                        } else {
                            "Live Interview (${session.interviewType.name})"
                        }
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
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
                    LiveInterviewLoadingView(message = "Starting interview...")
                }
                is LiveInterviewUiState.ActiveQuestion -> {
                    currentQuestion?.let { question ->
                        interviewSession?.let { session ->
                            QuestionView(
                                question = question,
                                questionNumber = session.currentQuestionIndex + 1,
                                totalQuestions = session.targetQuestionCount,
                                recordingState = recordingState,
                                amplitude = audioAmplitude,
                                onStartRecording = { viewModel.startRecording() },
                                onStopRecording = { viewModel.stopRecordingAndProcess() }
                            )
                        }
                    }
                }
                is LiveInterviewUiState.AnswerCollection -> {
                    currentQuestion?.let { question ->
                        interviewSession?.let { session ->
                            BatchQuestionView(
                                question = question,
                                questionNumber = session.currentQuestionIndex + 1,
                                totalQuestions = session.targetQuestionCount,
                                recordingState = recordingState,
                                amplitude = audioAmplitude,
                                onStartRecording = { viewModel.startRecording() },
                                onStopRecording = { viewModel.stopRecordingAndProcess() },
                                onPrevious = { viewModel.goToPreviousQuestion() },
                                onNext = { viewModel.continueToNextQuestion() },
                                onExit = { showExitDialog = true }
                            )
                        }
                    }
                }
                is LiveInterviewUiState.ReviewingAnswer -> {
                    val reviewState = uiState as LiveInterviewUiState.ReviewingAnswer
                    currentQuestion?.let { question ->
                        interviewSession?.let { session ->
                            ReviewAnswerView(
                                question = question,
                                questionNumber = session.currentQuestionIndex + 1,
                                totalQuestions = session.targetQuestionCount,
                                initialAnswer = reviewState.transcription,
                                onConfirm = { viewModel.confirmAnswer(it) },
                                onRetake = { viewModel.retakeRecording() }
                            )
                        }
                    }
                }
                is LiveInterviewUiState.Processing -> {
                    LiveInterviewLoadingView(message = "Processing your answer...")
                }
                is LiveInterviewUiState.ViewingFeedback -> {
                    currentFeedback?.let { feedback ->
                        interviewSession?.let { session ->
                            val isLastQuestion = session.currentQuestionIndex >= session.targetQuestionCount - 1
                            FeedbackView(
                                feedback = feedback,
                                expectedAnswer = currentQuestion?.correctAnswer,
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
                    LiveInterviewLoadingView(message = "Loading next question...")
                }
                is LiveInterviewUiState.GeneratingBatchFeedback -> {
                   LiveInterviewLoadingView(message = "Analyzing all your answers...")
                }
                is LiveInterviewUiState.BatchFeedback -> {
                    val batchState = uiState as LiveInterviewUiState.BatchFeedback
                    BatchFeedbackView(
                        questions = batchState.questions,
                        onComplete = { viewModel.completeInterview() }
                    )
                }
                is LiveInterviewUiState.GeneratingSummary -> {
                    LiveInterviewLoadingView(message = "Generating interview summary...")
                }
                is LiveInterviewUiState.Paused -> {
                    PausedView(
                        onResume = { viewModel.resumeInterview() },
                        onExit = { showExitDialog = true }
                    )
                }
                else -> {
                    // Handle other states if needed
                }
            }
        }
    }


}

@Composable
private fun QuestionView(
    question: com.phamnhantucode.aicareercoach.data.interview.LiveQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    recordingState: RecordingState,
    amplitude: Float,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
    }

    var showHint by remember { mutableStateOf(false) }

    if (showHint) {
        AlertDialog(
            onDismissRequest = { showHint = false },
            title = {
                Text(
                    text = "Expected Answer",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = question.correctAnswer.ifBlank { "No expected answer available." },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showHint = false }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: Progress indicator
        InterviewProgressIndicator(
            currentQuestion = questionNumber,
            totalQuestions = totalQuestions
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Body: Scrollable Question Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            // Question card container
            Box(modifier = Modifier.fillMaxWidth()) {
                QuestionCard(
                    question = question,
                    questionNumber = questionNumber,
                    totalQuestions = totalQuestions
                )
                
                // Hint button overlay
                TextButton(
                    onClick = { showHint = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "Hint",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer: Controls (Visualizer + Mic)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Recording hint or Visualizer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (recordingState == RecordingState.RECORDING) {
                        Text(
                            text = "🎤 Recording...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AudioVisualizer(amplitude = amplitude)
                    } else {
                        Text(
                            text = "💡 Tip: Think, then hold to speak",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Push-to-talk button
            PushToTalkButton(
                isRecording = recordingState == RecordingState.RECORDING,
                onStartRecording = {
                    if (hasPermission) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStopRecording()
                },
                enabled = recordingState == RecordingState.IDLE || recordingState == RecordingState.RECORDING
            )
        }
    }
}

@Composable
private fun FeedbackView(
    feedback: QuestionFeedback,
    expectedAnswer: String?,
    isLastQuestion: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Body: Scrollable Feedback
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Feedback card
            FeedbackCard(
                transcription = feedback.transcription,
                expectedAnswer = expectedAnswer,
                feedback = feedback.feedback,
                rating = feedback.rating
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer: Continue Button
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
private fun BatchQuestionView(
    question: com.phamnhantucode.aicareercoach.data.interview.LiveQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    recordingState: RecordingState,
    amplitude: Float,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: Progress indicator
        InterviewProgressIndicator(
            currentQuestion = questionNumber,
            totalQuestions = totalQuestions
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Body: Scrollable Question Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            // Question card
            QuestionCard(
                question = question,
                questionNumber = questionNumber,
                totalQuestions = totalQuestions
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Footer: Controls (Visualizer + Mic + Navigation)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Batch mode hint or Visualizer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (recordingState == RecordingState.RECORDING) {
                        Text(
                            text = "🎤 Recording...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AudioVisualizer(amplitude = amplitude)
                    } else {
                        Text(
                            text = "📝 Answer Collection Mode: No immediate feedback - answer all questions first",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Push-to-talk button
            PushToTalkButton(
                isRecording = recordingState == RecordingState.RECORDING,
                onStartRecording = {
                    if (hasPermission) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStopRecording()
                },
                enabled = recordingState == RecordingState.IDLE || recordingState == RecordingState.RECORDING
            )

            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Button
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = questionNumber > 1,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("Previous")
                }

                // Exit Button
                TextButton(
                    onClick = onExit,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Exit")
                }

                // Next Button
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (questionNumber == totalQuestions) "Finish" else "Next")
                }
            }
        }
    }
}

@Composable
private fun BatchFeedbackView(
    questions: List<com.phamnhantucode.aicareercoach.data.interview.LiveQuestion>,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Body: Scrollable Feedback List
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header information (scrolls with content)
            Text(
                text = "Interview Feedback",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Here's your comprehensive feedback for all questions:",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Overall stats
            val averageRating = questions.mapNotNull { it.rating }.average()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Overall Score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${String.format("%.1f", averageRating)}/10",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Individual question feedback
            questions.forEachIndexed { index, question ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Question ${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = question.questionText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (question.userAnswer != null) {
                            Text(
                                text = "Your Answer:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = question.userAnswer!!,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        if (question.correctAnswer.isNotBlank()) {
                            Text(
                                text = "Expected Answer:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = question.correctAnswer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        if (question.rating != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Score:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${question.rating}/10",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        question.rating >= 8 -> MaterialTheme.colorScheme.primary
                                        question.rating >= 6 -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                        
                        if (question.feedback != null) {
                            Text(
                                text = "Feedback:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = question.feedback!!,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer: Complete button
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Complete Interview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
@Composable
private fun AudioVisualizer(
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 30
    val animAmplitudes = remember { mutableStateListOf<Float>().apply { repeat(barCount) { add(0f) } } }

    // Shift values and add new amplitude
    LaunchedEffect(amplitude) {
        // Simple smoothing
        val effectiveAmp = amplitude.coerceIn(0f, 1f)
        
        // Shift left
        for (i in 0 until barCount - 1) {
            animAmplitudes[i] = animAmplitudes[i+1]
        }
        // Add new value with some random variation for visual interest if sound is present
        animAmplitudes[barCount - 1] = if (effectiveAmp > 0.01f) {
            effectiveAmp * (0.8f + Math.random().toFloat() * 0.4f)
        } else {
            0f
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        animAmplitudes.forEachIndexed { index, value ->
            // Animated bar height
            val animatedHeight by animateFloatAsState(
                targetValue = value.coerceIn(0.1f, 1f),
                animationSpec = tween(100), label = "barHeight"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .width(4.dp)
                    .fillMaxHeight(animatedHeight)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
private fun ReviewAnswerView(
    question: com.phamnhantucode.aicareercoach.data.interview.LiveQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    initialAnswer: String,
    onConfirm: (String) -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    var answerText by remember(initialAnswer) { mutableStateOf(initialAnswer) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: Progress indicator
        InterviewProgressIndicator(
            currentQuestion = questionNumber,
            totalQuestions = totalQuestions
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Body: Scrollable Content (Title + Question + Input)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Review Your Answer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Question card
            QuestionCard(
                question = question,
                questionNumber = questionNumber,
                totalQuestions = totalQuestions
            )

            // Editable answer field
            OutlinedTextField(
                value = answerText,
                onValueChange = { answerText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                label = { Text("Your Answer") },
                placeholder = { Text("Edit your answer here...") },
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer: Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retake")
            }

            Button(
                onClick = { onConfirm(answerText) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (questionNumber == totalQuestions) "Submit Interview" else "Next Question")
            }
        }
    }
}
