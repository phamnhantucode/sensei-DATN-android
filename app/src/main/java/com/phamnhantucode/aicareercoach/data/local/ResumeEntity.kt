package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.phamnhantucode.aicareercoach.ui.resumebuilder.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

@Entity(tableName = "resumes")
@TypeConverters(ResumeConverters::class)
data class ResumeEntity(
    @PrimaryKey
    val id: String,
    val userId: String, // Neon user ID
    val resumeData: Resume,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class ResumeConverters {

    @TypeConverter
    fun fromResume(resume: Resume): String {
        return resumeToJson(resume).toString()
    }

    @TypeConverter
    fun toResume(json: String): Resume {
        return jsonToResume(JSONObject(json))
    }

    // Helper functions for Resume serialization
    private fun resumeToJson(resume: Resume): JSONObject {
        return JSONObject().apply {
            put("id", resume.id)
            put("personalInfo", personalInfoToJson(resume.personalInfo))
            put("professionalSummary", resume.professionalSummary)
            put("workExperiences", workExperiencesToJson(resume.workExperiences))
            put("education", educationToJson(resume.education))
            put("skills", JSONArray(resume.skills))
            put("projects", projectsToJson(resume.projects))
            put("certifications", certificationsToJson(resume.certifications))
            put("languages", languagesToJson(resume.languages))
            put("theme", themeToJson(resume.theme))
            put("sectionConfig", sectionConfigToJson(resume.sectionConfig))
        }
    }

    private fun jsonToResume(json: JSONObject): Resume {
        return Resume(
            id = json.getString("id"),
            personalInfo = jsonToPersonalInfo(json.getJSONObject("personalInfo")),
            professionalSummary = json.getString("professionalSummary"),
            workExperiences = jsonToWorkExperiences(json.getJSONArray("workExperiences")),
            education = jsonToEducation(json.getJSONArray("education")),
            skills = jsonToStringList(json.getJSONArray("skills")),
            projects = jsonToProjects(json.getJSONArray("projects")),
            certifications = jsonToCertifications(json.getJSONArray("certifications")),
            languages = jsonToLanguages(json.getJSONArray("languages")),
            theme = jsonToTheme(json.getJSONObject("theme")),
            sectionConfig = jsonToSectionConfig(json.getJSONArray("sectionConfig"))
        )
    }

    // PersonalInfo serialization
    private fun personalInfoToJson(info: PersonalInfo): JSONObject {
        return JSONObject().apply {
            put("fullName", info.fullName)
            put("email", info.email)
            put("phone", info.phone)
            put("location", info.location)
            put("linkedIn", info.linkedIn)
            put("portfolio", info.portfolio)
            put("github", info.github)
        }
    }

    private fun jsonToPersonalInfo(json: JSONObject): PersonalInfo {
        return PersonalInfo(
            fullName = json.getString("fullName"),
            email = json.getString("email"),
            phone = json.getString("phone"),
            location = json.getString("location"),
            linkedIn = json.getString("linkedIn"),
            portfolio = json.getString("portfolio"),
            github = json.getString("github")
        )
    }

    // WorkExperience serialization
    private fun workExperiencesToJson(experiences: List<WorkExperience>): JSONArray {
        return JSONArray().apply {
            experiences.forEach { exp ->
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
        }
    }

    private fun jsonToWorkExperiences(json: JSONArray): List<WorkExperience> {
        val list = mutableListOf<WorkExperience>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            list.add(WorkExperience(
                id = obj.getString("id"),
                jobTitle = obj.getString("jobTitle"),
                company = obj.getString("company"),
                location = obj.getString("location"),
                startDate = obj.getString("startDate").takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                endDate = obj.getString("endDate").takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                isCurrentRole = obj.getBoolean("isCurrentRole"),
                responsibilities = jsonToStringList(obj.getJSONArray("responsibilities"))
            ))
        }
        return list
    }

    // Education serialization
    private fun educationToJson(education: List<Education>): JSONArray {
        return JSONArray().apply {
            education.forEach { edu ->
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
        }
    }

    private fun jsonToEducation(json: JSONArray): List<Education> {
        val list = mutableListOf<Education>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            list.add(Education(
                id = obj.getString("id"),
                degree = obj.getString("degree"),
                institution = obj.getString("institution"),
                location = obj.getString("location"),
                startDate = obj.getString("startDate").takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                endDate = obj.getString("endDate").takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                gpa = obj.getString("gpa"),
                achievements = jsonToStringList(obj.getJSONArray("achievements"))
            ))
        }
        return list
    }

    // Project serialization
    private fun projectsToJson(projects: List<Project>): JSONArray {
        return JSONArray().apply {
            projects.forEach { proj ->
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
        }
    }

    private fun jsonToProjects(json: JSONArray): List<Project> {
        val list = mutableListOf<Project>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            list.add(Project(
                id = obj.getString("id"),
                title = obj.getString("title"),
                description = obj.getString("description"),
                technologies = jsonToStringList(obj.getJSONArray("technologies")),
                link = obj.getString("link"),
                startDate = obj.getString("startDate").takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                endDate = obj.getString("endDate").takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) }
            ))
        }
        return list
    }

    // Certification serialization
    private fun certificationsToJson(certifications: List<Certification>): JSONArray {
        return JSONArray().apply {
            certifications.forEach { cert ->
                put(JSONObject().apply {
                    put("id", cert.id)
                    put("name", cert.name)
                    put("issuer", cert.issuer)
                    put("issueDate", cert.issueDate?.toString() ?: "")
                    put("expiryDate", cert.expiryDate?.toString() ?: "")
                    put("credentialId", cert.credentialId)
                })
            }
        }
    }

    private fun jsonToCertifications(json: JSONArray): List<Certification> {
        val list = mutableListOf<Certification>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            list.add(Certification(
                id = obj.getString("id"),
                name = obj.getString("name"),
                issuer = obj.getString("issuer"),
                issueDate = obj.getString("issueDate").takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                expiryDate = obj.getString("expiryDate").takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                credentialId = obj.getString("credentialId")
            ))
        }
        return list
    }

    // Language serialization
    private fun languagesToJson(languages: List<Language>): JSONArray {
        return JSONArray().apply {
            languages.forEach { lang ->
                put(JSONObject().apply {
                    put("id", lang.id)
                    put("name", lang.name)
                    put("proficiency", lang.proficiency.name)
                })
            }
        }
    }

    private fun jsonToLanguages(json: JSONArray): List<Language> {
        val list = mutableListOf<Language>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            list.add(Language(
                id = obj.getString("id"),
                name = obj.getString("name"),
                proficiency = LanguageProficiency.valueOf(obj.getString("proficiency"))
            ))
        }
        return list
    }

    // Theme serialization
    private fun themeToJson(theme: ResumeTheme): JSONObject {
        return JSONObject().apply {
            put("templateId", theme.templateId)
            put("colorScheme", JSONObject().apply {
                put("primaryColor", theme.colorScheme.primaryColor)
                put("accentColor", theme.colorScheme.accentColor)
                put("textColor", theme.colorScheme.textColor)
                put("backgroundColor", theme.colorScheme.backgroundColor)
                put("sectionHeaderColor", theme.colorScheme.sectionHeaderColor)
                put("secondaryTextColor", theme.colorScheme.secondaryTextColor)
            })
            put("typography", JSONObject().apply {
                put("fontFamily", theme.typography.fontFamily)
                put("headerSize", theme.typography.headerSize)
                put("subHeaderSize", theme.typography.subHeaderSize)
                put("bodySize", theme.typography.bodySize)
                put("captionSize", theme.typography.captionSize)
                put("headerWeight", theme.typography.headerWeight)
                put("bodyWeight", theme.typography.bodyWeight)
            })
            put("layout", JSONObject().apply {
                put("type", theme.layout.type.name)
                put("spacing", theme.layout.spacing)
                put("sectionSpacing", theme.layout.sectionSpacing)
                put("sectionStyle", theme.layout.sectionStyle.name)
            })
        }
    }

    private fun jsonToTheme(json: JSONObject): ResumeTheme {
        val colorScheme = json.getJSONObject("colorScheme")
        val typography = json.getJSONObject("typography")
        val layout = json.getJSONObject("layout")

        return ResumeTheme(
            templateId = json.getString("templateId"),
            colorScheme = ColorScheme(
                primaryColor = colorScheme.getLong("primaryColor"),
                accentColor = colorScheme.getLong("accentColor"),
                textColor = colorScheme.getLong("textColor"),
                backgroundColor = colorScheme.getLong("backgroundColor"),
                sectionHeaderColor = colorScheme.getLong("sectionHeaderColor"),
                secondaryTextColor = colorScheme.getLong("secondaryTextColor")
            ),
            typography = TypographyScheme(
                fontFamily = typography.getString("fontFamily"),
                headerSize = typography.getDouble("headerSize").toFloat(),
                subHeaderSize = typography.getDouble("subHeaderSize").toFloat(),
                bodySize = typography.getDouble("bodySize").toFloat(),
                captionSize = typography.getDouble("captionSize").toFloat(),
                headerWeight = typography.getInt("headerWeight"),
                bodyWeight = typography.getInt("bodyWeight")
            ),
            layout = LayoutConfig(
                type = LayoutType.valueOf(layout.getString("type")),
                spacing = layout.getInt("spacing"),
                sectionSpacing = layout.getInt("sectionSpacing"),
                sectionStyle = SectionStyle.valueOf(layout.getString("sectionStyle"))
            )
        )
    }

    // SectionConfig serialization
    private fun sectionConfigToJson(configs: List<SectionConfig>): JSONArray {
        return JSONArray().apply {
            configs.forEach { config ->
                put(JSONObject().apply {
                    put("sectionTypeId", config.sectionType.id)
                    put("isVisible", config.isVisible)
                    put("order", config.order)
                })
            }
        }
    }

    private fun jsonToSectionConfig(json: JSONArray): List<SectionConfig> {
        val list = mutableListOf<SectionConfig>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            val sectionType = ResumeSectionType.fromId(obj.getString("sectionTypeId"))
            if (sectionType != null) {
                list.add(SectionConfig(
                    sectionType = sectionType,
                    isVisible = obj.getBoolean("isVisible"),
                    order = obj.getInt("order")
                ))
            }
        }
        return list.ifEmpty { getDefaultSectionConfig() }
    }

    // Helper
    private fun jsonToStringList(json: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until json.length()) {
            list.add(json.getString(i))
        }
        return list
    }
}
