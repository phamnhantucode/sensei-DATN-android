package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridConfig
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridPosition
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
import kotlin.math.roundToInt

/**
 * Grid utility functions for snap-to-grid, collision detection, and coordinate conversion
 */
object GridUtils {

    /**
     * Converts pixel offset to grid position
     * @param offsetPx Offset in pixels
     * @param cellSizePx Cell size in pixels
     * @return Grid row/column index
     */
    fun pxToGrid(offsetPx: Float, cellSizePx: Float): Int {
        return (offsetPx / cellSizePx).toInt().coerceAtLeast(0)
    }

    /**
     * Converts grid position to pixel offset
     * @param gridIndex Grid row/column index
     * @param cellSizePx Cell size in pixels
     * @return Pixel offset
     */
    fun gridToPx(gridIndex: Int, cellSizePx: Float): Float {
        return gridIndex * cellSizePx
    }

    /**
     * Snaps a pixel offset to the nearest grid cell
     * @param offsetPx Current offset in pixels
     * @param cellSizePx Cell size in pixels
     * @return Snapped pixel offset
     */
    fun snapToGrid(offsetPx: Float, cellSizePx: Float): Float {
        val gridIndex = (offsetPx / cellSizePx).roundToInt()
        return gridIndex * cellSizePx
    }

    /**
     * Snaps with magnetic effect - only snaps if within threshold
     * @param offsetPx Current offset in pixels
     * @param cellSizePx Cell size in pixels
     * @param threshold Snap threshold as fraction (0.0 to 1.0)
     * @return Snapped or original offset
     */
    fun magneticSnap(
        offsetPx: Float,
        cellSizePx: Float,
        threshold: Float = 0.3f
    ): Float {
        val gridPosition = offsetPx / cellSizePx
        val remainder = gridPosition % 1f
        val thresholdDistance = threshold

        return when {
            // Close to the grid line on the left/top
            remainder < thresholdDistance -> {
                gridPosition.toInt() * cellSizePx
            }
            // Close to the grid line on the right/bottom
            remainder > (1 - thresholdDistance) -> {
                (gridPosition.toInt() + 1) * cellSizePx
            }
            // Not close enough - don't snap
            else -> offsetPx
        }
    }

    /**
     * Converts a pixel offset to grid position with snapping
     * @param offsetPx Pixel offset
     * @param cellSizePx Cell size in pixels
     * @param snapEnabled Whether snapping is enabled
     * @param threshold Snap threshold
     * @return Grid position
     */
    fun offsetToGridPosition(
        offsetX: Float,
        offsetY: Float,
        cellSizePx: Float,
        snapEnabled: Boolean = true,
        threshold: Float = 0.3f
    ): Pair<Int, Int> {
        val snappedX = if (snapEnabled) {
            magneticSnap(offsetX, cellSizePx, threshold)
        } else {
            offsetX
        }

        val snappedY = if (snapEnabled) {
            magneticSnap(offsetY, cellSizePx, threshold)
        } else {
            offsetY
        }

        val col = pxToGrid(snappedX, cellSizePx)
        val row = pxToGrid(snappedY, cellSizePx)

        return Pair(row, col)
    }

    /**
     * Validates if a position is within grid bounds
     * @param position Grid position
     * @param gridConfig Grid configuration
     * @return True if valid
     */
    fun isValidPosition(position: GridPosition, gridConfig: GridConfig): Boolean {
        val endRow = position.row + position.rowSpan
        val endCol = position.col + position.colSpan

        return position.row >= 0 &&
               position.col >= 0 &&
               endRow <= gridConfig.rows &&
               endCol <= gridConfig.columns
    }

    /**
     * Clamps a position to fit within grid bounds
     * @param position Grid position
     * @param gridConfig Grid configuration
     * @return Clamped position
     */
    fun clampPosition(position: GridPosition, gridConfig: GridConfig): GridPosition {
        // Ensure rowSpan and colSpan don't exceed grid size
        val safeRowSpan = position.rowSpan.coerceIn(1, gridConfig.rows)
        val safeColSpan = position.colSpan.coerceIn(1, gridConfig.columns)

        // Calculate maximum valid positions
        val maxRow = (gridConfig.rows - safeRowSpan).coerceAtLeast(0)
        val maxCol = (gridConfig.columns - safeColSpan).coerceAtLeast(0)

        val row = position.row.coerceIn(0, maxRow)
        val col = position.col.coerceIn(0, maxCol)

        return position.copy(row = row, col = col, rowSpan = safeRowSpan, colSpan = safeColSpan)
    }

