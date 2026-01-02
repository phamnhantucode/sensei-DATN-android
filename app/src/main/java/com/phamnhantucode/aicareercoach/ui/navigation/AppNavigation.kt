package com.phamnhantucode.aicareercoach.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.phamnhantucode.aicareercoach.data.preferences.PreferencesRepository
import com.phamnhantucode.aicareercoach.data.preferences.ThemeMode
import com.phamnhantucode.aicareercoach.ui.accountsettings.AccountSettingsScreen
import com.phamnhantucode.aicareercoach.ui.components.NetworkAwareContent
import com.phamnhantucode.aicareercoach.ui.coverletter.CoverLetterScreen
import com.phamnhantucode.aicareercoach.ui.coverletter.CoverLetterTypeSelectionScreen
import com.phamnhantucode.aicareercoach.ui.coverletter.CoverLetterInputScreen
import com.phamnhantucode.aicareercoach.ui.coverletter.editor.CoverLetterEditorScreen
import com.phamnhantucode.aicareercoach.ui.industryinsights.IndustryInsightsScreen
import com.phamnhantucode.aicareercoach.ui.interviewprep.InterviewPrepScreen
import com.phamnhantucode.aicareercoach.ui.liveinterview.LiveInterviewScreen
import com.phamnhantucode.aicareercoach.ui.login.LoginScreen
import com.phamnhantucode.aicareercoach.ui.onboarding.IntroPage
import com.phamnhantucode.aicareercoach.ui.onboarding.OnboardingScreen
import com.phamnhantucode.aicareercoach.ui.resumebuilder.ResumeBuilderScreen
import com.phamnhantucode.aicareercoach.ui.resumebuilder.ResumeListScreen
import com.phamnhantucode.aicareercoach.ui.resumebuilder.ResumeMarkdownScreen
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.GridEditorScreen
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.GridEditorViewModel
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumeDesignScreen
import com.phamnhantucode.aicareercoach.ui.purchase.PurchaseScreen
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val preferencesRepository = PreferencesRepository.getInstance(context)
    val themeMode by preferencesRepository.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    val isFirstTime by androidx.compose.runtime.produceState<Boolean?>(initialValue = null, preferencesRepository) {
        preferencesRepository.isFirstTimeFlow.collect { value = it }
    }
    
    // Check if user is logged in via Clerk
    val isLoggedIn = com.clerk.api.Clerk.session != null

    val isSystemInDarkTheme = isSystemInDarkTheme()

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme
    }

    val navController = rememberNavController()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    AppTheme(darkTheme = darkTheme) {
        NetworkAwareContent {
            if (isFirstTime == null) {
                // Show a blank screen or a loading indicator while checking preferences to avoid flash
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                )
            } else {
                // Determine start destination based on state
                val startDestination = when {
                    isLoggedIn -> Screen.IndustryInsights.route
                    isFirstTime == true -> Screen.Intro.route
                    else -> Screen.Login.route
                }

                NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
        composable(Screen.Intro.route) {
            IntroPage(
                onGetStarted = { 
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                       preferencesRepository.setFirstTime(false)
                    }
                    navController.navigate(Screen.IndustryInsights.route) 
                },
                onSignIn = { 
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                       preferencesRepository.setFirstTime(false)
                    }
                    navController.navigate(Screen.Login.route) 
                }
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
                    navController.navigate(Screen.ResumeList.route)
                },
                onNavigateToInterviewPrep = {
                    navController.navigate(Screen.InterviewPrep.route)
                },
                onNavigateToCoverLetter = {
                    navController.navigate(Screen.CoverLetter.route)
                },
                onNavigateToAccountSettings = {
                    navController.navigate(Screen.AccountSettings.route)
                },
                onNavigateToPro = {
                    navController.navigate(Screen.StripePayment.route)
                },
                onNavigateToLiveInterview = {
                    navController.navigate(Screen.LiveInterviewSetup.route)
                }
            )
        }

        composable(Screen.AccountSettings.route) {
            AccountSettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Intro.route) {
                        popUpTo(Screen.Intro.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateToPayment = {
                    navController.navigate(Screen.StripePayment.route)
                }
            )
        }

        composable(Screen.ResumeList.route) {
            ResumeListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToResumeBuilder = { resumeId ->
                    navController.navigate(Screen.ResumeBuilder.buildRoute(resumeId))
                }
            )
        }

        composable(
            route = Screen.ResumeBuilder.routeWithArgs,
            arguments = listOf(
                navArgument(Screen.ResumeBuilder.resumeIdKey()) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val resumeId = backStackEntry.arguments?.getString(Screen.ResumeBuilder.resumeIdKey())

            ResumeBuilderScreen(
                resumeId = resumeId,
                onBack = { navController.popBackStack() },
                onNavigateToGridEditor = { designId, template, isNewDesign, linkedResumeId ->
                    navController.navigate(Screen.GridEditor.buildRoute(designId, template, isNewDesign, linkedResumeId))
                }
            )
        }

        composable(Screen.ResumeMarkdown.route) {
            ResumeMarkdownScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ResumeDesignScreen.route) {
            ResumeDesignScreen(
                onBack = { navController.popBackStack() },
                onNavigateToGridEditor = { designId, template, isNewDesign, linkedResumeId ->
                    navController.navigate(Screen.GridEditor.buildRoute(designId, template, isNewDesign, linkedResumeId))
                }
            )
        }

        composable(
            route = Screen.GridEditor.routeWithArgs,
            arguments = listOf(
                navArgument(Screen.GridEditor.designIdKey()) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Screen.GridEditor.templateKey()) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Screen.GridEditor.isNewDesignKey()) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(Screen.GridEditor.linkedResumeIdKey()) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val designId = backStackEntry.arguments?.getString(Screen.GridEditor.designIdKey())
            val template = backStackEntry.arguments?.getString(Screen.GridEditor.templateKey())
            val isNewDesign = backStackEntry.arguments?.getBoolean(Screen.GridEditor.isNewDesignKey()) ?: false
            val linkedResumeId = backStackEntry.arguments?.getString(Screen.GridEditor.linkedResumeIdKey())

            val viewModel: GridEditorViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return GridEditorViewModel(context, designId, template, isNewDesign, linkedResumeId) as T
                    }
                }
            )
            GridEditorScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = { /* TODO: Navigate to preview if needed */ },
                onSwitchToFormEditor = {
                    // Navigate back to resume builder with the current resume
                    navController.popBackStack()
                },
                viewModel = viewModel,
                onNavigateToPurchase = {
                    navController.navigate(Screen.Purchase.route)
                }
            )
        }

        composable(Screen.InterviewPrep.route) {
            InterviewPrepScreen(
                onBack = { navController.popBackStack() },

                onNavigateToPurchase = { navController.navigate(Screen.Purchase.route) }
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
                },
                onNavigateToTypeSelection = {
                    navController.navigate(Screen.CoverLetterTypeSelection.route)
                }
            )
        }

        composable(Screen.CoverLetterTypeSelection.route) {
            CoverLetterTypeSelectionScreen(
                onBack = { navController.popBackStack() },
                onTypeSelected = { type ->
                    navController.navigate(Screen.CoverLetterInput.buildRoute(type))
                }
            )
        }

        composable(
            route = Screen.CoverLetterInput.routeWithArgs,
            arguments = listOf(
                navArgument(Screen.CoverLetterInput.typeKey()) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString(Screen.CoverLetterInput.typeKey()) ?: "APPLICATION"
            CoverLetterInputScreen(
                type = type,
                onBack = { navController.popBackStack() },
                onCoverLetterGenerated = { entry ->
                     navController.navigate(
                        Screen.CoverLetterEditor.buildRoute(
                            id = entry.id,
                            jobTitle = entry.jobTitle,
                            company = entry.companyName,
                            jobDescription = entry.jobDescription,
                            content = entry.content
                        )
                    ) {
                        popUpTo(Screen.CoverLetter.route) {
                            inclusive = false
                        }
                    }
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
                onBack = { navController.popBackStack() },
                onNavigateToPurchase = { navController.navigate(Screen.Purchase.route) }
            )
        }

        composable(Screen.Purchase.route) {
            PurchaseScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.StripePayment.route) {
            com.phamnhantucode.aicareercoach.ui.payment.PaymentScreen(
                onBack = { navController.popBackStack() }
            )
        }
        }
        }
        }
    }
}
