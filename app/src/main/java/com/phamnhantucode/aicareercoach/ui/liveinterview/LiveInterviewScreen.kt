package com.phamnhantucode.aicareercoach.ui.liveinterview

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamnhantucode.aicareercoach.ui.components.CreditExhaustedDialog

// Live interview feature container
@Composable
fun LiveInterviewScreen(
    onBack: () -> Unit,
    onNavigateToPurchase: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: LiveInterviewViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val showCreditDialog by viewModel.showCreditDialog.collectAsState()

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
        is LiveInterviewUiState.AnswerCollection,
        is LiveInterviewUiState.Processing,
        is LiveInterviewUiState.ViewingFeedback,
        is LiveInterviewUiState.LoadingNextQuestion,
        is LiveInterviewUiState.GeneratingBatchFeedback,
        is LiveInterviewUiState.BatchFeedback,
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

    if (showCreditDialog) {
        CreditExhaustedDialog(
            onDismiss = { viewModel.dismissCreditDialog() },
            onPurchase = { 
                viewModel.dismissCreditDialog()
                onNavigateToPurchase() 
            }
        )
    }
}
