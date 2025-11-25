package com.phamnhantucode.aicareercoach.ui.resumebuilder

import android.content.ContentValues
import android.content.Context
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.VisibleForTesting
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ResumeExportFormat(
    val fileExtension: String,
    val mimeType: String,
    val displayName: String
) {
    MARKDOWN("md", "text/markdown", "Markdown"),
    PDF("pdf", "application/pdf", "PDF")
}

sealed class ResumeExportResult {
    data class Success(
        val format: ResumeExportFormat,
        val uri: Uri,
        val fileName: String
    ) : ResumeExportResult()

    data class Error(
        val format: ResumeExportFormat,
        val throwable: Throwable
    ) : ResumeExportResult()
}

object ResumeFormatter {
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())

    fun toMarkdown(resume: Resume): String = buildString {
        // Contact Information - Centered with emojis
        val contactParts = buildContactLine(resume.personalInfo)
        if (contactParts.isNotEmpty()) {
            appendLine("## <div align=\"center\">${resume.personalInfo.fullName.ifBlank { "Professional Resume" }}</div>")
            appendLine()
            appendLine("<div align=\"center\">")
            appendLine()
            appendLine(contactParts.joinToString(" | "))
            appendLine()
            appendLine("</div>")
            appendLine()
        }

        // Professional Summary
        if (resume.professionalSummary.isNotBlank()) {
            appendLine("## Professional Summary")
            appendLine()
            appendLine(resume.professionalSummary.trim())
            appendLine()
        }

        // Skills
        if (resume.skills.isNotEmpty()) {
            appendLine("## Skills")
            appendLine()
            appendLine(resume.skills.joinToString(", "))
            appendLine()
        }

        // Work Experience
        if (resume.workExperiences.isNotEmpty()) {
            appendLine("## Work Experience")
            appendLine()
            resume.workExperiences.forEach { exp ->
                appendLine("### ${exp.jobTitle}".takeIf { exp.jobTitle.isNotBlank() } ?: "### Experience")
                appendLine()
                detailLine(exp.company, exp.location).takeIf { it.isNotBlank() }?.let {
                    appendLine("**$it**")
                    appendLine()
                }
                dateRange(exp.startDate, exp.endDate, exp.isCurrentRole).takeIf { it.isNotBlank() }?.let {
                    appendLine("*$it*")
                    appendLine()
                }
                exp.responsibilities.filter { it.isNotBlank() }.forEach { responsibility ->
                    appendLine("- $responsibility")
                }
                appendLine()
            }
        }

        // Education
        if (resume.education.isNotEmpty()) {
            appendLine("## Education")
            appendLine()
            resume.education.forEach { edu ->
                appendLine("### ${edu.degree}".takeIf { edu.degree.isNotBlank() } ?: "### Education")
                appendLine()
                detailLine(edu.institution, edu.location).takeIf { it.isNotBlank() }?.let {
                    appendLine("**$it**")
                    appendLine()
                }
                dateRange(edu.startDate, edu.endDate, false).takeIf { it.isNotBlank() }?.let {
                    appendLine("*$it*")
                    appendLine()
                }
                if (edu.gpa.isNotBlank()) {
                    appendLine("- GPA: ${edu.gpa}")
                }
                edu.achievements.filter { it.isNotBlank() }.forEach { achievement ->
                    appendLine("- $achievement")
                }
                appendLine()
            }
        }

        // Projects
        if (resume.projects.isNotEmpty()) {
            appendLine("## Projects")
            appendLine()
            resume.projects.forEach { project ->
                appendLine("### ${project.title}".takeIf { project.title.isNotBlank() } ?: "### Project")
                appendLine()
                dateRange(project.startDate, project.endDate, false).takeIf { it.isNotBlank() }?.let {
                    appendLine("*$it*")
                    appendLine()
                }
                if (project.description.isNotBlank()) {
                    appendLine(project.description.trim())
                    appendLine()
                }
                if (project.technologies.isNotEmpty()) {
                    appendLine("**Technologies:** ${project.technologies.joinToString(", ")}")
                }
                if (project.link.isNotBlank()) {
                    appendLine("**Link:** [${project.link}](${project.link})")
                }
                appendLine()
            }
        }

        // Certifications
        if (resume.certifications.isNotEmpty()) {
            appendLine("## Certifications")
            appendLine()
            resume.certifications.forEach { certification ->
                val certName = certification.name.takeIf { it.isNotBlank() } ?: "Certification"
                val details = listOfNotBlank(
                    certification.issuer,
                    dateString(certification.issueDate),
                    certification.credentialId.takeIf { it.isNotBlank() }?.let { "ID: $it" }
                ).joinToString(" • ")

                if (details.isNotBlank()) {
                    appendLine("- **$certName** - $details")
                } else {
                    appendLine("- **$certName**")
                }
            }
            appendLine()
        }

        // Languages
        if (resume.languages.isNotEmpty()) {
            appendLine("## Languages")
            appendLine()
            resume.languages.forEach { language ->
                appendLine("- **${language.name}** - ${language.proficiency.displayName}")
            }
        }
    }.trimEnd()

    private fun buildContactLine(info: PersonalInfo): List<String> {
        val parts = mutableListOf<String>()

        if (info.email.isNotBlank()) {
            parts.add("📧 ${info.email}")
        }
        if (info.phone.isNotBlank()) {
            parts.add("📱 ${info.phone}")
        }
        if (info.linkedIn.isNotBlank()) {
            parts.add("💼 [LinkedIn](${info.linkedIn})")
        }
        if (info.github.isNotBlank()) {
            parts.add("🔗 [GitHub](${info.github})")
        }
        if (info.portfolio.isNotBlank()) {
            parts.add("🌐 [Portfolio](${info.portfolio})")
        }
        if (info.location.isNotBlank()) {
            parts.add("📍 ${info.location}")
        }

        return parts
    }

    private fun detailLine(primary: String, secondary: String): String = listOfNotBlank(primary, secondary)
        .joinToString(" • ")

    private fun dateRange(start: LocalDate?, end: LocalDate?, isCurrent: Boolean): String {
        if (start == null && end == null) return ""
        val startText = start?.format(dateFormatter)
        val endText = when {
            isCurrent -> "Present"
            end != null -> end.format(dateFormatter)
            else -> null
        }
        return listOfNotBlank(startText, endText).joinToString(" - ")
    }

    private fun dateString(date: LocalDate?): String? = date?.format(dateFormatter)

    private fun listOfNotBlank(vararg values: String?): List<String> =
        values.mapNotNull { value ->
            value?.takeIf { it.isNotBlank() }?.trim()
        }

    /**
     * Parses Markdown text back into a Resume object.
     * This enables cross-platform compatibility with the web version.
     */
    fun fromMarkdown(markdown: String): Resume {
        val lines = markdown.lines()
        var currentLine = 0

        val personalInfo = parsePersonalInfo(lines, currentLine)
        currentLine = findNextSection(lines, currentLine)

        val professionalSummary = parseSummary(lines, currentLine)
        currentLine = findNextSection(lines, currentLine)

        val skills = parseSkills(lines, currentLine)
        currentLine = findNextSection(lines, currentLine)

        val workExperiences = parseWorkExperience(lines, currentLine)
        currentLine = findNextSection(lines, currentLine)

        val education = parseEducation(lines, currentLine)
        currentLine = findNextSection(lines, currentLine)

        val projects = parseProjects(lines, currentLine)
        currentLine = findNextSection(lines, currentLine)

        val certifications = parseCertifications(lines, currentLine)
        currentLine = findNextSection(lines, currentLine)

        val languages = parseLanguages(lines, currentLine)

        return Resume(
            id = java.util.UUID.randomUUID().toString(),
            personalInfo = personalInfo,
            professionalSummary = professionalSummary,
            workExperiences = workExperiences,
            education = education,
            skills = skills,
            projects = projects,
            certifications = certifications,
            languages = languages
        )
    }

    private fun parsePersonalInfo(lines: List<String>, startFrom: Int): PersonalInfo {
        var fullName = ""
        var email = ""
        var phone = ""
        var linkedIn = ""
        var github = ""
        var portfolio = ""
        var location = ""

        // Find name in header (## <div align="center">Name</div>)
        for (i in startFrom until lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("## <div")) {
                fullName = line.replace(Regex("<[^>]*>"), "").replace("##", "").trim()
                break
            } else if (line.startsWith("##")) {
                // If we hit another section header, stop
                break
            }
        }

        // Find contact info line with emojis
        for (i in startFrom until lines.size) {
            val line = lines[i].trim()
            if (line.contains("📧") || line.contains("📱") || line.contains("💼")) {
                val parts = line.split("|").map { it.trim() }
                parts.forEach { part ->
                    when {
                        part.contains("📧") -> email = part.replace("📧", "").trim()
                        part.contains("📱") -> phone = part.replace("📱", "").trim()
                        part.contains("💼") && part.contains("[LinkedIn]") -> {
                            linkedIn = extractUrl(part)
                        }
                        part.contains("🔗") && part.contains("[GitHub]") -> {
                            github = extractUrl(part)
                        }
                        part.contains("🌐") && part.contains("[Portfolio]") -> {
                            portfolio = extractUrl(part)
                        }
                        part.contains("📍") -> location = part.replace("📍", "").trim()
                    }
                }
                break
            }
        }

        return PersonalInfo(
            fullName = fullName,
            email = email,
            phone = phone,
            location = location,
            linkedIn = linkedIn,
            github = github,
            portfolio = portfolio
        )
    }

    private fun parseSummary(lines: List<String>, startFrom: Int): String {
        val summaryLines = mutableListOf<String>()
        var foundSection = false

        for (i in startFrom until lines.size) {
            val line = lines[i].trim()
            when {
                line == "## Professional Summary" -> foundSection = true
                foundSection && line.startsWith("##") -> break
                foundSection && line.isNotBlank() -> summaryLines.add(line)
            }
        }

        return summaryLines.joinToString(" ").trim()
    }

    private fun parseSkills(lines: List<String>, startFrom: Int): List<String> {
        var foundSection = false

        for (i in startFrom until lines.size) {
            val line = lines[i].trim()
            when {
                line == "## Skills" -> foundSection = true
                foundSection && line.startsWith("##") -> break
                foundSection && line.isNotBlank() -> {
                    return line.split(",").map { it.trim() }.filter { it.isNotBlank() }
                }
            }
        }

        return emptyList()
    }

    private fun parseWorkExperience(lines: List<String>, startFrom: Int): List<WorkExperience> {
        val experiences = mutableListOf<WorkExperience>()
        var foundSection = false
        var currentExp: MutableMap<String, Any?> = mutableMapOf()
        val responsibilities = mutableListOf<String>()

        for (i in startFrom until lines.size) {
            val line = lines[i].trim()
            when {
                line == "## Work Experience" -> foundSection = true
                foundSection && line.startsWith("## ") && line != "## Work Experience" -> break
                foundSection && line.startsWith("### ") -> {
                    // Save previous experience if exists
                    if (currentExp.isNotEmpty()) {
                        experiences.add(buildWorkExperience(currentExp, responsibilities.toList()))
                        responsibilities.clear()
                    }
                    currentExp = mutableMapOf("jobTitle" to line.removePrefix("### "))
                }
                foundSection && line.startsWith("**") && line.endsWith("**") -> {
                    val text = line.removeSurrounding("**")
                    val parts = text.split("•").map { it.trim() }
                    if (parts.isNotEmpty()) currentExp["company"] = parts[0]
                    if (parts.size > 1) currentExp["location"] = parts[1]
                }
                foundSection && line.startsWith("*") && line.endsWith("*") -> {
                    val dateText = line.removeSurrounding("*")
                    val (startDate, endDate, isCurrent) = parseDateRange(dateText)
                    currentExp["startDate"] = startDate
                    currentExp["endDate"] = endDate
                    currentExp["isCurrentRole"] = isCurrent
                }
                foundSection && line.startsWith("- ") -> {
                    responsibilities.add(line.removePrefix("- "))
                }
            }
        }

        // Add last experience
        if (currentExp.isNotEmpty()) {
            experiences.add(buildWorkExperience(currentExp, responsibilities.toList()))
        }

        return experiences
    }

    private fun parseEducation(lines: List<String>, startFrom: Int): List<Education> {
        val educationList = mutableListOf<Education>()
        var foundSection = false
        var currentEdu: MutableMap<String, Any?> = mutableMapOf()
        val achievements = mutableListOf<String>()

        for (i in startFrom until lines.size) {
            val line = lines[i].trim()
            when {
                line == "## Education" -> foundSection = true
                foundSection && line.startsWith("## ") && line != "## Education" -> break
                foundSection && line.startsWith("### ") -> {
                    // Save previous education if exists
                    if (currentEdu.isNotEmpty()) {
                        educationList.add(buildEducation(currentEdu, achievements.toList()))
                        achievements.clear()
                    }
                    currentEdu = mutableMapOf("degree" to line.removePrefix("### "))
                }
                foundSection && line.startsWith("**") && line.endsWith("**") -> {
                    val text = line.removeSurrounding("**")
                    val parts = text.split("•").map { it.trim() }
                    if (parts.isNotEmpty()) currentEdu["institution"] = parts[0]
                    if (parts.size > 1) currentEdu["location"] = parts[1]
                }
                foundSection && line.startsWith("*") && line.endsWith("*") -> {
                    val dateText = line.removeSurrounding("*")
                    val (startDate, endDate, _) = parseDateRange(dateText)
                    currentEdu["startDate"] = startDate
                    currentEdu["endDate"] = endDate
                }
                foundSection && line.startsWith("- GPA:") -> {
                    currentEdu["gpa"] = line.removePrefix("- GPA:").trim()
                }
                foundSection && line.startsWith("- ") && !line.startsWith("- GPA:") -> {
                    achievements.add(line.removePrefix("- "))
                }
            }
        }

        // Add last education
        if (currentEdu.isNotEmpty()) {
            educationList.add(buildEducation(currentEdu, achievements.toList()))
        }

        return educationList
    }

    private fun parseProjects(lines: List<String>, startFrom: Int): List<Project> {
        val projects = mutableListOf<Project>()
        var foundSection = false
        var currentProject: MutableMap<String, Any?> = mutableMapOf()

        for (i in startFrom until lines.size) {
            val line = lines[i].trim()
            when {
                line == "## Projects" -> foundSection = true
                foundSection && line.startsWith("## ") && line != "## Projects" -> break
                foundSection && line.startsWith("### ") -> {
                    // Save previous project if exists
                    if (currentProject.isNotEmpty()) {
                        projects.add(buildProject(currentProject))
                    }
                    currentProject = mutableMapOf("title" to line.removePrefix("### "))
                }
                foundSection && line.startsWith("*") && line.endsWith("*") -> {
                    val dateText = line.removeSurrounding("*")
                    val (startDate, endDate, _) = parseDateRange(dateText)
                    currentProject["startDate"] = startDate
                    currentProject["endDate"] = endDate
                }
                foundSection && line.startsWith("**Technologies:**") -> {
                    val techText = line.removePrefix("**Technologies:**").trim()
                    currentProject["technologies"] = techText.split(",").map { it.trim() }
                }
                foundSection && line.startsWith("**Link:**") -> {
                    currentProject["link"] = extractUrl(line)
                }
                foundSection && !line.startsWith("**") && !line.startsWith("###") && !line.startsWith("*") && line.isNotBlank() -> {
                    // Description text
                    if (!currentProject.containsKey("description")) {
                        currentProject["description"] = line
                    }
                }
            }
        }

        // Add last project
        if (currentProject.isNotEmpty()) {
            projects.add(buildProject(currentProject))
        }

        return projects
    }

    private fun parseCertifications(lines: List<String>, startFrom: Int): List<Certification> {
        val certifications = mutableListOf<Certification>()
        var foundSection = false

        for (i in startFrom until lines.size) {
            val line = lines[i].trim()
            when {
                line == "## Certifications" -> foundSection = true
                foundSection && line.startsWith("##") -> break
                foundSection && line.startsWith("- **") -> {
                    val cert = parseCertificationLine(line)
                    if (cert != null) certifications.add(cert)
                }
            }
        }

        return certifications
    }

    private fun parseLanguages(lines: List<String>, startFrom: Int): List<Language> {
        val languages = mutableListOf<Language>()
        var foundSection = false

        for (i in startFrom until lines.size) {
            val line = lines[i].trim()
            when {
                line == "## Languages" -> foundSection = true
                foundSection && line.startsWith("##") -> break
                foundSection && line.startsWith("- **") -> {
                    val lang = parseLanguageLine(line)
                    if (lang != null) languages.add(lang)
                }
            }
        }

        return languages
    }

    // Helper functions
    private fun findNextSection(lines: List<String>, currentLine: Int): Int {
        for (i in (currentLine + 1) until lines.size) {
            if (lines[i].trim().startsWith("##")) {
                return i
            }
        }
        return lines.size
    }

    private fun extractUrl(text: String): String {
        val regex = Regex("\\[.+?\\]\\((.+?)\\)")
        return regex.find(text)?.groupValues?.get(1) ?: ""
    }

    private fun parseDateRange(dateText: String): Triple<LocalDate?, LocalDate?, Boolean> {
        val parts = dateText.split("-").map { it.trim() }
        var startDate: LocalDate? = null
        var endDate: LocalDate? = null
        var isCurrent = false

        if (parts.isNotEmpty()) {
            startDate = parseDate(parts[0])
        }
        if (parts.size > 1) {
            when (parts[1]) {
                "Present" -> isCurrent = true
                else -> endDate = parseDate(parts[1])
            }
        }

        return Triple(startDate, endDate, isCurrent)
    }

    private fun parseDate(dateStr: String): LocalDate? {
        return try {
            val parts = dateStr.split(" ")
            if (parts.size == 2) {
                val month = when (parts[0]) {
                    "Jan" -> 1
                    "Feb" -> 2
                    "Mar" -> 3
                    "Apr" -> 4
                    "May" -> 5
                    "Jun" -> 6
                    "Jul" -> 7
                    "Aug" -> 8
                    "Sep" -> 9
                    "Oct" -> 10
                    "Nov" -> 11
                    "Dec" -> 12
                    else -> return null
                }
                val year = parts[1].toIntOrNull() ?: return null
                LocalDate.of(year, month, 1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildWorkExperience(data: Map<String, Any?>, responsibilities: List<String>): WorkExperience {
        return WorkExperience(
            jobTitle = data["jobTitle"] as? String ?: "",
            company = data["company"] as? String ?: "",
            location = data["location"] as? String ?: "",
            startDate = data["startDate"] as? LocalDate,
            endDate = data["endDate"] as? LocalDate,
            isCurrentRole = data["isCurrentRole"] as? Boolean ?: false,
            responsibilities = responsibilities
        )
    }

    private fun buildEducation(data: Map<String, Any?>, achievements: List<String>): Education {
        return Education(
            degree = data["degree"] as? String ?: "",
            institution = data["institution"] as? String ?: "",
            location = data["location"] as? String ?: "",
            startDate = data["startDate"] as? LocalDate,
            endDate = data["endDate"] as? LocalDate,
            gpa = data["gpa"] as? String ?: "",
            achievements = achievements
        )
    }

    private fun buildProject(data: Map<String, Any?>): Project {
        return Project(
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            technologies = data["technologies"] as? List<String> ?: emptyList(),
            link = data["link"] as? String ?: "",
            startDate = data["startDate"] as? LocalDate,
            endDate = data["endDate"] as? LocalDate
        )
    }

    private fun parseCertificationLine(line: String): Certification? {
        // Format: - **Name** - Issuer • Date • ID: xxx
        val nameMatch = Regex("\\*\\*(.+?)\\*\\*").find(line) ?: return null
        val name = nameMatch.groupValues[1]

        val detailsPart = line.substringAfter("** - ").takeIf { it != line } ?: ""
        val details = detailsPart.split("•").map { it.trim() }

        var issuer = ""
        var issueDate: LocalDate? = null
        var credentialId = ""

        details.forEach { detail ->
            when {
                detail.startsWith("ID:") -> credentialId = detail.removePrefix("ID:").trim()
                detail.contains(" ") && !detail.startsWith("ID") -> {
                    // Try to parse as date
                    val date = parseDate(detail)
                    if (date != null) {
                        issueDate = date
                    } else if (issuer.isEmpty()) {
                        issuer = detail
                    }
                }
                issuer.isEmpty() -> issuer = detail
            }
        }

        return Certification(
            name = name,
            issuer = issuer,
            issueDate = issueDate,
            credentialId = credentialId
        )
    }

    private fun parseLanguageLine(line: String): Language? {
        // Format: - **Name** - Proficiency
        val parts = line.removePrefix("- ").split(" - ")
        if (parts.size < 2) return null

        val name = parts[0].removeSurrounding("**")
        val proficiencyStr = parts[1].trim()

        val proficiency = when (proficiencyStr) {
            "Elementary" -> LanguageProficiency.ELEMENTARY
            "Intermediate" -> LanguageProficiency.INTERMEDIATE
            "Proficient" -> LanguageProficiency.PROFICIENT
            "Fluent" -> LanguageProficiency.FLUENT
            "Native" -> LanguageProficiency.NATIVE
            else -> LanguageProficiency.INTERMEDIATE
        }

        return Language(name = name, proficiency = proficiency)
    }
}

class ResumeExporter(private val context: Context) {

    suspend fun export(format: ResumeExportFormat, resume: Resume): ResumeExportResult = when (format) {
        ResumeExportFormat.MARKDOWN -> exportMarkdown(resume)
        ResumeExportFormat.PDF -> exportPdf(resume)
    }

    private fun exportMarkdown(resume: Resume): ResumeExportResult = runCatching {
        val fileName = buildFileName(resume, ResumeExportFormat.MARKDOWN)
        val markdown = ResumeFormatter.toMarkdown(resume)
        val uri = saveToDownloads(fileName, ResumeExportFormat.MARKDOWN.mimeType, markdown.toByteArray())
        ResumeExportResult.Success(ResumeExportFormat.MARKDOWN, uri, fileName)
    }.getOrElse { throwable ->
        ResumeExportResult.Error(ResumeExportFormat.MARKDOWN, throwable)
    }

    private fun exportPdf(resume: Resume): ResumeExportResult = runCatching {
        val fileName = buildFileName(resume, ResumeExportFormat.PDF)
        val markdown = ResumeFormatter.toMarkdown(resume)
        val pdfBytes = buildPdf(markdown)
        val uri = saveToDownloads(fileName, ResumeExportFormat.PDF.mimeType, pdfBytes)
        ResumeExportResult.Success(ResumeExportFormat.PDF, uri, fileName)
    }.getOrElse { throwable ->
        ResumeExportResult.Error(ResumeExportFormat.PDF, throwable)
    }

    private fun saveToDownloads(
        displayName: String,
        mimeType: String,
        bytes: ByteArray
    ): Uri {
        val resolver = context.contentResolver
        val downloadsUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/AI Career Coach"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            put(MediaStore.MediaColumns.SIZE, bytes.size)
        }

        val uri = resolver.insert(downloadsUri, contentValues)
            ?: throw IOException("Unable to create export file.")

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(bytes)
            outputStream.flush()
        } ?: throw IOException("Unable to open export destination.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val completedValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, completedValues, null, null)
        }

        return uri
    }

    private fun buildFileName(resume: Resume, format: ResumeExportFormat): String {
        val baseName = resume.personalInfo.fullName
            .ifBlank { "resume" }
            .lowercase(Locale.getDefault())
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
            .ifBlank { "resume" }
        val timestamp = System.currentTimeMillis()
        return "${baseName}-${timestamp}.${format.fileExtension}"
    }

    @VisibleForTesting
    internal fun buildPdf(markdown: String): ByteArray {
        // A4 size in points (72 points = 1 inch)
        val pageWidth = 595  // 8.27 inches
        val pageHeight = 842  // 11.69 inches
        val margin = 50
        val contentWidth = pageWidth - (margin * 2)
        val contentHeight = pageHeight - (margin * 2)

        // Parse markdown into styled sections
        val styledText = parseMarkdownForPdf(markdown)

        val textPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = android.graphics.Color.BLACK
        }

        val layout = StaticLayout.Builder
            .obtain(styledText, 0, styledText.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(2f, 1.15f)
            .build()

        val document = PdfDocument()
        var offsetY = 0
        var pageNumber = 1

        while (offsetY < layout.height) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Draw white background
            canvas.drawColor(android.graphics.Color.WHITE)

            canvas.save()
            canvas.translate(margin.toFloat(), margin.toFloat())
            canvas.clipRect(0f, 0f, contentWidth.toFloat(), contentHeight.toFloat())
            canvas.translate(0f, -offsetY.toFloat())
            layout.draw(canvas)
            canvas.restore()
            document.finishPage(page)

            offsetY += contentHeight
            pageNumber += 1
        }

        return ByteArrayOutputStream().use { output ->
            document.writeTo(output)
            document.close()
            output.toByteArray()
        }
    }

    /**
     * Simplified markdown parser for PDF rendering.
     * Strips markdown syntax while preserving structure and readability.
     */
    private fun parseMarkdownForPdf(markdown: String): String {
        return markdown.lines().joinToString("\n") { line ->
            when {
                // Remove HTML tags (like <div align="center">)
                line.trim().startsWith("<") && line.trim().endsWith(">") -> ""

                // H2 headers (##)
                line.startsWith("## ") -> {
                    "\n" + line.removePrefix("## ")
                        .replace("<div align=\"center\">", "")
                        .replace("</div>", "")
                        .uppercase() + "\n"
                }

                // H3 headers (###)
                line.startsWith("### ") -> {
                    "\n" + line.removePrefix("### ") + "\n"
                }

                // Bold text (**text**)
                line.contains("**") -> {
                    line.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
                }

                // Italic text (*text*)
                line.contains("*") && !line.contains("**") -> {
                    line.replace(Regex("\\*(.+?)\\*"), "$1")
                }

                // Links [text](url)
                line.contains("](") -> {
                    line.replace(Regex("\\[(.+?)\\]\\(.+?\\)"), "$1")
                }

                // Bullet points
                line.trim().startsWith("- ") -> {
                    "  • " + line.trim().removePrefix("- ")
                }

                // Empty lines
                line.isBlank() -> ""

                else -> line
            }
        }.replace(Regex("\n{3,}"), "\n\n") // Normalize multiple newlines
    }
}
