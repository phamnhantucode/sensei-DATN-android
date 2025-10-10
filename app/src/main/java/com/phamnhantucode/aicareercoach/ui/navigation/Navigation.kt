package com.phamnhantucode.aicareercoach.ui.navigation

import kotlinx.serialization.Serializable

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object IndustryInsights : Screen("industry_insights")
}