package com.phamnhantucode.aicareercoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.phamnhantucode.aicareercoach.ui.login.LoginScreen
import com.phamnhantucode.aicareercoach.ui.onboarding.IntroPage
import com.phamnhantucode.aicareercoach.ui.onboarding.OnboardingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnboardingScreen()
        }
    }
}

private enum class AppDestination {
    Onboarding,
    Login
}

@Composable
fun AiCareerCoachApp() {
    var destination by remember { mutableStateOf(AppDestination.Onboarding) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visible = destination == AppDestination.Onboarding,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IntroPage(
                    onGetStarted = { destination = AppDestination.Login },
                    onSignIn = { destination = AppDestination.Login }
                )
            }

            AnimatedVisibility(
                visible = destination == AppDestination.Login,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LoginScreen(
                    onBackToOnboarding = { destination = AppDestination.Onboarding }
                )
            }
        }
    }
}
