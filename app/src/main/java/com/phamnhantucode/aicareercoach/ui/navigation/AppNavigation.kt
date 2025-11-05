package com.phamnhantucode.aicareercoach.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.phamnhantucode.aicareercoach.data.preferences.PreferencesRepository
import com.phamnhantucode.aicareercoach.data.preferences.ThemeMode
import com.phamnhantucode.aicareercoach.ui.accountsettings.AccountSettingsScreen
import com.phamnhantucode.aicareercoach.ui.coverletter.CoverLetterScreen
import com.phamnhantucode.aicareercoach.ui.coverletter.editor.CoverLetterEditorScreen
import com.phamnhantucode.aicareercoach.ui.industryinsights.IndustryInsightsScreen
import com.phamnhantucode.aicareercoach.ui.interviewprep.InterviewPrepScreen
import com.phamnhantucode.aicareercoach.ui.liveinterview.LiveInterviewScreen
import com.phamnhantucode.aicareercoach.ui.login.LoginScreen
import com.phamnhantucode.aicareercoach.ui.onboarding.IntroPage
import com.phamnhantucode.aicareercoach.ui.onboarding.OnboardingScreen
import com.phamnhantucode.aicareercoach.ui.resumebuilder.ResumeBuilderScreen
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.GridEditorScreen
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.GridEditorViewModel
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val preferencesRepository = PreferencesRepository.getInstance(context)
    val themeMode by preferencesRepository.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    val isSystemInDarkTheme = isSystemInDarkTheme()

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme
    }

    val navController = rememberNavController()

    AppTheme(darkTheme = darkTheme) {
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route
        ) {
        composable(Screen.Intro.route) {
            IntroPage(
                onGetStarted = { navController.navigate(Screen.Onboarding.route) },
                onSignIn = { navController.navigate(Screen.Login.route) }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    // Navigate to Industry Insights screen
                    navController.navigate(Screen.IndustryInsights.route) {
                        // Remove intro and onboarding from back stack once completed
                        popUpTo(Screen.Intro.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onSignedIn = {
                    navController.navigate(Screen.IndustryInsights.route) {
                        popUpTo(Screen.Intro.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.IndustryInsights.route) {
            IndustryInsightsScreen(
                onNavigateToResumeBuilder = {
                    navController.navigate(Screen.ResumeBuilder.route)
                },
                onNavigateToInterviewPrep = {
                    navController.navigate(Screen.InterviewPrep.route)
                },
                onNavigateToCoverLetter = {
                    navController.navigate(Screen.CoverLetter.route)
                },
                onNavigateToAccountSettings = {
                    navController.navigate(Screen.AccountSettings.route)
                }
            )
        }

        composable(Screen.AccountSettings.route) {
            AccountSettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.ResumeBuilder.route) {
            ResumeBuilderScreen(
                onBack = { navController.popBackStack() },
                onNavigateToGridEditor = {
                    navController.navigate(Screen.GridEditor.route)
                }
            )
        }

        composable(Screen.GridEditor.route) {
            val viewModel: GridEditorViewModel = remember { GridEditorViewModel(context) }
            GridEditorScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = { /* TODO: Navigate to preview if needed */ },
                onSwitchToFormEditor = {
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }

        composable(Screen.InterviewPrep.route) {
            InterviewPrepScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLiveInterview = {
                    navController.navigate(Screen.LiveInterviewSetup.route)
                }
            )
        }

        composable(Screen.CoverLetter.route) {
            CoverLetterScreen(
                onBack = { navController.popBackStack() },
                onOpenEditor = { entry ->
                    navController.navigate(
                        Screen.CoverLetterEditor.buildRoute(
                            id = entry.id,
                            jobTitle = entry.jobTitle,
                            company = entry.companyName,
                            jobDescription = entry.jobDescription,
                            content = entry.content
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.CoverLetterEditor.routeWithArgs,
            arguments = listOf(
                navArgument(Screen.CoverLetterEditor.idKey()) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(Screen.CoverLetterEditor.jobTitleKey()) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(Screen.CoverLetterEditor.companyKey()) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(Screen.CoverLetterEditor.jobDescriptionKey()) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(Screen.CoverLetterEditor.contentKey()) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString(Screen.CoverLetterEditor.idKey()).orEmpty()
            val jobTitle = backStackEntry.arguments?.getString(Screen.CoverLetterEditor.jobTitleKey()).orEmpty()
            val companyName = backStackEntry.arguments?.getString(Screen.CoverLetterEditor.companyKey()).orEmpty()
            val jobDescription = backStackEntry.arguments?.getString(Screen.CoverLetterEditor.jobDescriptionKey()).orEmpty()
            val content = backStackEntry.arguments?.getString(Screen.CoverLetterEditor.contentKey()).orEmpty()

            CoverLetterEditorScreen(
                coverLetterId = id,
                jobTitle = jobTitle,
                companyName = companyName,
                jobDescription = jobDescription,
                initialGeneratedContent = content,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LiveInterviewSetup.route) {
            LiveInterviewScreen(
                onBack = { navController.popBackStack() }
            )
        }
        }
    }
}
