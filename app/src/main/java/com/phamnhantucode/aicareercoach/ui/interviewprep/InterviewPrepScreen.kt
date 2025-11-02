package com.phamnhantucode.aicareercoach.ui.interviewprep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamnhantucode.aicareercoach.ui.components.InsetAwareColumn
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.ui.platform.LocalContext

// Screen states
private enum class InterviewPrepScreen {
    HOME,
    QUIZ_ACTIVE,
    QUIZ_RESULTS,
    QUIZ_HISTORY_RESULTS,
    INTERVIEW_ACTIVE,
    INTERVIEW_RESULTS,
    PROGRESS,
    TIPS
}

@Composable
fun InterviewPrepScreen(
    onBack: () -> Unit = {},
    viewModel: InterviewPrepViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    ),
) {
    var currentScreen by remember { mutableStateOf(InterviewPrepScreen.HOME) }
    var selectedQuizState by remember { mutableStateOf<QuizState?>(null) }
    val quizState by viewModel.quizState.collectAsStateWithLifecycle()
    val interviewState by viewModel.interviewState.collectAsStateWithLifecycle()
    val userProgress by viewModel.userProgress.collectAsStateWithLifecycle()
    val practiceTips by viewModel.practiceTips.collectAsStateWithLifecycle()
    val coachingNotes by viewModel.coachingNotes.collectAsStateWithLifecycle()
    val loadingState by viewModel.loadingState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                InterviewPrepScreen.HOME -> HomeScreen(
                    userProgress = userProgress,
                    onStartQuiz = {
                        viewModel.startQuiz()
                        currentScreen = InterviewPrepScreen.QUIZ_ACTIVE
                    },
                    onStartInterview = {
                        viewModel.startInterview()
                        currentScreen = InterviewPrepScreen.INTERVIEW_ACTIVE
                    },
                    onNavigateToProgress = { currentScreen = InterviewPrepScreen.PROGRESS },
                    onNavigateToTips = { currentScreen = InterviewPrepScreen.TIPS },
                    onQuizClick = {
                        selectedQuizState = it
                        currentScreen = InterviewPrepScreen.QUIZ_HISTORY_RESULTS
                    },
                    onBack = onBack
                )

                InterviewPrepScreen.QUIZ_ACTIVE -> quizState?.let { state ->
                    QuizActiveScreen(
                        quizState = state,
                        onAnswer = viewModel::answerQuizQuestion,
                        onNext = viewModel::nextQuizQuestion,
                        onBack = {
                            viewModel.resetQuiz()
                            currentScreen = InterviewPrepScreen.HOME
                        }
                    )
                    if (state.isComplete) {
                        currentScreen = InterviewPrepScreen.QUIZ_RESULTS
                    }
                }

                InterviewPrepScreen.QUIZ_RESULTS -> quizState?.let { state ->
                    QuizResultsScreen(
                        quizState = state,
                        onRetakeQuiz = {
                            viewModel.startQuiz()
                            currentScreen = InterviewPrepScreen.QUIZ_ACTIVE
                        },
                        onBackToHome = {
                            viewModel.resetQuiz()
                            currentScreen = InterviewPrepScreen.HOME
                        }
                    )
                }

                InterviewPrepScreen.QUIZ_HISTORY_RESULTS -> selectedQuizState?.let { state ->
                    QuizResultsScreen(
                        quizState = state,
                        onRetakeQuiz = {
                            viewModel.startQuiz()
                            currentScreen = InterviewPrepScreen.QUIZ_ACTIVE
                        },
                        onBackToHome = {
                            currentScreen = InterviewPrepScreen.HOME
                        }
                    )
                }

                InterviewPrepScreen.INTERVIEW_ACTIVE -> interviewState?.let { state ->
                    InterviewActiveScreen(
                        interviewState = state,
                        onAnswer = viewModel::answerInterviewQuestion,
                        onNext = viewModel::nextInterviewQuestion,
                        onBack = {
                            viewModel.resetInterview()
                            currentScreen = InterviewPrepScreen.HOME
                        }
                    )
                    if (state.isComplete) {
                        currentScreen = InterviewPrepScreen.INTERVIEW_RESULTS
                    }
                }

                InterviewPrepScreen.INTERVIEW_RESULTS -> interviewState?.let { state ->
                    InterviewResultsScreen(
                        interviewState = state,
                        onRetakeInterview = {
                            viewModel.startInterview()
                            currentScreen = InterviewPrepScreen.INTERVIEW_ACTIVE
                        },
                        onBackToHome = {
                            viewModel.resetInterview()
                            currentScreen = InterviewPrepScreen.HOME
                        }
                    )
                }

                InterviewPrepScreen.PROGRESS -> ProgressScreen(
                    userProgress = userProgress,
                    onBack = { currentScreen = InterviewPrepScreen.HOME }
                )

                InterviewPrepScreen.TIPS -> TipsScreen(
                    practiceTips = practiceTips,
                    coachingNotes = coachingNotes,
                    onBack = { currentScreen = InterviewPrepScreen.HOME }
                )
            }

            if (loadingState.isLoading) {
                LoadingDialog(
                    progress = loadingState.progress,
                    description = loadingState.description
                )
            }

            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::acknowledgeError) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    userProgress: UserProgress,
    onStartQuiz: () -> Unit,
    onStartInterview: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToTips: () -> Unit,
    onQuizClick: (QuizState) -> Unit,
    onBack: () -> Unit,
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
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column {
                        Text(
                            text = "Interview Prep",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Master your next interview",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

        // Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEvents,
                label = "Avg Score",
                value = "${userProgress.averageScore}%"
            )
            StatsCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CalendarToday,
                label = "Streak",
                value = "${userProgress.streak} days"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionCard(
                title = "Quick Quiz",
                subtitle = "10 questions • Multiple choice",
                icon = Icons.Default.PlayArrow,
                color = MaterialTheme.colorScheme.primary,
                onClick = onStartQuiz
            )

            // Full Interview feature hidden as per requirements
            // ActionCard(
            //     title = "Full Interview",
            //     subtitle = "Mock interview • 30 min",
            //     icon = Icons.Default.Psychology,
            //     color = MaterialTheme.colorScheme.secondary,
            //     onClick = onStartInterview
            // )
        }

        Spacer(modifier = Modifier.height(24.dp))

        RecentQuizzesSection(
            userProgress = userProgress,
            onStartQuiz = onStartQuiz,
            onQuizClick = onQuizClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Performance
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Last 8 quizzes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Score bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    userProgress.recentScores.forEach { score ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight((score / 100f).coerceIn(0.1f, 1f))
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Week 1",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Week 2",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsInfoCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.MenuBook,
                label = "Questions",
                value = "${userProgress.questionsAnswered}",
                subtitle = "Practiced"
            )
            StatsInfoCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEvents,
                label = "Quizzes",
                value = "${userProgress.totalQuizzes}",
                subtitle = "Completed"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
//            BottomNavItem(
//                icon = Icons.Default.Home,
//                label = "Home",
//                selected = true,
//                onClick = {}
//            )
//            BottomNavItem(
//                icon = Icons.Default.TrendingUp,
//                label = "Progress",
//                selected = false,
//                onClick = onNavigateToProgress
//            )
//            BottomNavItem(
//                icon = Icons.Default.Lightbulb,
//                label = "Tips",
//                selected = false,
//                onClick = onNavigateToTips
//            )
        }

            Spacer(
                modifier = Modifier.height(
                    16.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
                )
            )
        }
    }
}

