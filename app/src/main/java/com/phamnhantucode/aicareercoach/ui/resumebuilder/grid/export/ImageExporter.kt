package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumePageRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridResume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Image Export Interface
 */
interface ImageExporter {
    /**
     * Generate an image from a grid-based resume
     *
     * @param resume The grid resume to export
     * @param outputFile The destination file for the image
     * @return Flow of export states (progress, completion, error)
     */
    fun generateImage(resume: GridResume, outputFile: File): Flow<ImageExportState>
}

/**
 * Image Export State
 */
sealed class ImageExportState {
    object Idle : ImageExportState()
    object Rendering : ImageExportState()
    object SavingFile : ImageExportState()
    data class Success(val uri: Uri, val fileSizeBytes: Long) : ImageExportState()
    data class Error(val throwable: Throwable, val message: String) : ImageExportState()
}

/**
 * Android implementation of ImageExporter
 */
class AndroidImageExporter(private val context: Context) : ImageExporter {

    private val pageRenderer = ResumePageRenderer(context)

    override fun generateImage(resume: GridResume, outputFile: File): Flow<ImageExportState> = flow {
        emit(ImageExportState.Rendering)

        try {
            // Render the first page at high resolution (2x A4 size for better quality)
            // A4 at 72 DPI is 595x842. Let's do 2x that -> 1190x1684 (approx 150 DPI equivalent)
            val width = 1190
            val height = 1684
            
            val bitmap = pageRenderer.renderFirstPage(resume, width, height)
            
            if (bitmap == null) {
                emit(ImageExportState.Error(Exception("Failed to render page"), "Failed to render resume page"))
                return@flow
            }

            emit(ImageExportState.SavingFile)

            withContext(Dispatchers.IO) {
                FileOutputStream(outputFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )

            emit(ImageExportState.Success(uri, outputFile.length()))

        } catch (e: Exception) {
            emit(ImageExportState.Error(e, e.message ?: "Unknown error during image export"))
        }
    }
}
