package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models

import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.pagination.LinkedElementGroup
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.pagination.PagePaginationInfo
import java.util.UUID

/**
 * Main grid-based resume data structure
 * @param id Unique identifier
 * @param userId Owner's user ID
 * @param name Resume name/title
 * @param pages List of pages (supports multi-page resumes)
 * @param gridConfig Grid configuration
 * @param globalStyles Global styling defaults
 * @param metadata Resume metadata
 * @param linkedElementGroups Cross-page element linking for pagination
 * @param thumbnail Base64 encoded thumbnail image for preview
 */
data class GridResume(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String = "Untitled Resume",
    val pages: List<ResumePage> = listOf(ResumePage()),
    val gridConfig: GridConfig = GridConfig(),
    val globalStyles: GlobalStyles = GlobalStyles(),
    val metadata: ResumeMetadata = ResumeMetadata(),
    val linkedElementGroups: List<LinkedElementGroup> = emptyList(),
    val thumbnail: String = ""
) {
    /**
     * Get total page count
     */
    val pageCount: Int get() = pages.size
    
    /**
     * Find which page contains a specific element
     */
    fun findPageForElement(elementId: String): Int? {
        pages.forEachIndexed { index, page ->
            if (page.elements.any { it.id == elementId }) {
                return index
            }
        }
        return null
    }
    
    /**
     * Get the linked group for an element (if it's part of a split element)
     */
    fun getLinkedGroupForElement(elementId: String): LinkedElementGroup? {
        return linkedElementGroups.find { it.containsElement(elementId) }
    }
}

/**
 * A single page in the resume
 * @param id Page identifier
 * @param elements List of elements on this page
 * @param backgroundColor Background color of the page
 * @param layoutMode Page-level layout mode (FREE by default)
 * @param paginationInfo Pagination metadata for multi-page support
 */
data class ResumePage(
    val id: String = UUID.randomUUID().toString(),
    val elements: List<ResumeElement> = emptyList(),
    val backgroundColor: Long = 0xFFFFFFFF,
    val layoutMode: LayoutMode = LayoutMode.FREE,
    val paginationInfo: PagePaginationInfo? = null
) {
    /**
     * Check if this page is an auto-generated overflow page
     */
    val isOverflowPage: Boolean
        get() = paginationInfo?.isOverflowPage == true
    
    /**
     * Check if this page has elements that continue from the previous page
     */
    val hasContinuedContent: Boolean
        get() = paginationInfo?.hasContinuedElements == true
    
    /**
     * Check if this page has elements that overflow to the next page
     */
    val hasOverflowingContent: Boolean
        get() = paginationInfo?.hasOverflowingElements == true
}

/**
 * Grid configuration
 * @param columns Number of columns (default: 48)
 * @param rows Number of rows (default: 68 to match A4 aspect ratio √2)
 * @param cellSizeDp Size of each cell in DP
 * @param showGrid Whether to show grid lines
 * @param snapToGrid Whether to snap elements to grid
 * @param snapThreshold Snap threshold as fraction of cell size (0.0-1.0)
 * @param showPageNumbers Whether to show page numbers on exported PDF
 * @param pageNumberPosition Position of page numbers on the page
 */
data class GridConfig(
    val columns: Int = 96,
    val rows: Int = 136,  // 96 × √2 ≈ 135.76, rounded to 136 for A4 aspect ratio
    val cellSizeDp: Float = CELL_SIZE_FOR_A4,  // Calculated to match A4 dimensions exactly
    val showGrid: Boolean = true,
    val snapToGrid: Boolean = true,
    val snapThreshold: Float = 0.3f, // 30% of cell size
    val showPageNumbers: Boolean = false,
    val pageNumberPosition: PageNumberPosition = PageNumberPosition.BOTTOM_CENTER
) {
    companion object {
        /**
         * Optimal cell size calculated to make a 96x136 grid match A4 paper dimensions exactly.
         *
         * A4 at 72 DPI = 595 x 842 points
         * For 136 rows: 842 / 136 = 6.1911765 points per row (height-based)
         *
         * Using height-based calculation ensures:
         * - Grid height = 136 * 6.1911765 = 842 points (exact match)
         * - Grid width = 96 * 6.1911765 = 594.35 points (fits within 595 with minimal centering)
         * - Uniform scale factor = 1.0 (no distortion, perfect 1:1 rendering)
         *
         * This eliminates scaling artifacts and ensures pixel-perfect consistency
         * between the editor canvas and exported PDF.
         */
        const val CELL_SIZE_FOR_A4 = 6.1911765f
    }
}

/**
 * Position options for page numbers
 */
enum class PageNumberPosition {
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT
}

/**
 * Resume metadata
 */
data class ResumeMetadata(
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val editorType: EditorType = EditorType.GRID,
    val templateId: String = "classic" // Template type: classic, modern, minimal_image, minimal
)
