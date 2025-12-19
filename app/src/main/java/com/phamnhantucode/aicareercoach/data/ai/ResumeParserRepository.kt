package com.phamnhantucode.aicareercoach.data.ai

import com.clerk.api.Clerk
import com.google.gson.Gson
import com.phamnhantucode.aicareercoach.data.ai.OpenRouterService
import com.phamnhantucode.aicareercoach.data.neon.NeonAuth
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService
import com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

// Parse text to Resume
class ResumeParserRepository {

    private val gson = Gson()

    // Parse resume text
    suspend fun parseResume(rawText: String): Resume = withContext(Dispatchers.IO) {
        val user = Clerk.user ?: throw IllegalStateException("User session unavailable")
        val authToken = NeonAuth.fetchNeonAuthToken() ?: throw IllegalStateException("Authentication unavailable")
        val authHeader = "Bearer $authToken"

        try {
            val neonUserId = NeonUserService.fetchNeonUserId(user.id, authHeader)
            NeonUserService.deductCredit(neonUserId, 1, "Resume Parsing", authToken)
        } catch (e: Exception) {
            if (e is NeonUserService.InsufficientCreditException) throw e
            // If fetching ID fails or other error, wrap/rethrow
             throw IOException("Failed to process credit deduction: ${e.message}", e)
        }

        // Truncate to save tokens
        val truncatedText = rawText.take(20000) 

        val prompt = buildResumeParsePrompt(truncatedText)
        
        try {
            val messages = listOf(
                OpenRouterService.Message(
                    role = "system",
                    content = "You are an expert resume parser. Your job is to extract structured data from resume text."
                ),
                OpenRouterService.Message(
                    role = "user",
                    content = prompt
                )
            )

            // Request JSON
            val responseText = OpenRouterService.chatCompletion(
                messages = messages,
                temperature = 0.1, // Low temperature for consistent extraction
                responseFormat = OpenRouterService.ResponseFormat(type = "json_object")
            )

            // Parse result
            val cleanJson = cleanJsonString(responseText)
            val parsedData = gson.fromJson(cleanJson, ParsedResumeData::class.java)
            
            return@withContext mapToResume(parsedData)
            
        } catch (e: Exception) {
            throw Exception("Failed to parse resume with AI: ${e.message}", e)
        }
    }

