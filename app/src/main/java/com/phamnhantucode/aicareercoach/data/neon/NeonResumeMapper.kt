package com.phamnhantucode.aicareercoach.data.neon

import com.google.gson.GsonBuilder
import com.phamnhantucode.aicareercoach.ui.resumebuilder.*
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumeElementTypeAdapter
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridResume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Mapper for converting between app Resume models and Neon database schema.
 * 
 * Database Tables:
 * - Resume: id, userId, content, atsScore, feedback, createdAt, updatedAt, json, skills, template, title, professional_summary, accentColor
 * - ResumePersonalInfo: id, resumeId, image, fullName, profession, phone, email, location, linkedin, website
 * - ResumeExperience: id, resumeId, title, organization, description, startDate, endDate, isCurrent
 * - ResumeEducation: id, resumeId, graduationDate, institution, degree, field, gpa
 * - ResumeProject: id, resumeId, name, description, type
 */
object NeonResumeMapper {

    // Gson instance for GridResume serialization
    private val gson = GsonBuilder()
        .registerTypeAdapter(ResumeElement::class.java, ResumeElementTypeAdapter())
        .serializeNulls()
        .create()

    // ==================== Resume Table ====================

    /**
     * Convert app Resume to Neon Resume table payload
     * @param gridResume Optional GridResume to store in the 'json' field instead of form data
     */
    fun toNeonResumePayload(resume: Resume, userId: String, gridResume: GridResume? = null): JSONObject {
        return JSONObject().apply {
            put("id", resume.id)
            put("userId", userId)
            put("content", ResumeFormatter.toMarkdown(resume)) // Keep markdown for backward compatibility
            put("title", resume.personalInfo.fullName.ifEmpty { "Untitled Resume" })
            put("professional_summary", resume.professionalSummary)
            put("template", resume.theme.templateId)
            put("accentColor", getAccentColorName(resume.theme.colorScheme.accentColor))
            put("skills", toPostgresTextArray(resume.skills))
            // Store GridResume design JSON if provided, otherwise store NULL
            // IMPORTANT: Do NOT store form data JSON here, as it causes the app to think a design exists
            put("json", gridResume?.let { toGridResumeJson(it) } ?: JSONObject.NULL)
        }
    }

    /**
     * Convert app Resume to Neon Resume table update payload (without id/userId)
     * @param gridResume Optional GridResume to store in the 'json' field instead of form data
     */
    fun toNeonResumeUpdatePayload(resume: Resume, gridResume: GridResume? = null): JSONObject {
        return JSONObject().apply {
            put("content", ResumeFormatter.toMarkdown(resume))
            put("title", resume.personalInfo.fullName.ifEmpty { "Untitled Resume" })
            put("professional_summary", resume.professionalSummary)
            put("template", resume.theme.templateId)
            put("accentColor", getAccentColorName(resume.theme.colorScheme.accentColor))
            put("skills", toPostgresTextArray(resume.skills))
            // Store GridResume design JSON if provided, otherwise store NULL
            // IMPORTANT: Do NOT store form data JSON here, as it causes the app to think a design exists
            put("json", gridResume?.let { toGridResumeJson(it) } ?: JSONObject.NULL)
        }
    }

    /**
     * Serialize GridResume to JSON string for 'json' field storage
     */
    private fun toGridResumeJson(gridResume: GridResume): String {
        return gson.toJson(gridResume)
    }

    /**
     * Parse Neon Resume row to app Resume
     */
    fun fromNeonResumeRow(json: JSONObject): Resume {
        // Try to parse from 'json' field first (contains full resume data)
        val jsonField = json.optString("json", "")
        if (jsonField.isNotEmpty()) {
            try {
                return fromResumeJson(JSONObject(jsonField)).copy(id = json.getString("id"))
            } catch (e: Exception) {
                // Fall through to markdown parsing
            }
        }

        // Fallback: parse from markdown content
        val content = json.optString("content", "")
        if (content.isNotEmpty()) {
            try {
                return ResumeFormatter.fromMarkdown(content).copy(id = json.getString("id"))
            } catch (e: Exception) {
                // Fall through to basic parsing
            }
        }

        // Last fallback: construct from individual fields
        return Resume(
            id = json.getString("id"),
            personalInfo = PersonalInfo(
                fullName = optStringSafe(json, "title", "Untitled Resume")
            ),
            professionalSummary = optStringSafe(json, "professional_summary", ""),
            skills = fromPostgresTextArray(json.optString("skills", "")),
            theme = ResumeTheme(
                templateId = optStringSafe(json, "template", "classic"),
                colorScheme = ColorScheme(
                    accentColor = getAccentColorValue(optStringSafe(json, "accentColor", "neutral"))
                )
            )
        )
    }