@Composable
private fun StatsCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentQuizzesSection(
    userProgress: UserProgress,
    onStartQuiz: () -> Unit,
    onQuizClick: (QuizState) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Recent Quizzes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Review your past performance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onStartQuiz) {
                Text(text = "Start new Quiz")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            userProgress.completedQuizStates.forEach {
                QuizItem(quiz = it, onClick = { onQuizClick(it) })
            }
        }
    }
}

@Composable
private fun QuizItem(quiz: QuizState, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Quiz #${quiz.timeStarted.toLocalDate()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Score: ${quiz.finalScore}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Completed on: ${quiz.timeStarted.toLocalDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Target:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                quiz.questions.map { it.category }.distinct().forEach {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsInfoCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subtitle: String,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingDialog(
    progress: Float,
    description: String,
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Loading Interview Prep",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val mockProgress = UserProgress(
        totalQuizzes = 12,
        averageScore = 78.5,
        questionsAnswered = 145,
        streak = 5,
        recentScores = listOf(75, 80, 72, 85, 78, 82, 79, 88)
    )
    AppTheme {
        HomeScreen(
            userProgress = mockProgress,
            onStartQuiz = {},
            onStartInterview = {},
            onNavigateToProgress = {},
            onNavigateToTips = {},
            onQuizClick = {},
            onBack = {}
        )
    }
}