    private fun buildResumeParsePrompt(resumeText: String): String {
        return """
            Extract the following information from the resume text provided below and return it as a VALID JSON object.
            
            Resume Text:
            ----------------
            $resumeText
            ----------------
            
            Return a JSON object with this exact structure:
            {
              "fullName": "String",
              "profession": "String (Job title/role, e.g. Software Engineer)",
              "email": "String",
              "phone": "String",
              "location": "String",
              "website": "String (optional)",
              "linkedin": "String (optional)",
              "github": "String (optional)",
              "summary": "String (Professional Summary/Objective)",
              "workExperiences": [
                {
                  "jobTitle": "String",
                  "companyName": "String",
                  "location": "String",
                  "startDate": "String (e.g. Jan 2020)",
                  "endDate": "String (e.g. Present or Dec 2022)",
                  "isCurrent": boolean,
                  "description": "String (full description)",
                  "responsibilities": ["String", "String"] (Split bullet points into array)
                }
              ],
              "education": [
                {
                  "schoolName": "String",
                  "degree": "String",
                  "fieldOfStudy": "String",
                  "location": "String",
                  "startDate": "String",
                  "endDate": "String",
                  "isCurrent": boolean,
                  "gpa": "String (optional)",
                  "achievements": ["String"] (optional achievements/honors)
                }
              ],
              "skills": ["String", "String"],
              "projects": [
                {
                  "title": "String",
                  "description": "String",
                  "technologies": ["String", "String"],
                  "link": "String (optional URL/GitHub link)",
                  "startDate": "String (optional)",
                  "endDate": "String (optional)"
                }
              ],
              "certifications": [
                {
                  "name": "String",
                  "issuer": "String",
                  "issueDate": "String (e.g. Jan 2020)",
                  "expiryDate": "String (optional, e.g. Jan 2025)",
                  "credentialId": "String (optional)"
                }
              ],
              "languages": [
                {
                  "name": "String",
                  "proficiency": "String (Elementary/Intermediate/Proficient/Fluent/Native)"
                }
              ]
            }
            
            IMPORTANT:
            - If a field is missing, use empty string or empty array.
            - Extract skills as a flat list of strings.
            - Split work experience responsibilities into individual strings.
            - Ensure Dates are string format.
            - Return ONLY the JSON object.
        """.trimIndent()
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

    private fun mapToResume(data: ParsedResumeData): Resume {
        // Create new Resume
        return Resume(
            id = UUID.randomUUID().toString(),
            personalInfo = com.phamnhantucode.aicareercoach.ui.resumebuilder.PersonalInfo(
                fullName = data.fullName ?: "",
                profession = data.profession ?: "",
                email = data.email ?: "",
                phone = data.phone ?: "",
                location = data.location ?: "",
                portfolio = data.website ?: "",
                linkedIn = data.linkedin ?: "",
                github = data.github ?: ""
            ),
            professionalSummary = data.summary ?: "",
            workExperiences = data.workExperiences?.map { 
                com.phamnhantucode.aicareercoach.ui.resumebuilder.WorkExperience(
                    id = UUID.randomUUID().toString(),
                    jobTitle = it.jobTitle ?: "",
                    company = it.companyName ?: "",
                    location = it.location ?: "",
                    startDate = parseDate(it.startDate),
                    endDate = parseDate(it.endDate),
                    isCurrentRole = it.isCurrent ?: false,
                    responsibilities = it.responsibilities?.filter { resp -> resp.isNotBlank() } ?: emptyList()
                )
            } ?: emptyList(),
            education = data.education?.map {
                com.phamnhantucode.aicareercoach.ui.resumebuilder.Education(
                    id = UUID.randomUUID().toString(),
                    institution = it.schoolName ?: "",
                    degree = it.degree ?: "",
                    location = it.location ?: "",
                    startDate = parseDate(it.startDate),
                    endDate = parseDate(it.endDate),
                    gpa = it.gpa ?: "",
                    achievements = it.achievements?.filter { ach -> ach.isNotBlank() } ?: emptyList()
                )
            } ?: emptyList(),
            skills = data.skills?.filter { it.isNotBlank() } ?: emptyList(),
            projects = data.projects?.map {
                com.phamnhantucode.aicareercoach.ui.resumebuilder.Project(
                    id = UUID.randomUUID().toString(),
                    title = it.title ?: "",
                    description = it.description ?: "",
                    technologies = it.technologies?.filter { tech -> tech.isNotBlank() } ?: emptyList(),
                    link = it.link ?: "",
                    startDate = parseDate(it.startDate),
                    endDate = parseDate(it.endDate)
                )
            } ?: emptyList(),
            certifications = data.certifications?.map {
                com.phamnhantucode.aicareercoach.ui.resumebuilder.Certification(
                    id = UUID.randomUUID().toString(),
                    name = it.name ?: "",
                    issuer = it.issuer ?: "",
                    issueDate = parseDate(it.issueDate),
                    expiryDate = parseDate(it.expiryDate),
                    credentialId = it.credentialId ?: ""
                )
            } ?: emptyList(),
            languages = data.languages?.map {
                com.phamnhantucode.aicareercoach.ui.resumebuilder.Language(
                    id = UUID.randomUUID().toString(),
                    name = it.name ?: "",
                    proficiency = parseProficiency(it.proficiency)
                )
            } ?: emptyList()
        )
    }

    // JSON Models
    private data class ParsedResumeData(
        val fullName: String?,
        val profession: String?,
        val email: String?,
        val phone: String?,
        val location: String?,
        val website: String?,
        val linkedin: String?,
        val github: String?,
        val summary: String?,
        val workExperiences: List<ParsedWorkExperience>?,
        val education: List<ParsedEducation>?,
        val skills: List<String>?,
        val projects: List<ParsedProject>?,
        val certifications: List<ParsedCertification>?,
        val languages: List<ParsedLanguage>?
    )

    private data class ParsedWorkExperience(
        val jobTitle: String?,
        val companyName: String?,
        val location: String?,
        val startDate: String?,
        val endDate: String?,
        val isCurrent: Boolean?,
        val description: String?,
        val responsibilities: List<String>?
    )

    private data class ParsedEducation(
        val schoolName: String?,
        val degree: String?,
        val fieldOfStudy: String?,
        val location: String?,
        val startDate: String?,
        val endDate: String?,
        val isCurrent: Boolean?,
        val gpa: String?,
        val achievements: List<String>?
    )

    private data class ParsedProject(
        val title: String?,
        val description: String?,
        val technologies: List<String>?,
        val link: String?,
        val startDate: String?,
        val endDate: String?
    )

    private data class ParsedCertification(
        val name: String?,
        val issuer: String?,
        val issueDate: String?,
        val expiryDate: String?,
        val credentialId: String?
    )

    private data class ParsedLanguage(
        val name: String?,
        val proficiency: String?
    )

    private fun parseDate(dateString: String?): LocalDate? {
        if (dateString.isNullOrBlank()) return null
        val formats = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM"),
            DateTimeFormatter.ofPattern("MMM yyyy"),
            DateTimeFormatter.ofPattern("MMMM yyyy")
        )
        for (format in formats) {
            try {
                return LocalDate.parse(dateString, format)
            } catch (_: DateTimeParseException) { }
        }
        // Try just year
        try {
            val year = dateString.trim().toIntOrNull()
            if (year != null && year in 1900..2100) {
                return LocalDate.of(year, 1, 1)
            }
        } catch (_: Exception) { }
        return null
    }

    private fun parseProficiency(proficiencyString: String?): com.phamnhantucode.aicareercoach.ui.resumebuilder.LanguageProficiency {
        return when (proficiencyString?.trim()?.lowercase()) {
            "elementary", "beginner", "basic" -> com.phamnhantucode.aicareercoach.ui.resumebuilder.LanguageProficiency.ELEMENTARY
            "intermediate", "conversational" -> com.phamnhantucode.aicareercoach.ui.resumebuilder.LanguageProficiency.INTERMEDIATE
            "proficient", "advanced", "professional" -> com.phamnhantucode.aicareercoach.ui.resumebuilder.LanguageProficiency.PROFICIENT
            "fluent", "full professional" -> com.phamnhantucode.aicareercoach.ui.resumebuilder.LanguageProficiency.FLUENT
            "native", "native or bilingual" -> com.phamnhantucode.aicareercoach.ui.resumebuilder.LanguageProficiency.NATIVE
            else -> com.phamnhantucode.aicareercoach.ui.resumebuilder.LanguageProficiency.INTERMEDIATE
        }
    }
}
