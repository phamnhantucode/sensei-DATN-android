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

    // Element renderers
    private val textRenderer = TextElementPdfRenderer()
    private val shapeRenderer = ShapeElementPdfRenderer()
    private val imageRenderer = ImageElementPdfRenderer()
    private val chartRenderer = ChartElementPdfRenderer()
    private val iconRenderer = IconElementPdfRenderer()
    private val contactRenderer = ContactElementPdfRenderer()
    private val workExperienceRenderer = WorkExperienceElementPdfRenderer()
    private val educationRenderer = EducationElementPdfRenderer()
    private val skillRenderer = SkillElementPdfRenderer()
    private val projectRenderer = ProjectElementPdfRenderer()
    private val certificationRenderer = CertificationElementPdfRenderer()
    private val languageRenderer = LanguageElementPdfRenderer()
    
    // Container renderer with recursive child rendering callback
    private val containerRenderer = ContainerElementPdfRenderer { canvas, container ->
        // This callback renders the children of a container
        renderContainerChildren(canvas, container)
    }
    
    // Store current render state for recursive rendering
    private var currentElements: List<ResumeElement> = emptyList()
    private lateinit var currentMapper: GridCoordinateMapper
    private lateinit var currentRenderContext: PdfRenderContext

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
        val mapper = GridCoordinateMapper(gridConfig, config)
        android.util.Log.d("PDF_Export", "GridCoordinateMapper config:\n${mapper.toString()}")
        android.util.Log.d("PDF_Export", "Device density: ${context.resources.displayMetrics.density}, scaledDensity: ${context.resources.displayMetrics.scaledDensity}")
        val renderContext = PdfRenderContext(context, config, imageCache, colorConverter)
        
        // Store current state for recursive container rendering
        currentElements = elements
        currentMapper = mapper
        currentRenderContext = renderContext

        elements.forEach { element ->
            try {
                when (element) {
                    is ResumeElement.TextElement -> {
                        val bounds = mapper.gridToPdfRect(element.position)
                        textRenderer.render(canvas, element, bounds, mapper, renderContext)
                    }

                    is ResumeElement.ShapeElement -> {
                        val bounds = if (element.customWidthDp != null || element.customHeightDp != null) {
                            mapper.customSizeToRect(
                                element.position,
                                element.customWidthDp,
                                element.customHeightDp
                            )
                        } else {
                            mapper.gridToPdfRect(element.position)
                        }
                        shapeRenderer.render(canvas, element, bounds, mapper, renderContext)
                    }

                    is ResumeElement.ImageElement -> {
                        val bounds = mapper.gridToPdfRect(element.position)
                        imageRenderer.render(canvas, element, bounds, mapper, renderContext)
                    }

                    is ResumeElement.ChartElement -> {
                        val bounds = mapper.gridToPdfRect(element.position)
                        chartRenderer.render(canvas, element, bounds, mapper, renderContext)
                    }

                    is ResumeElement.IconElement -> {
                        val bounds = mapper.gridToPdfRect(element.position)
                        iconRenderer.render(canvas, element, bounds, mapper, renderContext)
                    }

                    is ResumeElement.ContactElement -> {
                        val bounds = mapper.gridToPdfRect(element.position)
                        contactRenderer.render(canvas, element, bounds, mapper, renderContext)
                    }

                    is ResumeElement.WorkExperienceElement -> {
                        val bounds = mapper.gridToPdfRect(element.position)
                        workExperienceRenderer.render(canvas, element, bounds, mapper, renderContext)
                    }

                    is ResumeElement.EducationElement -> {
                        val bounds = mapper.gridToPdfRect(element.position)
                        educationRenderer.render(canvas, element, bounds, mapper, renderContext)
                    }

                    is ResumeElement.SkillElement -> {
                        val bounds = mapper.gridToPdfRect(element.position)
                        skillRenderer.render(canvas, element, bounds, mapper, renderContext)
                    }

                    is ResumeElement.ProjectElement -> {
                        val bounds = mapper.gridToPdfRect(element.position)
                        projectRenderer.render(canvas, element, bounds, mapper, renderContext)
                    }

                    is ResumeElement.CertificationElement -> {
                        val bounds = mapper.gridToPdfRect(element.position)
                        certificationRenderer.render(canvas, element, bounds, mapper, renderContext)
                    }

                    is ResumeElement.LanguageElement -> {
                        val bounds = mapper.gridToPdfRect(element.position)
                        languageRenderer.render(canvas, element, bounds, mapper, renderContext)
                    }

                    is ResumeElement.ContainerElement -> {
                        val bounds = mapper.gridToPdfRect(element.position)
                        containerRenderer.render(canvas, element, bounds, mapper, renderContext)
                    }
                }
            } catch (e: Exception) {
                // Log error but continue rendering other elements
                e.printStackTrace()
            }
        }
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
    
    /**
     * Render children of a container element
     * This is called by the ContainerElementPdfRenderer's callback
     * 
     * IMPORTANT: The canvas has already been translated to the container's top-left position
     * by ContainerElementPdfRenderer.
     * 
     * Position handling depends on container layout mode:
     * - VERTICAL layout: Children store RELATIVE positions (matching UI's useRelativePositioning=true)
     * - GRID/FREE layout: Children store ABSOLUTE positions (no useRelativePositioning in UI)
     */
    private suspend fun renderContainerChildren(
        canvas: android.graphics.Canvas,
        container: ResumeElement.ContainerElement
    ) {
        // Find child elements by their IDs
        val childElements = currentElements.filter { element ->
            container.children.contains(element.id)
        }.sortedBy { it.zIndex }
        
        // Check if container uses vertical layout
        val isVerticalLayout = container.effectiveLayoutMode == com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.LayoutMode.VERTICAL
        
        // Render each child element
        childElements.forEach { child ->
            try {
                // Determine position based on layout mode
                val renderPosition = if (isVerticalLayout) {
                    // VERTICAL layout: children already have relative positions, use as-is
                    child.position
                } else {
                    // GRID/FREE layout: children have absolute positions, convert to relative
                    GridPosition(
                        row = child.position.row - container.position.row,
                        col = child.position.col - container.position.col,
                        rowSpan = child.position.rowSpan,
                        colSpan = child.position.colSpan,
                        widthMode = child.position.widthMode,
                        heightMode = child.position.heightMode,
                        cachedHeightDp = child.position.cachedHeightDp
                    )
                }
                
                when (child) {
                    is ResumeElement.TextElement -> {
                        val bounds = currentMapper.gridToPdfRect(renderPosition)
                        textRenderer.render(canvas, child, bounds, currentMapper, currentRenderContext)
                    }
                    
                    is ResumeElement.ShapeElement -> {
                        val bounds = if (child.customWidthDp != null || child.customHeightDp != null) {
                            currentMapper.customSizeToRect(
                                renderPosition,
                                child.customWidthDp,
                                child.customHeightDp
                            )
                        } else {
                            currentMapper.gridToPdfRect(renderPosition)
                        }
                        shapeRenderer.render(canvas, child, bounds, currentMapper, currentRenderContext)
                    }
                    
                    is ResumeElement.ImageElement -> {
                        val bounds = currentMapper.gridToPdfRect(renderPosition)
                        imageRenderer.render(canvas, child, bounds, currentMapper, currentRenderContext)
                    }
                    
                    is ResumeElement.ChartElement -> {
                        val bounds = currentMapper.gridToPdfRect(renderPosition)
                        chartRenderer.render(canvas, child, bounds, currentMapper, currentRenderContext)
                    }
                    
                    is ResumeElement.IconElement -> {
                        val bounds = currentMapper.gridToPdfRect(renderPosition)
                        iconRenderer.render(canvas, child, bounds, currentMapper, currentRenderContext)
                    }
                    
                    is ResumeElement.ContactElement -> {
                        val bounds = currentMapper.gridToPdfRect(renderPosition)
                        contactRenderer.render(canvas, child, bounds, currentMapper, currentRenderContext)
                    }
                    
                    is ResumeElement.WorkExperienceElement -> {
                        val bounds = currentMapper.gridToPdfRect(renderPosition)
                        workExperienceRenderer.render(canvas, child, bounds, currentMapper, currentRenderContext)
                    }
                    
                    is ResumeElement.EducationElement -> {
                        val bounds = currentMapper.gridToPdfRect(renderPosition)
                        educationRenderer.render(canvas, child, bounds, currentMapper, currentRenderContext)
                    }
                    
                    is ResumeElement.SkillElement -> {
                        val bounds = currentMapper.gridToPdfRect(renderPosition)
                        skillRenderer.render(canvas, child, bounds, currentMapper, currentRenderContext)
                    }
                    
                    is ResumeElement.ProjectElement -> {
                        val bounds = currentMapper.gridToPdfRect(renderPosition)
                        projectRenderer.render(canvas, child, bounds, currentMapper, currentRenderContext)
                    }
                    
                    is ResumeElement.CertificationElement -> {
                        val bounds = currentMapper.gridToPdfRect(renderPosition)
                        certificationRenderer.render(canvas, child, bounds, currentMapper, currentRenderContext)
                    }
                    
                    is ResumeElement.LanguageElement -> {
                        val bounds = currentMapper.gridToPdfRect(renderPosition)
                        languageRenderer.render(canvas, child, bounds, currentMapper, currentRenderContext)
                    }
                    
                    is ResumeElement.ContainerElement -> {
                        // Nested container - also needs same position handling
                        val bounds = currentMapper.gridToPdfRect(renderPosition)
                        containerRenderer.render(canvas, child, bounds, currentMapper, currentRenderContext)
                    }
                }
            } catch (e: Exception) {
                // Log error but continue rendering other children
                android.util.Log.e("PDF_Export", "Error rendering container child: ${child.id}", e)
                e.printStackTrace()
            }
        }
    }
}

