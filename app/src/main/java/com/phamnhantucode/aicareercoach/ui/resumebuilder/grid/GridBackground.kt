package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*

/**
 * Renders a grid background for the resume editor
 */
@Composable
fun GridBackground(
    gridConfig: GridConfig,
    modifier: Modifier = Modifier,
    zoomLevel: Float = 1f,
    gridLineColor: Color = Color.LightGray.copy(alpha = 0.3f),
    majorGridLineColor: Color = Color.LightGray.copy(alpha = 0.5f),
    showMajorLines: Boolean = true,
    majorLineInterval: Int = 4 // Draw thicker line every N cells
) {
    val density = LocalDensity.current.density
    val cellSizePx = gridConfig.cellSizeDp * density * zoomLevel

    Canvas(modifier = modifier.fillMaxSize()) {
        if (!gridConfig.showGrid) return@Canvas

        val (gridWidth, gridHeight) = GridUtils.getGridSizePx(gridConfig, cellSizePx)

        // Draw vertical lines
        for (col in 0..gridConfig.columns) {
            val x = col * cellSizePx
            val isMajorLine = showMajorLines && col % majorLineInterval == 0

            drawLine(
                color = if (isMajorLine) majorGridLineColor else gridLineColor,
                start = Offset(x, 0f),
                end = Offset(x, gridHeight),
                strokeWidth = if (isMajorLine) 2f else 1f,
                pathEffect = if (isMajorLine) null else PathEffect.dashPathEffect(
                    floatArrayOf(4f, 4f),
                    0f
                )
            )
        }

        // Draw horizontal lines
        for (row in 0..gridConfig.rows) {
            val y = row * cellSizePx
            val isMajorLine = showMajorLines && row % majorLineInterval == 0

            drawLine(
                color = if (isMajorLine) majorGridLineColor else gridLineColor,
                start = Offset(0f, y),
                end = Offset(gridWidth, y),
                strokeWidth = if (isMajorLine) 2f else 1f,
                pathEffect = if (isMajorLine) null else PathEffect.dashPathEffect(
                    floatArrayOf(4f, 4f),
                    0f
                )
            )
        }
    }
}

/**
 * Renders grid coordinates/labels for debugging or reference
 */
@Composable
fun GridLabels(
    gridConfig: GridConfig,
    modifier: Modifier = Modifier,
    showRowLabels: Boolean = true,
    showColumnLabels: Boolean = true
) {
    val density = LocalDensity.current.density
    val cellSizePx = gridConfig.cellSizeDp * density

    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw column labels
        if (showColumnLabels) {
            for (col in 0 until gridConfig.columns) {
                val x = col * cellSizePx + cellSizePx / 2
                drawText(
                    text = col.toString(),
                    x = x,
                    y = 8f,
                    color = Color.Gray
                )
            }
        }

        // Draw row labels
        if (showRowLabels) {
            for (row in 0 until gridConfig.rows) {
                val y = row * cellSizePx + cellSizePx / 2
                drawText(
                    text = row.toString(),
                    x = 8f,
                    y = y,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Helper to draw text on canvas (simplified - for labels)
 */
private fun DrawScope.drawText(
    text: String,
    x: Float,
    y: Float,
    color: Color
) {
    // Note: For production, you'd use drawContext.canvas.nativeCanvas.drawText
    // with a proper Paint object. This is a placeholder.
    // In a real app, consider using Compose's Text overlays instead
}

/**
 * Renders a highlighted cell or range of cells
 */
@Composable
fun GridHighlight(
    position: GridPosition,
    gridConfig: GridConfig,
    modifier: Modifier = Modifier,
    highlightColor: Color = Color.Blue.copy(alpha = 0.2f),
    borderColor: Color = Color.Blue,
    borderWidth: Float = 2f
) {
    val density = LocalDensity.current.density
    val cellSizePx = gridConfig.cellSizeDp * density

    Canvas(modifier = modifier.fillMaxSize()) {
        val x = position.col * cellSizePx
        val y = position.row * cellSizePx
        val width = position.colSpan * cellSizePx
        val height = position.rowSpan * cellSizePx

        // Draw fill
        drawRect(
            color = highlightColor,
            topLeft = Offset(x, y),
            size = androidx.compose.ui.geometry.Size(width, height)
        )

        // Draw border
        drawRect(
            color = borderColor,
            topLeft = Offset(x, y),
            size = androidx.compose.ui.geometry.Size(width, height),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = borderWidth)
        )
    }
}

/**
 * Renders alignment guidelines when dragging
 */
@Composable
fun AlignmentGuidelines(
    guidelines: List<Guideline>,
    gridConfig: GridConfig,
    modifier: Modifier = Modifier,
    guidelineColor: Color = Color(0xFF2196F3),
    guidelineWidth: Float = 1.5f
) {
    val density = LocalDensity.current.density
    val cellSizePx = gridConfig.cellSizeDp * density

    Canvas(modifier = modifier.fillMaxSize()) {
        val (gridWidth, gridHeight) = GridUtils.getGridSizePx(gridConfig, cellSizePx)

        guidelines.forEach { guideline ->
            when (guideline.type) {
                GuidelineType.VERTICAL -> {
                    val x = guideline.position * cellSizePx
                    drawLine(
                        color = guidelineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, gridHeight),
                        strokeWidth = guidelineWidth,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(8f, 4f),
                            0f
                        )
                    )
                }
                GuidelineType.HORIZONTAL -> {
                    val y = guideline.position * cellSizePx
                    drawLine(
                        color = guidelineColor,
                        start = Offset(0f, y),
                        end = Offset(gridWidth, y),
                        strokeWidth = guidelineWidth,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(8f, 4f),
                            0f
                        )
                    )
                }
            }
        }
    }
}

/**
 * Guideline data for alignment helpers
 */
data class Guideline(
    val type: GuidelineType,
    val position: Int // Grid column or row
)

enum class GuidelineType {
    VERTICAL,
    HORIZONTAL
}

/**
 * Calculates alignment guidelines based on current drag position
 */
fun calculateAlignmentGuidelines(
    draggedPosition: GridPosition,
    otherElements: List<ResumeElement>,
    threshold: Int = 1 // How many cells away to show guidelines
): List<Guideline> {
    val guidelines = mutableListOf<Guideline>()

    otherElements.forEach { element ->
        val pos = element.position

        // Check left edge alignment
        if (kotlin.math.abs(draggedPosition.col - pos.col) <= threshold) {
            guidelines.add(Guideline(GuidelineType.VERTICAL, pos.col))
        }

        // Check right edge alignment
        val draggedRight = draggedPosition.col + draggedPosition.colSpan
        val elementRight = pos.col + pos.colSpan
        if (kotlin.math.abs(draggedRight - elementRight) <= threshold) {
            guidelines.add(Guideline(GuidelineType.VERTICAL, elementRight))
        }

        // Check top edge alignment
        if (kotlin.math.abs(draggedPosition.row - pos.row) <= threshold) {
            guidelines.add(Guideline(GuidelineType.HORIZONTAL, pos.row))
        }

        // Check bottom edge alignment
        val draggedBottom = draggedPosition.row + draggedPosition.rowSpan
        val elementBottom = pos.row + pos.rowSpan
        if (kotlin.math.abs(draggedBottom - elementBottom) <= threshold) {
            guidelines.add(Guideline(GuidelineType.HORIZONTAL, elementBottom))
        }
    }

    // Remove duplicates
    return guidelines.distinctBy { it.type to it.position }
}
