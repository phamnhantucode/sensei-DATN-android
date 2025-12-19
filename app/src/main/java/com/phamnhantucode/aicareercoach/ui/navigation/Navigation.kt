package com.phamnhantucode.aicareercoach.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Intro : Screen("intro")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object IndustryInsights : Screen("industry_insights")
    object ResumeList : Screen("resume_list")
    object ResumeBuilder : Screen("resume_builder") {
        private const val ResumeIdArg = "resumeId"

        val routeWithArgs: String = "$route?$ResumeIdArg={$ResumeIdArg}"

        fun buildRoute(resumeId: String? = null): String {
            return if (resumeId != null) {
                "$route?$ResumeIdArg=${Uri.encode(resumeId)}"
            } else {
                route
            }
        }

        fun resumeIdKey(): String = ResumeIdArg
    }
    object ResumeMarkdown : Screen("resume_markdown")
    object ResumeDesignScreen : Screen("resume_design_screen")
    object GridEditor : Screen("grid_editor") {
        private const val DesignIdArg = "designId"
        private const val TemplateArg = "template"
        private const val IsNewDesignArg = "isNewDesign"
        private const val LinkedResumeIdArg = "linkedResumeId"

        val routeWithArgs: String =
            "$route?$DesignIdArg={$DesignIdArg}&$TemplateArg={$TemplateArg}&$IsNewDesignArg={$IsNewDesignArg}&$LinkedResumeIdArg={$LinkedResumeIdArg}"

        fun buildRoute(designId: String? = null, template: String? = null, isNewDesign: Boolean = false, linkedResumeId: String? = null): String {
            val params = mutableListOf<String>()
            if (designId != null) {
                params.add("$DesignIdArg=${Uri.encode(designId)}")
            }
            if (template != null) {
                params.add("$TemplateArg=${Uri.encode(template)}")
            }
            params.add("$IsNewDesignArg=$isNewDesign")
            if (linkedResumeId != null) {
                params.add("$LinkedResumeIdArg=${Uri.encode(linkedResumeId)}")
            }
            return if (params.isNotEmpty()) {
                "$route?${params.joinToString("&")}"
            } else {
                route
            }
        }

        fun designIdKey(): String = DesignIdArg
        fun templateKey(): String = TemplateArg
        fun isNewDesignKey(): String = IsNewDesignArg
        fun linkedResumeIdKey(): String = LinkedResumeIdArg
    }
    object InterviewPrep : Screen("interview_prep")
    object CoverLetter : Screen("cover_letter")
    object AccountSettings : Screen("account_settings")
    object LiveInterviewSetup : Screen("live_interview_setup")
    object LiveInterviewActive : Screen("live_interview_active")
    object LiveInterviewResults : Screen("live_interview_results")
    object Purchase : Screen("purchase")
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
