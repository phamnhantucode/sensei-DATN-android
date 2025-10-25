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
        private const val IdArg = "id"
        private const val JobTitleArg = "jobTitle"
        private const val CompanyArg = "company"
        private const val JobDescriptionArg = "jobDescription"
        private const val ContentArg = "content"

        val routeWithArgs: String =
            "$route?$IdArg={$IdArg}&$JobTitleArg={$JobTitleArg}&$CompanyArg={$CompanyArg}&$JobDescriptionArg={$JobDescriptionArg}&$ContentArg={$ContentArg}"

        fun buildRoute(id: String, jobTitle: String, company: String, jobDescription: String, content: String = ""): String {
            val encodedId = Uri.encode(id)
            val encodedJobTitle = Uri.encode(jobTitle)
            val encodedCompany = Uri.encode(company)
            val encodedDescription = Uri.encode(jobDescription)
            val encodedContent = Uri.encode(content)
            return "$route?$IdArg=$encodedId&$JobTitleArg=$encodedJobTitle&$CompanyArg=$encodedCompany&$JobDescriptionArg=$encodedDescription&$ContentArg=$encodedContent"
        }

        fun idKey(): String = IdArg
        fun jobTitleKey(): String = JobTitleArg
        fun companyKey(): String = CompanyArg
        fun jobDescriptionKey(): String = JobDescriptionArg
        fun contentKey(): String = ContentArg
    }
}