    /**
     * Checks if an element at a given position collides with other elements
     * @param position Position to check
     * @param elements List of existing elements
     * @param excludeId ID of element to exclude from collision check (for moving elements)
     * @param layoutMode Layout mode that determines collision behavior (null = GRID)
     * @return True if collision detected
     */
    fun hasCollision(
        position: GridPosition,
        elements: List<ResumeElement>,
        excludeId: String? = null,
        layoutMode: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.LayoutMode? = null
    ): Boolean {
        // In FREE layout mode, elements can overlap freely
        if (layoutMode == com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.LayoutMode.FREE) {
            return false
        }
        
        // In GRID mode (or default), check for collisions
        return elements.any { element ->
            element.id != excludeId && element.position.overlaps(position)
        }
    }

    /**
     * Finds the nearest non-colliding position
     * @param desiredPosition Desired position
     * @param elements Existing elements
     * @param gridConfig Grid configuration
     * @param excludeId Element ID to exclude
     * @return Nearest valid position, or null if none found
     */
    fun findNearestValidPosition(
        desiredPosition: GridPosition,
        elements: List<ResumeElement>,
        gridConfig: GridConfig,
        excludeId: String? = null,
        maxAttempts: Int = 50
    ): GridPosition? {
        // First try the desired position
        if (!hasCollision(desiredPosition, elements, excludeId) &&
            isValidPosition(desiredPosition, gridConfig)) {
            return desiredPosition
        }

        // Try positions in a spiral pattern outward
        var distance = 1
        while (distance < maxAttempts) {
            for (rowOffset in -distance..distance) {
                for (colOffset in -distance..distance) {
                    // Skip positions not on the perimeter of current distance
                    if (rowOffset != -distance && rowOffset != distance &&
                        colOffset != -distance && colOffset != distance) {
                        continue
                    }

                    val testPosition = desiredPosition.copy(
                        row = desiredPosition.row + rowOffset,
                        col = desiredPosition.col + colOffset
                    )

                    if (isValidPosition(testPosition, gridConfig) &&
                        !hasCollision(testPosition, elements, excludeId)) {
                        return testPosition
                    }
                }
            }
            distance++
        }

        return null
    }

    /**
     * Gets all occupied grid cells
     * @param elements List of elements
     * @return Set of occupied cell coordinates (row, col)
     */
    fun getOccupiedCells(elements: List<ResumeElement>): Set<Pair<Int, Int>> {
        val occupied = mutableSetOf<Pair<Int, Int>>()

        elements.forEach { element ->
            for (row in element.position.row until element.position.row + element.position.rowSpan) {
                for (col in element.position.col until element.position.col + element.position.colSpan) {
                    occupied.add(Pair(row, col))
                }
            }
        }

        return occupied
    }

    /**
     * Finds the next available position for a new element
     * @param elementSize Size of element (rowSpan, colSpan)
     * @param elements Existing elements
     * @param gridConfig Grid configuration
     * @return Position for new element, or null if grid is full
     */
    fun findNextAvailablePosition(
        elementSize: Pair<Int, Int>, // (rowSpan, colSpan)
        elements: List<ResumeElement>,
        gridConfig: GridConfig
    ): GridPosition? {
        val (rowSpan, colSpan) = elementSize

        // Try to place from top-left, moving right then down
        for (row in 0..(gridConfig.rows - rowSpan)) {
            for (col in 0..(gridConfig.columns - colSpan)) {
                val testPosition = GridPosition(row, col, rowSpan, colSpan)

                if (!hasCollision(testPosition, elements)) {
                    return testPosition
                }
            }
        }

        return null
    }

    /**
     * Calculates the bounding box of all elements
     * @param elements List of elements
     * @return Bounding box as GridPosition, or null if no elements
     */
    fun calculateBoundingBox(elements: List<ResumeElement>): GridPosition? {
        if (elements.isEmpty()) return null

        val minRow = elements.minOf { it.position.row }
        val minCol = elements.minOf { it.position.col }
        val maxRow = elements.maxOf { it.position.row + it.position.rowSpan }
        val maxCol = elements.maxOf { it.position.col + it.position.colSpan }

        return GridPosition(
            row = minRow,
            col = minCol,
            rowSpan = maxRow - minRow,
            colSpan = maxCol - minCol
        )
    }

