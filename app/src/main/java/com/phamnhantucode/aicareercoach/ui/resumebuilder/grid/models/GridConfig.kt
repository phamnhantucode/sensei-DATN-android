package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models

import java.util.UUID

/**
 * Main grid-based resume data structure
 * @param id Unique identifier
 * @param userId Owner's user ID
 * @param name Resume name/title
 * @param pages List of pages (usually just one for now)
 * @param gridConfig Grid configuration
 * @param globalStyles Global styling defaults
 * @param metadata Resume metadata
 */
data class GridResume(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String = "Untitled Resume",
    val pages: List<ResumePage> = listOf(ResumePage()),
    val gridConfig: GridConfig = GridConfig(),
    val globalStyles: GlobalStyles = GlobalStyles(),
    val metadata: ResumeMetadata = ResumeMetadata()
)

/**
 * A single page in the resume
 * @param id Page identifier
 * @param elements List of elements on this page
 * @param backgroundColor Background color of the page
 */
data class ResumePage(
    val id: String = UUID.randomUUID().toString(),
    val elements: List<ResumeElement> = emptyList(),
    val backgroundColor: Long = 0xFFFFFFFF,
    val layoutMode: LayoutMode = LayoutMode.FREE // Page-level layout mode - FREE by default
)

/**
 * Grid configuration
 * @param columns Number of columns (default: 48)
 * @param rows Number of rows (default: 68 to match A4 aspect ratio √2)
 * @param cellSizeDp Size of each cell in DP
 * @param showGrid Whether to show grid lines
 * @param snapToGrid Whether to snap elements to grid
 * @param snapThreshold Snap threshold as fraction of cell size (0.0-1.0)
 */
data class GridConfig(
    val columns: Int = 48,
    val rows: Int = 68,  // 48 × √2 ≈ 67.9, rounded to 68 for A4 aspect ratio
    val cellSizeDp: Float = CELL_SIZE_FOR_A4,  // Calculated to match A4 dimensions exactly
    val showGrid: Boolean = true,
    val snapToGrid: Boolean = true,
    val snapThreshold: Float = 0.3f // 30% of cell size
) {
    companion object {
        /**
         * Optimal cell size calculated to make a 48x68 grid match A4 paper dimensions exactly.
         *
         * A4 at 72 DPI = 595 x 842 points
         * For 68 rows: 842 / 68 = 12.382353 points per row (height-based)
         *
         * Using height-based calculation ensures:
         * - Grid height = 68 * 12.382353 = 842 points (exact match)
         * - Grid width = 48 * 12.382353 = 594.35 points (fits within 595 with minimal centering)
         * - Uniform scale factor = 1.0 (no distortion, perfect 1:1 rendering)
         *
         * This eliminates scaling artifacts and ensures pixel-perfect consistency
         * between the editor canvas and exported PDF.
         */
        const val CELL_SIZE_FOR_A4 = 12.382353f
    }
}

/**
 * Resume metadata
 */
data class ResumeMetadata(
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val editorType: EditorType = EditorType.GRID
)
