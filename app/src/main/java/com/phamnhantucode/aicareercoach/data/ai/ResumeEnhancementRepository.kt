package com.phamnhantucode.aicareercoach.data.ai

import com.clerk.api.Clerk
import com.google.gson.Gson
import com.phamnhantucode.aicareercoach.data.neon.NeonAuth
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService
import com.phamnhantucode.aicareercoach.ui.resumebuilder.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

// Enhance resume with AI
class ResumeEnhancementRepository {

    private val gson = Gson()

    // Enhance resume content using Match Analysis (Web Alignment)
    suspend fun enhanceResume(
        resume: Resume,
        jobDescription: String? = null
    ): EnhancementSuggestions = withContext(Dispatchers.IO) {
        val user = Clerk.user ?: throw IllegalStateException("User session unavailable")
        val authToken = NeonAuth.fetchNeonAuthToken() ?: throw IllegalStateException("Authentication unavailable")
        // val authHeader = "Bearer $authToken" 

        val neonUserId = NeonUserService.fetchNeonUserId(user.id, "Bearer $authToken")
        NeonUserService.deductCredit(neonUserId, 1, "Resume Enhancement", authToken)

        // Serialize resume to JSON for the new prompt
        // We use a simplified version of the resume to save tokens if needed, 
        // but the prompt expects "CANDIDATE_RESUME_JSON".
        val resumeJson = gson.toJson(resume) 
        val jdText = jobDescription ?: "General Software Engineering role" // Fallback if null, though Web requires JD for match analysis.
        
        val prompt = PromptFactory.createMatchAnalysisPrompt(
            resumeJson = resumeJson,
            jobDescription = jdText
        )

        try {
            val messages = listOf(
                OpenRouterService.Message(
                    role = "system",
                    content = "You are an expert ATS Specialist and Resume Strategist."
                ),
                OpenRouterService.Message(
                    role = "user",
                    content = prompt
                )
            )

            val responseText = OpenRouterService.chatCompletion(
                messages = messages,
                temperature = 0.7, // Match Web usually
                responseFormat = OpenRouterService.ResponseFormat(type = "json_object")
            )

            val cleanJson = cleanJsonString(responseText)
            // Parse into the NEW structure
            val matchAnalysisResult = gson.fromJson(cleanJson, MatchAnalysisResult::class.java)
            
            // Map to OLD structure (Adapter)
            return@withContext mapToEnhancementSuggestions(matchAnalysisResult, resume)
            
        } catch (e: Exception) {
            // Rethrow InsufficientCreditException as is, wrap others
            if (e is NeonUserService.InsufficientCreditException) throw e
            throw IOException("Failed to enhance resume with AI: ${e.message}", e)
        }
    }

    private fun mapToEnhancementSuggestions(
        result: MatchAnalysisResult,
        originalResume: Resume
    ): EnhancementSuggestions {
        val suggestions = result.fieldSuggestions

        // Professional Summary
        val summarySuggestion = suggestions.professional_summary?.let {
            SummarySuggestion(
                original = it.current,
                enhanced = it.suggested,
                reason = it.reason
            )
        }

        // Skills
        val skillsSuggestion = suggestions.skills?.let {
            SkillsSuggestion(
                suggested = it.suggested,
                reason = it.reason,
                reorder = null // Web doesn't explicitly reorder, just provides list
            )
        }

        // Experiences
        val experienceSuggestions = suggestions.experiences?.mapNotNull { exp ->
            // Find original by index or try to match? 
            // The prompt returns "index" or we can try to key off something else. 
            // Web prompt says "index". 
            // Assuming 0-based index from the 'experiences' array in the input JSON.
            val originalExp = originalResume.workExperiences.getOrNull(exp.index)
            
            if (originalExp != null) {
                WorkExperienceSuggestion(
                    id = originalExp.id,
                    jobTitle = exp.suggested.title,
                    responsibilities = listOf(
                         ResponsibilitySuggestion(
                             original = originalExp.responsibilities.joinToString("\n"), // Simplified mapping
                             enhanced = exp.suggested.description,
                             reason = exp.reason
                         )
                    )
                )
            } else null
        }

        // Education
        val educationSuggestions = suggestions.educations?.mapNotNull { edu ->
            val originalEdu = originalResume.education.getOrNull(edu.index)
            if (originalEdu != null) {
                EducationSuggestion(
                    id = originalEdu.id,
                    suggestion = "Degree: ${edu.suggested.degree}, Field: ${edu.suggested.field}, Inst: ${edu.suggested.institution}. Reason: ${edu.reason}"
                )
            } else null
        }
        
        // Overall
        val overallFeedback = OverallFeedback(
            strengths = result.generalSuggestions, // Map general suggestions to strengths/improvements usage
            improvements = result.matchAnalysis.missingSkills.required // Use missing skills as improvements
        )

        return EnhancementSuggestions(
            professionalSummary = summarySuggestion,
            skills = skillsSuggestion,
            workExperiences = experienceSuggestions,
            education = educationSuggestions,
            overall = overallFeedback
        )
    }

    private fun buildResumeText(resume: Resume): String {
       // Deprecated but kept if needed by other internal methods (none found)
       return gson.toJson(resume)
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

    // --- NEW Data Classes for Match Analysis (Web Alignment) ---
    data class MatchAnalysisResult(
        val matchAnalysis: MatchAnalysis,
        val generalSuggestions: List<String>,
        val fieldSuggestions: FieldSuggestions
    )

    data class MatchAnalysis(
        val overallScore: Int,
        val verdict: String,
        val missingSkills: MissingSkills,
        val missingExperience: List<String>,
        val notes: String
    )

    data class MissingSkills(
        val required: List<String>,
        val niceToHave: List<String>
    )

    data class FieldSuggestions(
        val professional_summary: FieldSuggestionSingle?,
        val experiences: List<ExperienceSuggestion>?,
        val educations: List<EducationSuggestionInput>?,
        val projects: List<ProjectSuggestion>?,
        val skills: SkillsSuggestionInput?
    )

    data class FieldSuggestionSingle(
        val current: String,
        val suggested: String,
        val reason: String
    )

    data class ExperienceSuggestion(
        val index: Int,
        val suggested: ExperienceContent,
        val reason: String
    )

    data class ExperienceContent(
        val title: String,
        val organization: String,
        val description: String,
        val startDate: String,
        val isCurrent: Boolean
    )
    
    data class EducationSuggestionInput(
        val index: Int,
        val suggested: EducationContent,
        val reason: String
    )
    
    data class EducationContent(
        val institution: String,
        val degree: String,
        val field: String,
        val graduationDate: String
    )
    
    data class ProjectSuggestion(
        val index: Int,
        val suggested: ProjectContent,
        val reason: String
    )
    
    data class ProjectContent(
        val name: String,
        val description: String,
        val type: String
    )

    data class SkillsSuggestionInput(
        val current: List<String>,
        val suggested: List<String>,
        val reason: String
    )

    // --- OLD Data Classes (Preserved for UI Compatibility) ---
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
