package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export

import android.content.Context
import android.net.Uri
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridResume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * PDF Export Interface
 *
 * This interface allows for differ ent PDF generation implementations
 * (e.g., Android PdfDocument, Apache PDFBox, iText, etc.)
 */
interface PdfExporter {
    /**
     * Generate a PDF from a grid-based resume
     *
     * @param resume The grid resume to export
     * @param outputFile The destination file for the PDF
     * @return Flow of export states (progress, completion, error)
     */
    fun generatePdf(resume: GridResume, outputFile: File): Flow<PdfExportState>

    /**
     * Save PDF to Downloads folder and return shareable URI
     *
     * @param resume The grid resume to export
     * @param fileName Optional custom filename
     * @return Result with URI or error
     */
    suspend fun exportToDownloads(resume: GridResume, fileName: String? = null): Result<PdfExportResult>
}

/**
 * PDF Export State - used for progress tracking
 */
sealed class PdfExportState {
    object Idle : PdfExportState()
    object PreparingImages : PdfExportState()
    data class RenderingPage(val page: Int, val total: Int) : PdfExportState()
    object SavingFile : PdfExportState()
    data class Success(val uri: Uri, val fileSizeBytes: Long) : PdfExportState()
    data class Error(val throwable: Throwable, val message: String) : PdfExportState()
}

/**
 * PDF Export Result
 */
data class PdfExportResult(
    val uri: Uri,
    val fileName: String,
    val fileSizeBytes: Long,
    val pageCount: Int
)

/**
 * PDF Export Exception
 */
sealed class PdfExportException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ImageLoadFailed(imageUrl: String, cause: Throwable? = null) :
        PdfExportException("Failed to load image: $imageUrl", cause)

    class InvalidResume(reason: String) :
        PdfExportException("Invalid resume data: $reason")

    class FileWriteFailed(cause: Throwable) :
        PdfExportException("Failed to write PDF file", cause)

    class UnsupportedElement(elementType: String) :
        PdfExportException("Unsupported element type: $elementType")
}

/**
 * PDF Export Configuration
 */
data class PdfExportConfig(
    val pageWidth: Int = 595,  // A4 width in points (8.27 inches * 72 dpi)
    val pageHeight: Int = 842,  // A4 height in points (11.69 inches * 72 dpi)
    val dpi: Int = 72,  // Standard PDF DPI
    val quality: PdfQuality = PdfQuality.HIGH,
    val embedFonts: Boolean = false,  // Phase 1: false, Phase 2: true
    val metadata: PdfMetadata = PdfMetadata()
)

data class PdfMetadata(
    val title: String = "",
    val author: String = "",
    val subject: String = "Resume",
    val keywords: List<String> = emptyList(),
    val creator: String = "AI Career Coach"
)

enum class PdfQuality {
    LOW,      // Lower image quality, smaller file size
    MEDIUM,   // Balanced quality
    HIGH      // Best quality, larger file size
}