    // ==================== ResumePersonalInfo Table ====================

    /**
     * Convert app PersonalInfo to Neon ResumePersonalInfo table payload
     */
    fun toNeonPersonalInfoPayload(resumeId: String, info: PersonalInfo): JSONObject {
        return JSONObject().apply {
            put("id", java.util.UUID.randomUUID().toString())
            put("resumeId", resumeId)
            put("fullName", info.fullName)
            put("email", info.email)
            put("phone", info.phone)
            put("location", info.location)
            put("linkedin", info.linkedIn)
            put("website", info.portfolio) // Map portfolio -> website
            put("image", info.avatar) // Map avatar -> image
            put("profession", "") // App doesn't have this field
        }
    }

    /**
     * Parse Neon ResumePersonalInfo row to app PersonalInfo
     */
    fun fromNeonPersonalInfoRow(json: JSONObject): PersonalInfo {
        return PersonalInfo(
            fullName = optStringSafe(json, "fullName", ""),
            email = optStringSafe(json, "email", ""),
            phone = optStringSafe(json, "phone", ""),
            location = optStringSafe(json, "location", ""),
            linkedIn = optStringSafe(json, "linkedin", ""),
            portfolio = optStringSafe(json, "website", ""), // Map website -> portfolio
            github = "", // DB doesn't have this
            avatar = optStringSafe(json, "image", "") // Map image -> avatar
        )
    }

    // ==================== ResumeExperience Table ====================

    /**
     * Convert app WorkExperience to Neon ResumeExperience table payload
     */
    fun toNeonExperiencePayload(resumeId: String, exp: WorkExperience): JSONObject {
        return JSONObject().apply {
            put("id", exp.id)
            put("resumeId", resumeId)
            put("title", exp.jobTitle) // Map jobTitle -> title
            put("organization", exp.company) // Map company -> organization
            put("description", exp.responsibilities.joinToString("\n• ", prefix = "• ")) // Convert list to string
            put("startDate", exp.startDate?.toString() ?: "")
            put("endDate", exp.endDate?.toString() ?: "")
            put("isCurrent", exp.isCurrentRole)
        }
    }

    /**
     * Parse Neon ResumeExperience row to app WorkExperience
     */
    fun fromNeonExperienceRow(json: JSONObject): WorkExperience {
        val description = optStringSafe(json, "description", "")
        val responsibilities = description
            .split("\n")
            .map { it.removePrefix("• ").trim() }
            .filter { it.isNotEmpty() }

        return WorkExperience(
            id = json.getString("id"),
            jobTitle = optStringSafe(json, "title", ""), // Map title -> jobTitle
            company = optStringSafe(json, "organization", ""), // Map organization -> company
            location = "", // DB doesn't have this
            startDate = parseDate(optStringSafe(json, "startDate", "")),
            endDate = parseDate(optStringSafe(json, "endDate", "")),
            isCurrentRole = json.optBoolean("isCurrent", false),
            responsibilities = responsibilities
        )
    }

    // ==================== ResumeEducation Table ====================

    /**
     * Convert app Education to Neon ResumeEducation table payload
     */
    fun toNeonEducationPayload(resumeId: String, edu: Education): JSONObject {
        return JSONObject().apply {
            put("id", edu.id)
            put("resumeId", resumeId)
            put("degree", edu.degree)
            put("institution", edu.institution)
            put("field", "") // App doesn't have separate field, could extract from degree
            put("graduationDate", edu.endDate?.toString() ?: "") // Map endDate -> graduationDate
            put("gpa", edu.gpa)
        }
    }

