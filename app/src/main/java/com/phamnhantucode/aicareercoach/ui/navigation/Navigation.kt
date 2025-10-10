package com.phamnhantucode.aicareercoach.ui.navigation

sealed class Screen(val route: String) {
    object Intro : Screen("intro")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object IndustryInsights : Screen("industry_insights")
    object ResumeBuilder : Screen("resume_builder")
}
