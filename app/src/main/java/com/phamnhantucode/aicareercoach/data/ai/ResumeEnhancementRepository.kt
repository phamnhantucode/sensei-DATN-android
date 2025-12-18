package com.phamnhantucode.aicareercoach.data.ai

import com.google.gson.Gson
import com.phamnhantucode.aicareercoach.ui.resumebuilder.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

// Enhance resume with AI
class ResumeEnhancementRepository {

    private val gson = Gson()

    // Enhance resume content
    suspend fun enhanceResume(
        resume: Resume,
        jobDescription: String? = null
    ): EnhancementSuggestions = withContext(Dispatchers.IO) {
        val prompt = buildEnhancementPrompt(resume, jobDescription)

        try {
            val messages = listOf(
                OpenRouterService.Message(
                    role = "system",
                    content = "You are an expert resume writer and career coach. Your job is to suggest improvements to resumes."
                ),
                OpenRouterService.Message(
                    role = "user",
                    content = prompt
                )
            )

            val responseText = OpenRouterService.chatCompletion(
                messages = messages,
                temperature = 0.7,
                responseFormat = OpenRouterService.ResponseFormat(type = "json_object")
            )

            val cleanJson = cleanJsonString(responseText)
            val suggestions = gson.fromJson(cleanJson, EnhancementSuggestions::class.java)
            
            return@withContext suggestions
            
        } catch (e: Exception) {
            throw IOException("Failed to enhance resume with AI: ${e.message}", e)
        }
    }

    private fun buildEnhancementPrompt(resume: Resume, jobDescription: String?): String {
        val jdContext = if (!jobDescription.isNullOrBlank()) {
            """
            
            Job Description Context:
            ----------------
            $jobDescription
            ----------------
            
            Please tailor the enhancements to align with this job description where appropriate.
            """
        } else {
            ""
        }

        val resumeText = buildResumeText(resume)

        return """
            Analyze the following resume and suggest improvements for each section.
            Provide specific, actionable enhancements that will make the resume stronger.
            $jdContext
            
            Current Resume:
            ----------------
            $resumeText
            ----------------
            
            Return a JSON object with this exact structure:
            {
              "personalInfo": {
                "profession": "Enhanced profession/title suggestion (if applicable)",
                "summary": "Brief note on personal info improvements"
              },
              "professionalSummary": {
                "original": "Current summary text",
                "enhanced": "Enhanced professional summary (2-3 sentences, impactful)",
                "reason": "Why this enhancement is better"
              },
              "workExperiences": [
                {
                  "id": "work experience id",
                  "jobTitle": "Enhanced job title (if needed)",
                  "responsibilities": [
                    {
                      "original": "Original responsibility",
                      "enhanced": "Enhanced responsibility with metrics and impact",
                      "reason": "Why this is better"
                    }
                  ]
                }
              ],
              "education": [
                {
                  "id": "education id",
                  "suggestion": "Suggestions for education entry (e.g., add GPA, achievements)"
                }
              ],
              "skills": {
                "suggested": ["skill1", "skill2"],
                "reorder": ["skill in priority order"],
                "reason": "Why these skills or ordering"
              },
              "overall": {
                "strengths": ["strength1", "strength2"],
                "improvements": ["improvement1", "improvement2"]
              }
            }
            
            IMPORTANT:
            - Be specific and actionable
            - Focus on impact and achievements
            - Use strong action verbs
            - Quantify where possible
            - Keep enhancements professional and concise
            - Only suggest changes that genuinely improve the content
            - Return ONLY the JSON object
        """.trimIndent()
    }

    private fun buildResumeText(resume: Resume): String {
        return buildString {
            appendLine("PERSONAL INFO:")
            appendLine("Name: ${resume.personalInfo.fullName}")
            if (resume.personalInfo.profession.isNotBlank()) {
                appendLine("Profession: ${resume.personalInfo.profession}")
            }
            appendLine("Email: ${resume.personalInfo.email}")
            appendLine("Phone: ${resume.personalInfo.phone}")
            appendLine("Location: ${resume.personalInfo.location}")
            if (resume.personalInfo.linkedIn.isNotBlank()) {
                appendLine("LinkedIn: ${resume.personalInfo.linkedIn}")
            }
            
            appendLine()
            appendLine("PROFESSIONAL SUMMARY:")
            appendLine(resume.professionalSummary.ifBlank { "[No summary provided]" })
            
            if (resume.workExperiences.isNotEmpty()) {
                appendLine()
                appendLine("WORK EXPERIENCE:")
                resume.workExperiences.forEach { exp ->
                    appendLine("- ${exp.jobTitle} at ${exp.company}")
                    appendLine("  ID: ${exp.id}")
                    exp.responsibilities.forEach { resp ->
                        appendLine("  • $resp")
                    }
                }
            }
            
            if (resume.education.isNotEmpty()) {
                appendLine()
                appendLine("EDUCATION:")
                resume.education.forEach { edu ->
                    appendLine("- ${edu.degree} at ${edu.institution}")
                    appendLine("  ID: ${edu.id}")
                }
            }
            
            if (resume.skills.isNotEmpty()) {
                appendLine()
                appendLine("SKILLS:")
                appendLine(resume.skills.joinToString(", "))
            }
        }
    }

    private fun cleanJsonString(json: String): String {
        var clean = json.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substring(7)
        }
        if (clean.startsWith("```")) {
            clean = clean.substring(3)
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length - 3)
        }
        return clean.trim()
    }

    data class EnhancementSuggestions(
        val personalInfo: PersonalInfoSuggestion? = null,
        val professionalSummary: SummarySuggestion? = null,
        val workExperiences: List<WorkExperienceSuggestion>? = null,
        val education: List<EducationSuggestion>? = null,
        val skills: SkillsSuggestion? = null,
        val overall: OverallFeedback? = null
    )

    data class PersonalInfoSuggestion(
        val profession: String? = null,
        val summary: String? = null
    )

    data class SummarySuggestion(
        val original: String? = null,
        val enhanced: String,
        val reason: String? = null
    )

    data class WorkExperienceSuggestion(
        val id: String,
        val jobTitle: String? = null,
        val responsibilities: List<ResponsibilitySuggestion>? = null
    )

    data class ResponsibilitySuggestion(
        val original: String,
        val enhanced: String,
        val reason: String? = null
    )

    data class EducationSuggestion(
        val id: String,
        val suggestion: String
    )

    data class SkillsSuggestion(
        val suggested: List<String>? = null,
        val reorder: List<String>? = null,
        val reason: String? = null
    )

    data class OverallFeedback(
        val strengths: List<String>? = null,
        val improvements: List<String>? = null
    )
}
