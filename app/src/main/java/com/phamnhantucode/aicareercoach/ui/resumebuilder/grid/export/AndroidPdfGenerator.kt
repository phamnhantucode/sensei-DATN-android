package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridResume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridPosition
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.LayoutMode
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

/**
 * Android PDF Generator
 *
 * Implementation using Android's native PdfDocument API
 * This is the Phase 1 implementation - simple, zero dependencies
 */
class AndroidPdfGenerator(
    private val context: Context,
    private val config: PdfExportConfig = PdfExportConfig()
) : PdfExporter {

    private val imageCache: ImageCache = CoilImageCache(context)
    private val colorConverter = ColorConverter()

    override fun generatePdf(resume: GridResume, outputFile: File): Flow<PdfExportState> = flow {
        try {
            emit(PdfExportState.Idle)

            // Validate resume
            if (resume.pages.isEmpty()) {
                throw PdfExportException.InvalidResume("Resume has no pages")
            }

            // Pre-load all images
            emit(PdfExportState.PreparingImages)
            preloadImages(resume)

            // Create PDF document
            val pdfDocument = PdfDocument()

            // Render each page
            resume.pages.forEachIndexed { index, page ->
                emit(PdfExportState.RenderingPage(index + 1, resume.pages.size))

                val pageInfo = PdfDocument.PageInfo.Builder(
                    config.pageWidth,
                    config.pageHeight,
                    index + 1
                ).create()

                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas = pdfPage.canvas

                // Draw page background
                canvas.drawColor(colorConverter.toIntColor(page.backgroundColor))

                // Render all elements (sorted by z-index)
                val sortedElements = page.elements.sortedBy { it.zIndex }
                renderElements(canvas, sortedElements, resume.gridConfig)

                pdfDocument.finishPage(pdfPage)
            }

            // Save to file
            emit(PdfExportState.SavingFile)
            withContext(Dispatchers.IO) {
                FileOutputStream(outputFile).use { output ->
                    pdfDocument.writeTo(output)
                }
                pdfDocument.close()
            }

            // Get file size
            val fileSize = outputFile.length()

            // Clean up
            imageCache.clear()

            emit(PdfExportState.Success(Uri.fromFile(outputFile), fileSize))

        } catch (e: Exception) {
            imageCache.clear()
            emit(PdfExportState.Error(e, e.message ?: "Unknown error"))
        }
    }

    override suspend fun exportToDownloads(resume: GridResume, fileName: String?): Result<PdfExportResult> {
        return try {
            val finalFileName = fileName ?: generateFileName(resume)
            val uri = createDownloadUri(finalFileName)

            // Generate PDF to temp file first
            val tempFile = File.createTempFile("resume_", ".pdf", context.cacheDir)

            // Collect the flow to generate the PDF
            var result: PdfExportResult? = null
            generatePdf(resume, tempFile).collect { state ->
                if (state is PdfExportState.Success) {
                    // Copy temp file to downloads
                    copyToUri(tempFile, uri)

                    result = PdfExportResult(
                        uri = uri,
                        fileName = finalFileName,
                        fileSizeBytes = state.fileSizeBytes,
                        pageCount = resume.pages.size
                    )
                } else if (state is PdfExportState.Error) {
                    throw state.throwable
                }
            }

            // Clean up temp file
            tempFile.delete()

            Result.success(result!!)
        } catch (e: Exception) {
            Result.failure(PdfExportException.FileWriteFailed(e))
        }
    }

    /**
     * Pre-load all images from the resume
     */
    private suspend fun preloadImages(resume: GridResume) {
        resume.pages.forEach { page ->
            page.elements.filterIsInstance<ResumeElement.ImageElement>().forEach { imageElement ->
                if (imageElement.imageUrl.isNotBlank()) {
                    imageCache.getImage(imageElement.imageUrl)
                }
            }
        }
    }

    /**
     * Render all elements on the canvas
     */
    private suspend fun renderElements(
        canvas: android.graphics.Canvas,
        elements: List<ResumeElement>,
        gridConfig: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridConfig
    ) {
        val renderer = ResumeCanvasRenderer(context, config, imageCache, colorConverter)
        renderer.renderElements(canvas, elements, gridConfig)
    }

    /**
     * Generate filename from resume data
     */
    private fun generateFileName(resume: GridResume): String {
        val baseName = resume.name
            .ifBlank { "resume" }
            .lowercase(Locale.getDefault())
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
            .ifBlank { "resume" }
        val timestamp = System.currentTimeMillis()
        return "${baseName}-${timestamp}.pdf"
    }

    /**
     * Create URI in Downloads folder
     */
    private fun createDownloadUri(fileName: String): Uri {
        val resolver = context.contentResolver
        val downloadsUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/AI Career Coach"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        return resolver.insert(downloadsUri, contentValues)
            ?: throw IOException("Unable to create export file.")
    }

    /**
     * Copy file to URI
     */
    private fun copyToUri(file: File, uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw IOException("Unable to open export destination.")

        // Mark as complete
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val completedValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, completedValues, null, null)
        }
    }
}
