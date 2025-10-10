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
