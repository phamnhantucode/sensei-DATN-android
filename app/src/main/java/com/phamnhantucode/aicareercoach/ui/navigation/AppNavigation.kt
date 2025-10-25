package com.phamnhantucode.aicareercoach.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.phamnhantucode.aicareercoach.ui.accountsettings.AccountSettingsScreen
import com.phamnhantucode.aicareercoach.ui.coverletter.CoverLetterScreen
import com.phamnhantucode.aicareercoach.ui.coverletter.editor.CoverLetterEditorScreen
import com.phamnhantucode.aicareercoach.ui.industryinsights.IndustryInsightsScreen
import com.phamnhantucode.aicareercoach.ui.interviewprep.InterviewPrepScreen
import com.phamnhantucode.aicareercoach.ui.login.LoginScreen
import com.phamnhantucode.aicareercoach.ui.onboarding.IntroPage
import com.phamnhantucode.aicareercoach.ui.onboarding.OnboardingScreen
import com.phamnhantucode.aicareercoach.ui.resumebuilder.ResumeBuilderScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

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
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ResumeBuilder.route) {
            ResumeBuilderScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.InterviewPrep.route) {
            InterviewPrepScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CoverLetter.route) {
            CoverLetterScreen(
                onBack = { navController.popBackStack() },
                onOpenEditor = { entry ->
                    navController.navigate(
                        Screen.CoverLetterEditor.buildRoute(
                            jobTitle = entry.jobTitle,
                            company = entry.companyName,
                            jobDescription = entry.jobDescription
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.CoverLetterEditor.routeWithArgs,
            arguments = listOf(
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
                }
            )
        ) { backStackEntry ->
            val jobTitle = backStackEntry.arguments?.getString(Screen.CoverLetterEditor.jobTitleKey()).orEmpty()
            val companyName = backStackEntry.arguments?.getString(Screen.CoverLetterEditor.companyKey()).orEmpty()
            val jobDescription = backStackEntry.arguments?.getString(Screen.CoverLetterEditor.jobDescriptionKey()).orEmpty()

            CoverLetterEditorScreen(
                jobTitle = jobTitle,
                companyName = companyName,
                jobDescription = jobDescription,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
