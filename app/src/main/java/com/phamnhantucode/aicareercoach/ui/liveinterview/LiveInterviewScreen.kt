package com.phamnhantucode.aicareercoach.ui.liveinterview

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Main container for live interview feature that manages navigation between
 * setup, active interview, and results screens.
 */
@Composable
fun LiveInterviewScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LiveInterviewViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is LiveInterviewUiState.Setup -> {
            LiveInterviewSetupScreen(
                onBack = onBack,
                onStartInterview = { config ->
                    viewModel.startInterview(config)
                }
            )
        }
        is LiveInterviewUiState.Starting,
        is LiveInterviewUiState.ActiveQuestion,
        is LiveInterviewUiState.Processing,
        is LiveInterviewUiState.ViewingFeedback,
        is LiveInterviewUiState.LoadingNextQuestion,
        is LiveInterviewUiState.Paused,
        is LiveInterviewUiState.GeneratingSummary -> {
            LiveInterviewActiveScreen(
                viewModel = viewModel,
                onExit = onBack
            )
        }
        is LiveInterviewUiState.Completed -> {
            LiveInterviewResultsScreen(
                summary = state.summary,
                onBack = onBack,
                onNewInterview = {
                    // Reset to setup
                    viewModel.abandonInterview()
                }
            )
        }
    }
}
