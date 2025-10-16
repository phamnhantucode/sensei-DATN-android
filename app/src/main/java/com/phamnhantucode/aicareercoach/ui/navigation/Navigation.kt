package com.phamnhantucode.aicareercoach.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Intro : Screen("intro")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object IndustryInsights : Screen("industry_insights")
    object ResumeBuilder : Screen("resume_builder")
    object InterviewPrep : Screen("interview_prep")
    object CoverLetter : Screen("cover_letter")
    object AccountSettings : Screen("account_settings")
    object CoverLetterEditor : Screen("cover_letter_editor") {
        private const val JobTitleArg = "jobTitle"
        private const val CompanyArg = "company"
        private const val JobDescriptionArg = "jobDescription"

        val routeWithArgs: String =
            "$route?$JobTitleArg={$JobTitleArg}&$CompanyArg={$CompanyArg}&$JobDescriptionArg={$JobDescriptionArg}"

        fun buildRoute(jobTitle: String, company: String, jobDescription: String): String {
            val encodedJobTitle = Uri.encode(jobTitle)
            val encodedCompany = Uri.encode(company)
            val encodedDescription = Uri.encode(jobDescription)
            return "$route?$JobTitleArg=$encodedJobTitle&$CompanyArg=$encodedCompany&$JobDescriptionArg=$encodedDescription"
        }

        fun jobTitleKey(): String = JobTitleArg
        fun companyKey(): String = CompanyArg
        fun jobDescriptionKey(): String = JobDescriptionArg
    }
}