    /**
     * Parse Neon ResumeEducation row to app Education
     */
    fun fromNeonEducationRow(json: JSONObject): Education {
        val graduationDate = parseDate(optStringSafe(json, "graduationDate", ""))
        return Education(
            id = json.getString("id"),
            degree = optStringSafe(json, "degree", ""),
            institution = optStringSafe(json, "institution", ""),
            location = "", // DB doesn't have this
            startDate = null, // DB doesn't have this
            endDate = graduationDate, // Map graduationDate -> endDate
            gpa = optStringSafe(json, "gpa", ""),
            achievements = emptyList() // DB doesn't have this
        )
    }

    // ==================== ResumeProject Table ====================

    /**
     * Convert app Project to Neon ResumeProject table payload
     */
    fun toNeonProjectPayload(resumeId: String, project: Project): JSONObject {
        return JSONObject().apply {
            put("id", project.id)
            put("resumeId", resumeId)
            put("name", project.title) // Map title -> name
            put("description", project.description)
            put("type", project.technologies.firstOrNull() ?: "Other") // Use first technology as type
        }
    }

    /**
     * Parse Neon ResumeProject row to app Project
     */
    fun fromNeonProjectRow(json: JSONObject): Project {
        val projectType = optStringSafe(json, "type", "")
        return Project(
            id = json.getString("id"),
            title = optStringSafe(json, "name", ""), // Map name -> title
            description = optStringSafe(json, "description", ""),
            technologies = if (projectType.isNotEmpty()) listOf(projectType) else emptyList(),
            link = "", // DB doesn't have this
            startDate = null, // DB doesn't have this
            endDate = null // DB doesn't have this
        )
    }

    // ==================== Helper Functions ====================

