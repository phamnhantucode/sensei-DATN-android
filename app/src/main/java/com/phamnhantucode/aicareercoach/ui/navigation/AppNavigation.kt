package com.phamnhantucode.aicareercoach.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phamnhantucode.aicareercoach.ui.industryinsights.IndustryInsightsScreen
import com.phamnhantucode.aicareercoach.ui.login.LoginScreen
import com.phamnhantucode.aicareercoach.ui.onboarding.IntroPage
import com.phamnhantucode.aicareercoach.ui.onboarding.OnboardingScreen
import com.phamnhantucode.aicareercoach.ui.resumebuilder.ResumeBuilderScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Intro.route
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
                }
            )
        }

        composable(Screen.IndustryInsights.route) {
            IndustryInsightsScreen(
                onNavigateToResumeBuilder = {
                    navController.navigate(Screen.ResumeBuilder.route)
                }
            )
        }

        composable(Screen.ResumeBuilder.route) {
            ResumeBuilderScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
