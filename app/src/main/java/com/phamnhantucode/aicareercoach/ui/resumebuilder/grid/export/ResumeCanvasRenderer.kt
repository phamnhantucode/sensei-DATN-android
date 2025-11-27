package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export

import android.content.Context
import android.graphics.Canvas
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridConfig
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridPosition
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridResume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.LayoutMode
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.renderers.*

/**
 * Shared Canvas Renderer
 * 
 * Handles rendering of resume elements to any Android Canvas.
 * Used by both PDF Export (AndroidPdfGenerator) and Thumbnail Generation (ResumePageRenderer).
 * Ensures consistent visual output between previews and exported files.
 */
class ResumeCanvasRenderer(
    private val context: Context,
    private val config: PdfExportConfig,
    private val imageCache: ImageCache,
    private val colorConverter: ColorConverter
) {
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

    /**
     * Render a list of elements to the canvas
     */
    suspend fun renderElements(
        canvas: Canvas,
        elements: List<ResumeElement>,
        gridConfig: GridConfig
    ) {
        val mapper = GridCoordinateMapper(gridConfig, config)
        val renderContext = PdfRenderContext(context, config, imageCache, colorConverter)
        
        // Store current state for recursive container rendering
        currentElements = elements
        currentMapper = mapper
        currentRenderContext = renderContext

        // Collect all child IDs from containers to skip them in main render loop
        // (they will be rendered by their parent containers)
        val containerChildIds = elements
            .filterIsInstance<ResumeElement.ContainerElement>()
            .flatMap { it.children }
            .toSet()

        // Sort by z-index to ensure correct layering
        val sortedElements = elements.sortedBy { it.zIndex }

        sortedElements.forEach { element ->
            // Skip elements that are children of containers - they're rendered by their parent
            if (element.id !in containerChildIds) {
                renderSingleElement(canvas, element, mapper, renderContext)
            }
        }
    }

    /**
     * Renders a single element
     */
    private suspend fun renderSingleElement(
        canvas: Canvas,
        element: ResumeElement,
        mapper: GridCoordinateMapper,
        renderContext: PdfRenderContext
    ) {
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
            android.util.Log.e("ResumeCanvasRenderer", "Failed to render element: ${element.id} (${element::class.simpleName})", e)
        }
    }

    /**
     * Render children of a container element
     * This is called by the ContainerElementPdfRenderer's callback
     */
    private suspend fun renderContainerChildren(
        canvas: Canvas,
        container: ResumeElement.ContainerElement
    ) {
        // Find child elements by their IDs
        val childElements = currentElements.filter { element ->
            container.children.contains(element.id)
        }.sortedBy { it.zIndex }
        
        // Check if container uses vertical layout
        val isVerticalLayout = container.effectiveLayoutMode == LayoutMode.VERTICAL

        // For vertical layout, maintain child order from container.children list
        val orderedChildren = if (isVerticalLayout) {
            // Preserve the order from container.children array
            container.children.mapNotNull { childId ->
                childElements.find { it.id == childId }
            }
        } else {
            childElements
        }

        // Track accumulated Y offset for vertical layout (in dp)
        var accumulatedYDp = 0f
        // Get cell size in dp for calculations
        val cellSizeDp = currentMapper.cellWidthPoints / currentMapper.scale

        // Render each child element
        orderedChildren.forEachIndexed { index, child ->
            try {
                // Determine position based on layout mode
                val renderPosition = if (isVerticalLayout) {
                    // VERTICAL layout: position based on accumulated heights of previous children
                    // Store current offset BEFORE adding this child's height
                    val currentOffsetDp = accumulatedYDp
                    
                    // Calculate this child's height in dp
                    val childHeightDp = if (child.position.heightMode == com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.SizeMode.WRAP_CONTENT) {
                        child.position.cachedHeightDp ?: (child.position.rowSpan * cellSizeDp)
                    } else {
                        child.position.rowSpan * cellSizeDp
                    }
                    
                    // Update accumulated offset for next child
                    accumulatedYDp += childHeightDp
                    
                    GridPosition(
                        row = 0,  // Base row is 0, actual Y position handled by verticalOffsetDp
                        col = 0,
                        rowSpan = child.position.rowSpan,
                        colSpan = container.position.colSpan,  // Fill container width
                        widthMode = child.position.widthMode,
                        heightMode = child.position.heightMode,
                        cachedHeightDp = child.position.cachedHeightDp,
                        // Store the accumulated offset (position of this child)
                        verticalOffsetDp = currentOffsetDp
                    )
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
                
                // Temporarily update element position for rendering
                val relativeChild = when (child) {
                    is ResumeElement.TextElement -> child.copy(position = renderPosition)
                    is ResumeElement.ImageElement -> child.copy(position = renderPosition)
                    is ResumeElement.ShapeElement -> child.copy(position = renderPosition)
                    is ResumeElement.ChartElement -> child.copy(position = renderPosition)
                    is ResumeElement.ContainerElement -> child.copy(position = renderPosition)
                    is ResumeElement.IconElement -> child.copy(position = renderPosition)
                    is ResumeElement.ContactElement -> child.copy(position = renderPosition)
                    is ResumeElement.WorkExperienceElement -> child.copy(position = renderPosition)
                    is ResumeElement.EducationElement -> child.copy(position = renderPosition)
                    is ResumeElement.SkillElement -> child.copy(position = renderPosition)
                    is ResumeElement.ProjectElement -> child.copy(position = renderPosition)
                    is ResumeElement.CertificationElement -> child.copy(position = renderPosition)
                    is ResumeElement.LanguageElement -> child.copy(position = renderPosition)
                }
                
                // For vertical layout, apply the accumulated Y offset to the canvas
                if (isVerticalLayout && renderPosition.verticalOffsetDp != null && renderPosition.verticalOffsetDp > 0f) {
                    canvas.save()
                    val offsetPts = renderPosition.verticalOffsetDp * currentMapper.scale
                    canvas.translate(0f, offsetPts)
                    renderSingleElement(canvas, relativeChild, currentMapper, currentRenderContext)
                    canvas.restore()
                } else {
                    renderSingleElement(canvas, relativeChild, currentMapper, currentRenderContext)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ResumeCanvasRenderer", "Error rendering container child: ${child.id}", e)
            }
        }
    }
}