    /**
     * Calculates grid dimensions in pixels
     * @param gridConfig Grid configuration
     * @param cellSizePx Cell size in pixels
     * @return Pair of (width, height) in pixels
     */
    fun getGridSizePx(gridConfig: GridConfig, cellSizePx: Float): Pair<Float, Float> {
        val width = gridConfig.columns * cellSizePx
        val height = gridConfig.rows * cellSizePx
        return Pair(width, height)
    }

    /**
     * Converts Dp to Px using density
     * @param dp Value in Dp
     * @param density Screen density
     * @return Value in pixels
     */
    fun dpToPx(dp: Dp, density: Float): Float {
        return dp.value * density
    }

    /**
     * Converts Px to Dp using density
     * @param px Value in pixels
     * @param density Screen density
     * @return Value in Dp
     */
    fun pxToDp(px: Float, density: Float): Dp {
        return (px / density).dp
    }

    /**
     * Calculates the center point of a grid position in pixels
     * @param position Grid position
     * @param cellSizePx Cell size in pixels
     * @return Center point (x, y) in pixels
     */
    fun getPositionCenter(position: GridPosition, cellSizePx: Float): Pair<Float, Float> {
        val x = (position.col + position.colSpan / 2f) * cellSizePx
        val y = (position.row + position.rowSpan / 2f) * cellSizePx
        return Pair(x, y)
    }

    /**
     * Checks if two elements are aligned on any edge
     * @param pos1 First position
     * @param pos2 Second position
     * @return Alignment info (left, right, top, bottom)
     */
    fun checkAlignment(pos1: GridPosition, pos2: GridPosition): AlignmentInfo {
        val leftAligned = pos1.col == pos2.col
        val rightAligned = (pos1.col + pos1.colSpan) == (pos2.col + pos2.colSpan)
        val topAligned = pos1.row == pos2.row
        val bottomAligned = (pos1.row + pos1.rowSpan) == (pos2.row + pos2.rowSpan)

        return AlignmentInfo(
            leftAligned = leftAligned,
            rightAligned = rightAligned,
            topAligned = topAligned,
            bottomAligned = bottomAligned
        )
    }

    /**
     * Gets distance between two positions (in grid cells)
     * @param pos1 First position
     * @param pos2 Second position
     * @return Distance (Manhattan distance)
     */
    fun getDistance(pos1: GridPosition, pos2: GridPosition): Int {
        val rowDistance = kotlin.math.abs(pos1.row - pos2.row)
        val colDistance = kotlin.math.abs(pos1.col - pos2.col)
        return rowDistance + colDistance
    }

    /**
     * Calculates optimal default zoom level to fit page in viewport
     * @param gridConfig Grid configuration
     * @param availableWidthPx Available viewport width in pixels
     * @param availableHeightPx Available viewport height in pixels
     * @param cellSizePx Cell size in pixels
     * @param padding Padding factor (0.0 to 1.0) - e.g., 0.9 means 90% of viewport
     * @return Optimal zoom level (0.25 to 2.0)
     */
    fun calculateOptimalZoom(
        gridConfig: GridConfig,
        availableWidthPx: Float,
        availableHeightPx: Float,
        cellSizePx: Float,
        padding: Float = 0.9f
    ): Float {
        val (pageWidthPx, pageHeightPx) = getGridSizePx(gridConfig, cellSizePx)

        // Calculate zoom needed to fit width and height
        val zoomToFitWidth = (availableWidthPx * padding) / pageWidthPx
        val zoomToFitHeight = (availableHeightPx * padding) / pageHeightPx

        // Use the smaller zoom to ensure both dimensions fit
        val optimalZoom = minOf(zoomToFitWidth, zoomToFitHeight)

        // Clamp to reasonable zoom range
        return optimalZoom.coerceIn(0.25f, 2f)
    }
}

/**
 * Alignment information between two positions
 */
data class AlignmentInfo(
    val leftAligned: Boolean = false,
    val rightAligned: Boolean = false,
    val topAligned: Boolean = false,
    val bottomAligned: Boolean = false
) {
    val isAligned: Boolean
        get() = leftAligned || rightAligned || topAligned || bottomAligned
}