    /**
     * Convert List<String> to PostgreSQL TEXT[] format
     */
    private fun toPostgresTextArray(list: List<String>): String {
        if (list.isEmpty()) return "{}"
        return list.joinToString(",", prefix = "{", postfix = "}") { "\"${it.replace("\"", "\\\"")}\"" }
    }

    /**
     * Parse PostgreSQL TEXT[] to List<String>
     */
    private fun fromPostgresTextArray(pgArray: String): List<String> {
        if (pgArray.isEmpty() || pgArray == "{}" || pgArray == "null") return emptyList()
        return pgArray
            .removeSurrounding("{", "}")
            .split(",")
            .map { it.trim().removeSurrounding("\"").replace("\\\"", "\"") }
            .filter { it.isNotEmpty() && it != "null" }
    }

    /**
     * Helper to safely get string from JSONObject, handling "null" string and JSONObject.NULL
     */
    private fun optStringSafe(json: JSONObject, key: String, fallback: String): String {
        if (json.isNull(key)) return fallback
        val value = json.optString(key, fallback)
        return if (value == "null") fallback else value
    }

    /**
     * Parse date string to LocalDate
     */
    private fun parseDate(dateStr: String): LocalDate? {
        if (dateStr.isEmpty() || dateStr == "null") return null
        return try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            try {
                // Try alternative formats
                LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * Convert accent color Long to color name for DB
     */
    private fun getAccentColorName(colorValue: Long): String {
        return when (colorValue) {
            0xFF1976D2L -> "blue"
            0xFF0288D1L -> "lightblue"
            0xFF388E3CL -> "green"
            0xFFD32F2FL -> "red"
            0xFFF57C00L -> "orange"
            0xFF7B1FA2L -> "purple"
            0xFF455A64L -> "gray"
            else -> "neutral"
        }
    }

    /**
     * Convert color name from DB to accent color Long
     */
    private fun getAccentColorValue(colorName: String): Long {
        return when (colorName.lowercase()) {
            "blue" -> 0xFF1976D2L
            "lightblue" -> 0xFF0288D1L
            "green" -> 0xFF388E3CL
            "red" -> 0xFFD32F2FL
            "orange" -> 0xFFF57C00L
            "purple" -> 0xFF7B1FA2L
            "gray", "grey" -> 0xFF455A64L
            else -> 0xFF757575L // neutral
        }
    }

    /**
     * Serialize full Resume to JSON string for 'json' field storage
     */
    private fun toResumeJson(resume: Resume): String {
        return JSONObject().apply {
            put("id", resume.id)
            put("personalInfo", JSONObject().apply {
                put("fullName", resume.personalInfo.fullName)
                put("email", resume.personalInfo.email)
                put("phone", resume.personalInfo.phone)
                put("location", resume.personalInfo.location)
                put("linkedIn", resume.personalInfo.linkedIn)
                put("portfolio", resume.personalInfo.portfolio)
                put("github", resume.personalInfo.github)
                put("avatar", resume.personalInfo.avatar)
            })
            put("professionalSummary", resume.professionalSummary)
            put("workExperiences", JSONArray().apply {
                resume.workExperiences.forEach { exp ->
                    put(JSONObject().apply {
                        put("id", exp.id)
                        put("jobTitle", exp.jobTitle)
                        put("company", exp.company)
                        put("location", exp.location)
                        put("startDate", exp.startDate?.toString() ?: "")
                        put("endDate", exp.endDate?.toString() ?: "")
                        put("isCurrentRole", exp.isCurrentRole)
                        put("responsibilities", JSONArray(exp.responsibilities))
                    })
                }
            })
            put("education", JSONArray().apply {
                resume.education.forEach { edu ->
                    put(JSONObject().apply {
                        put("id", edu.id)
                        put("degree", edu.degree)
                        put("institution", edu.institution)
                        put("location", edu.location)
                        put("startDate", edu.startDate?.toString() ?: "")
                        put("endDate", edu.endDate?.toString() ?: "")
                        put("gpa", edu.gpa)
                        put("achievements", JSONArray(edu.achievements))
                    })
                }
            })
            put("skills", JSONArray(resume.skills))
            put("projects", JSONArray().apply {
                resume.projects.forEach { proj ->
                    put(JSONObject().apply {
                        put("id", proj.id)
                        put("title", proj.title)
                        put("description", proj.description)
                        put("technologies", JSONArray(proj.technologies))
                        put("link", proj.link)
                        put("startDate", proj.startDate?.toString() ?: "")
                        put("endDate", proj.endDate?.toString() ?: "")
                    })
                }
            })
            put("certifications", JSONArray().apply {
                resume.certifications.forEach { cert ->
                    put(JSONObject().apply {
                        put("id", cert.id)
                        put("name", cert.name)
                        put("issuer", cert.issuer)
                        put("issueDate", cert.issueDate?.toString() ?: "")
                        put("expiryDate", cert.expiryDate?.toString() ?: "")
                        put("credentialId", cert.credentialId)
                    })
                }
            })
            put("languages", JSONArray().apply {
                resume.languages.forEach { lang ->
                    put(JSONObject().apply {
                        put("id", lang.id)
                        put("name", lang.name)
                        put("proficiency", lang.proficiency.name)
                    })
                }
            })
            put("theme", JSONObject().apply {
                put("templateId", resume.theme.templateId)
                put("colorScheme", JSONObject().apply {
                    put("primaryColor", resume.theme.colorScheme.primaryColor)
                    put("accentColor", resume.theme.colorScheme.accentColor)
                    put("textColor", resume.theme.colorScheme.textColor)
                    put("backgroundColor", resume.theme.colorScheme.backgroundColor)
                    put("sectionHeaderColor", resume.theme.colorScheme.sectionHeaderColor)
                    put("secondaryTextColor", resume.theme.colorScheme.secondaryTextColor)
                })
                put("typography", JSONObject().apply {
                    put("fontFamily", resume.theme.typography.fontFamily)
                    put("headerSize", resume.theme.typography.headerSize)
                    put("subHeaderSize", resume.theme.typography.subHeaderSize)
                    put("bodySize", resume.theme.typography.bodySize)
                    put("captionSize", resume.theme.typography.captionSize)
                    put("headerWeight", resume.theme.typography.headerWeight)
                    put("bodyWeight", resume.theme.typography.bodyWeight)
                })
                put("layout", JSONObject().apply {
                    put("type", resume.theme.layout.type.name)
                    put("spacing", resume.theme.layout.spacing)
                    put("sectionSpacing", resume.theme.layout.sectionSpacing)
                    put("sectionStyle", resume.theme.layout.sectionStyle.name)
                })
            })
            put("sectionConfig", JSONArray().apply {
                resume.sectionConfig.forEach { config ->
                    put(JSONObject().apply {
                        put("sectionTypeId", config.sectionType.id)
                        put("isVisible", config.isVisible)
                        put("order", config.order)
                    })
                }
            })
        }.toString()
    }

    /**
     * Deserialize Resume from JSON string
     */
    private fun fromResumeJson(json: JSONObject): Resume {
        val personalInfoJson = json.getJSONObject("personalInfo")
        val themeJson = json.optJSONObject("theme")

        return Resume(
            id = json.getString("id"),
            personalInfo = PersonalInfo(
                fullName = optStringSafe(personalInfoJson, "fullName", ""),
                email = optStringSafe(personalInfoJson, "email", ""),
                phone = optStringSafe(personalInfoJson, "phone", ""),
                location = optStringSafe(personalInfoJson, "location", ""),
                linkedIn = optStringSafe(personalInfoJson, "linkedIn", ""),
                portfolio = optStringSafe(personalInfoJson, "portfolio", ""),
                github = optStringSafe(personalInfoJson, "github", ""),
                avatar = optStringSafe(personalInfoJson, "avatar", "")
            ),
            professionalSummary = optStringSafe(json, "professionalSummary", ""),
            workExperiences = parseWorkExperiences(json.optJSONArray("workExperiences")),
            education = parseEducation(json.optJSONArray("education")),
            skills = parseStringList(json.optJSONArray("skills")),
            projects = parseProjects(json.optJSONArray("projects")),
            certifications = parseCertifications(json.optJSONArray("certifications")),
            languages = parseLanguages(json.optJSONArray("languages")),
            theme = parseTheme(themeJson),
            sectionConfig = parseSectionConfig(json.optJSONArray("sectionConfig"))
        )
    }

    private fun parseWorkExperiences(jsonArray: JSONArray?): List<WorkExperience> {
        if (jsonArray == null) return emptyList()
        val list = mutableListOf<WorkExperience>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(WorkExperience(
                id = optStringSafe(obj, "id", java.util.UUID.randomUUID().toString()),
                jobTitle = optStringSafe(obj, "jobTitle", ""),
                company = optStringSafe(obj, "company", ""),
                location = optStringSafe(obj, "location", ""),
                startDate = parseDate(optStringSafe(obj, "startDate", "")),
                endDate = parseDate(optStringSafe(obj, "endDate", "")),
                isCurrentRole = obj.optBoolean("isCurrentRole", false),
                responsibilities = parseStringList(obj.optJSONArray("responsibilities"))
            ))
        }
        return list
    }

    private fun parseEducation(jsonArray: JSONArray?): List<Education> {
        if (jsonArray == null) return emptyList()
        val list = mutableListOf<Education>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(Education(
                id = optStringSafe(obj, "id", java.util.UUID.randomUUID().toString()),
                degree = optStringSafe(obj, "degree", ""),
                institution = optStringSafe(obj, "institution", ""),
                location = optStringSafe(obj, "location", ""),
                startDate = parseDate(optStringSafe(obj, "startDate", "")),
                endDate = parseDate(optStringSafe(obj, "endDate", "")),
                gpa = optStringSafe(obj, "gpa", ""),
                achievements = parseStringList(obj.optJSONArray("achievements"))
            ))
        }
        return list
    }

    private fun parseProjects(jsonArray: JSONArray?): List<Project> {
        if (jsonArray == null) return emptyList()
        val list = mutableListOf<Project>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(Project(
                id = optStringSafe(obj, "id", java.util.UUID.randomUUID().toString()),
                title = optStringSafe(obj, "title", ""),
                description = optStringSafe(obj, "description", ""),
                technologies = parseStringList(obj.optJSONArray("technologies")),
                link = optStringSafe(obj, "link", ""),
                startDate = parseDate(optStringSafe(obj, "startDate", "")),
                endDate = parseDate(optStringSafe(obj, "endDate", ""))
            ))
        }
        return list
    }

    private fun parseCertifications(jsonArray: JSONArray?): List<Certification> {
        if (jsonArray == null) return emptyList()
        val list = mutableListOf<Certification>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(Certification(
                id = optStringSafe(obj, "id", java.util.UUID.randomUUID().toString()),
                name = optStringSafe(obj, "name", ""),
                issuer = optStringSafe(obj, "issuer", ""),
                issueDate = parseDate(optStringSafe(obj, "issueDate", "")),
                expiryDate = parseDate(optStringSafe(obj, "expiryDate", "")),
                credentialId = optStringSafe(obj, "credentialId", "")
            ))
        }
        return list
    }

    private fun parseLanguages(jsonArray: JSONArray?): List<Language> {
        if (jsonArray == null) return emptyList()
        val list = mutableListOf<Language>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(Language(
                id = optStringSafe(obj, "id", java.util.UUID.randomUUID().toString()),
                name = optStringSafe(obj, "name", ""),
                proficiency = try {
                    LanguageProficiency.valueOf(optStringSafe(obj, "proficiency", "INTERMEDIATE"))
                } catch (e: Exception) {
                    LanguageProficiency.INTERMEDIATE
                }
            ))
        }
        return list
    }

    private fun parseTheme(json: JSONObject?): ResumeTheme {
        if (json == null) return ResumeTheme()
        
        val colorSchemeJson = json.optJSONObject("colorScheme")
        val typographyJson = json.optJSONObject("typography")
        val layoutJson = json.optJSONObject("layout")

        return ResumeTheme(
            templateId = json.optString("templateId", "professional"),
            colorScheme = if (colorSchemeJson != null) ColorScheme(
                primaryColor = colorSchemeJson.optLong("primaryColor", 0xFF1976D2L),
                accentColor = colorSchemeJson.optLong("accentColor", 0xFF0288D1L),
                textColor = colorSchemeJson.optLong("textColor", 0xFF212121L),
                backgroundColor = colorSchemeJson.optLong("backgroundColor", 0xFFFFFFFFL),
                sectionHeaderColor = colorSchemeJson.optLong("sectionHeaderColor", 0xFF1976D2L),
                secondaryTextColor = colorSchemeJson.optLong("secondaryTextColor", 0xFF757575L)
            ) else ColorScheme(),
            typography = if (typographyJson != null) TypographyScheme(
                fontFamily = typographyJson.optString("fontFamily", "Default"),
                headerSize = typographyJson.optDouble("headerSize", 24.0).toFloat(),
                subHeaderSize = typographyJson.optDouble("subHeaderSize", 18.0).toFloat(),
                bodySize = typographyJson.optDouble("bodySize", 14.0).toFloat(),
                captionSize = typographyJson.optDouble("captionSize", 12.0).toFloat(),
                headerWeight = typographyJson.optInt("headerWeight", 700),
                bodyWeight = typographyJson.optInt("bodyWeight", 400)
            ) else TypographyScheme(),
            layout = if (layoutJson != null) LayoutConfig(
                type = try {
                    LayoutType.valueOf(layoutJson.optString("type", "SINGLE_COLUMN"))
                } catch (e: Exception) {
                    LayoutType.SINGLE_COLUMN
                },
                spacing = layoutJson.optInt("spacing", 16),
                sectionSpacing = layoutJson.optInt("sectionSpacing", 24),
                sectionStyle = try {
                    SectionStyle.valueOf(layoutJson.optString("sectionStyle", "CARD"))
                } catch (e: Exception) {
                    SectionStyle.CARD
                }
            ) else LayoutConfig()
        )
    }

    private fun parseSectionConfig(jsonArray: JSONArray?): List<SectionConfig> {
        if (jsonArray == null) return getDefaultSectionConfig()
        val list = mutableListOf<SectionConfig>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val sectionType = ResumeSectionType.fromId(obj.optString("sectionTypeId", ""))
            if (sectionType != null) {
                list.add(SectionConfig(
                    sectionType = sectionType,
                    isVisible = obj.optBoolean("isVisible", true),
                    order = obj.optInt("order", i)
                ))
            }
        }
        return list.ifEmpty { getDefaultSectionConfig() }
    }

    private fun parseStringList(jsonArray: JSONArray?): List<String> {
        if (jsonArray == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }
}
