package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export

import android.graphics.RectF
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.GridConfig
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.GridPosition

/**
 * Grid Coordinate Mapper
 *
 * Maps grid-based positions (12x16 cells) to PDF coordinates (points)
 * Handles density-independent pixels (dp) to PDF points conversion
 */
class GridCoordinateMapper(
    private val gridConfig: GridConfig,
    private val pdfConfig: PdfExportConfig
) {
    /**
     * Density conversion: Android DP to PDF points
     * Standard Android: 160dpi = 1dp
     * PDF: 72dpi = 1 point
     * Conversion: 1dp ≈ 0.75 points (72/96)
     */
    private val DP_TO_POINTS = 0.75f

    /**
     * Calculate cell size in PDF points
     */
    val cellWidthPoints: Float = gridConfig.cellSizeDp * DP_TO_POINTS
    val cellHeightPoints: Float = gridConfig.cellSizeDp * DP_TO_POINTS

    /**
     * Total grid dimensions in PDF points
     */
    val totalGridWidth: Float = gridConfig.columns * cellWidthPoints
    val totalGridHeight: Float = gridConfig.rows * cellHeightPoints

    /**
     * Scaling factors to fit grid into PDF page
     * This ensures the grid fits perfectly on an A4 page
     */
    private val scaleX: Float = pdfConfig.pageWidth / totalGridWidth
    private val scaleY: Float = pdfConfig.pageHeight / totalGridHeight

    /**
     * Use uniform scaling to maintain aspect ratio
     */
    private val scale: Float = minOf(scaleX, scaleY)

    /**
     * Centering offsets (if grid doesn't fill entire page)
     */
    private val offsetX: Float = (pdfConfig.pageWidth - totalGridWidth * scale) / 2
    private val offsetY: Float = (pdfConfig.pageHeight - totalGridHeight * scale) / 2

    /**
     * Convert grid position to PDF rectangle (in points)
     *
     * @param position Grid position
     * @return PDF rectangle with coordinates in points
     */
    fun gridToPdfRect(position: GridPosition): RectF {
        // Calculate raw grid coordinates
        val left = position.col * cellWidthPoints
        val top = position.row * cellHeightPoints
        val right = left + (position.colSpan * cellWidthPoints)
        val bottom = top + (position.rowSpan * cellHeightPoints)

        // Apply scaling and centering
        return RectF(
            left * scale + offsetX,
            top * scale + offsetY,
            right * scale + offsetX,
            bottom * scale + offsetY
        )
    }

    /**
     * Convert custom dimensions (in dp) to PDF points
     *
     * @param widthDp Width in density-independent pixels
     * @param heightDp Height in density-independent pixels
     * @return Pair of (width, height) in PDF points
     */
    fun dpToPdfPoints(widthDp: Float?, heightDp: Float?): Pair<Float?, Float?> {
        return Pair(
            widthDp?.let { it * DP_TO_POINTS * scale },
            heightDp?.let { it * DP_TO_POINTS * scale }
        )
    }

    /**
     * Convert font size (sp) to PDF points
     * SP (scale-independent pixels) are typically the same as DP for PDF export
     *
     * @param fontSizeSp Font size in scale-independent pixels
     * @return Font size in PDF points
     */
    fun spToPdfPoints(fontSizeSp: Float): Float {
        return fontSizeSp * DP_TO_POINTS * scale
    }

    /**
     * Get PDF coordinates for custom-sized element
     * Used for shapes with custom dimensions (e.g., thin dividers)
     *
     * @param position Grid position (for top-left anchor)
     * @param customWidthDp Custom width in dp
     * @param customHeightDp Custom height in dp
     * @return PDF rectangle
     */
    fun customSizeToRect(
        position: GridPosition,
        customWidthDp: Float?,
        customHeightDp: Float?
    ): RectF {
        val baseRect = gridToPdfRect(position)

        val (pdfWidth, pdfHeight) = dpToPdfPoints(customWidthDp, customHeightDp)

        return RectF(
            baseRect.left,
            baseRect.top,
            pdfWidth?.let { baseRect.left + it } ?: baseRect.right,
            pdfHeight?.let { baseRect.top + it } ?: baseRect.bottom
        )
    }

    /**
     * Convert corner radius from dp to PDF points
     */
    fun cornerRadiusToPdfPoints(radiusDp: Float): Float {
        return radiusDp * DP_TO_POINTS * scale
    }

    /**
     * Convert border width from dp to PDF points
     */
    fun borderWidthToPdfPoints(widthDp: Float): Float {
        return widthDp * DP_TO_POINTS * scale
    }

    /**
     * Get page dimensions
     */
    fun getPageDimensions(): Pair<Int, Int> {
        return Pair(pdfConfig.pageWidth, pdfConfig.pageHeight)
    }

    /**
     * Debugging info
     */
    override fun toString(): String = """
        GridCoordinateMapper:
        - Cell Size: ${gridConfig.cellSizeDp}dp → $cellWidthPoints pts
        - Grid: ${gridConfig.columns}x${gridConfig.rows} cells
        - Total Grid: ${totalGridWidth}x${totalGridHeight} pts
        - PDF Page: ${pdfConfig.pageWidth}x${pdfConfig.pageHeight} pts
        - Scale: $scale
        - Offset: ($offsetX, $offsetY)
    """.trimIndent()
}
