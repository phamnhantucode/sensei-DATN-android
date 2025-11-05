package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A draggable element on the grid
 * This composable handles drag gestures, snap-to-grid, and selection
 */
@Composable
fun DraggableElement(
    element: ResumeElement,
    gridConfig: GridConfig,
    isSelected: Boolean = false,
    isDragging: Boolean = false,
    onDragStart: (ResumeElement) -> Unit = { _ -> },
    onDrag: (ResumeElement, GridPosition) -> Unit = { _, _ -> },
    onDragEnd: (ResumeElement, GridPosition) -> Unit = { _, _ -> },
    onSelect: (ResumeElement) -> Unit = { _ -> },
    onDeselect: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current.density
    val cellSizePx = gridConfig.cellSizeDp * density

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var currentDragPosition by remember { mutableStateOf(element.position) }

    // Calculate base position in pixels
    val baseX = element.position.col * cellSizePx
    val baseY = element.position.row * cellSizePx

    // Calculate size in pixels
    // For ShapeElements with custom dimensions, use those instead of grid-based sizing
    val width = if (element is ResumeElement.ShapeElement && element.customWidthDp != null) {
        element.customWidthDp * density
    } else {
        element.position.colSpan * cellSizePx
    }

    val height = if (element is ResumeElement.ShapeElement && element.customHeightDp != null) {
        element.customHeightDp * density
    } else {
        element.position.rowSpan * cellSizePx
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (baseX + offsetX).roundToInt(),
                    y = (baseY + offsetY).roundToInt()
                )
            }
            .size(
                width = GridUtils.pxToDp(width, density),
                height = GridUtils.pxToDp(height, density)
            )
            .graphicsLayer {
                // Scale up slightly when dragging for visual feedback
                val scale = if (isDragging) 1.05f else 1f
                scaleX = scale
                scaleY = scale

                // Add shadow/elevation effect
                shadowElevation = if (isDragging) 8f else if (isSelected) 4f else 0f
            }
            .pointerInput(element.id, gridConfig) {
                detectDragGestures(
                    onDragStart = { _ ->
                        onDragStart(element)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        offsetX += dragAmount.x
                        offsetY += dragAmount.y

                        // Calculate current grid position with snap
                        val (newRow, newCol) = GridUtils.offsetToGridPosition(
                            offsetX = baseX + offsetX,
                            offsetY = baseY + offsetY,
                            cellSizePx = cellSizePx,
                            snapEnabled = gridConfig.snapToGrid,
                            threshold = gridConfig.snapThreshold
                        )

                        currentDragPosition = GridPosition(
                            row = newRow,
                            col = newCol,
                            rowSpan = element.position.rowSpan,
                            colSpan = element.position.colSpan
                        )

                        onDrag(element, currentDragPosition)
                    },
                    onDragEnd = {
                        // Snap to final grid position
                        val finalPosition = if (gridConfig.snapToGrid) {
                            val snappedX = GridUtils.snapToGrid(baseX + offsetX, cellSizePx)
                            val snappedY = GridUtils.snapToGrid(baseY + offsetY, cellSizePx)

                            val row = GridUtils.pxToGrid(snappedY, cellSizePx)
                            val col = GridUtils.pxToGrid(snappedX, cellSizePx)

                            GridPosition(
                                row = row,
                                col = col,
                                rowSpan = element.position.rowSpan,
                                colSpan = element.position.colSpan
                            )
                        } else {
                            currentDragPosition
                        }

                        onDragEnd(element, finalPosition)

                        // Reset offset
                        offsetX = 0f
                        offsetY = 0f
                    }
                )
            }
            .pointerInput(element.id) {
                detectTapGestures(
                    onTap = {
                        if (isSelected) {
                            onDeselect()
                        } else {
                            onSelect(element)
                        }
                    }
                )
            }
    ) {
        // Selection border
        if (isSelected) {
            SelectionBorder(
                element = element,
                onResize = { _, _, _ -> /* TODO: Implement resize */ }
            )
        }

        // Drag handle (top-left corner)
        if (isSelected && !element.locked) {
            DragHandle()
        }

        // Lock indicator
        if (element.locked) {
            LockIndicator()
        }

        // Content
        content()
    }
}

/**
 * Selection border with resize handles
 */
@Composable
private fun BoxScope.SelectionBorder(
    element: ResumeElement,
    onResize: (ResizeHandle, Float, Float) -> Unit = { _, _, _ -> }
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .border(
                width = 2.dp,
                color = Color(0xFF2196F3),
                shape = RoundedCornerShape(4.dp)
            )
    )

    // Resize handles (corners and edges)
    if (!element.locked) {
        ResizeHandles(onResize)
    }
}

/**
 * Resize handles at corners and edges
 */
@Composable
private fun BoxScope.ResizeHandles(
    onResize: (ResizeHandle, Float, Float) -> Unit
) {
    val handleSize = 8.dp
    val handleColor = Color(0xFF2196F3)

    // Top-left
    ResizeHandle(
        handle = ResizeHandle.TOP_LEFT,
        size = handleSize,
        color = handleColor,
        alignment = Alignment.TopStart,
        onResize = onResize
    )

    // Top-right
    ResizeHandle(
        handle = ResizeHandle.TOP_RIGHT,
        size = handleSize,
        color = handleColor,
        alignment = Alignment.TopEnd,
        onResize = onResize
    )

    // Bottom-left
    ResizeHandle(
        handle = ResizeHandle.BOTTOM_LEFT,
        size = handleSize,
        color = handleColor,
        alignment = Alignment.BottomStart,
        onResize = onResize
    )

    // Bottom-right
    ResizeHandle(
        handle = ResizeHandle.BOTTOM_RIGHT,
        size = handleSize,
        color = handleColor,
        alignment = Alignment.BottomEnd,
        onResize = onResize
    )

    // Edge handles (optional - can add later)
}

/**
 * Single resize handle
 */
@Composable
private fun BoxScope.ResizeHandle(
    handle: ResizeHandle,
    size: androidx.compose.ui.unit.Dp,
    color: Color,
    alignment: Alignment,
    onResize: (ResizeHandle, Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .size(size)
            .background(color, RoundedCornerShape(size / 2))
            .pointerInput(handle) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onResize(handle, dragAmount.x, dragAmount.y)
                }
            }
    )
}

/**
 * Drag handle indicator
 */
@Composable
private fun BoxScope.DragHandle() {
    Surface(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(4.dp)
            .size(20.dp),
        color = Color(0xFF2196F3).copy(alpha = 0.8f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag",
            tint = Color.White,
            modifier = Modifier.padding(2.dp)
        )
    }
}

/**
 * Lock indicator for locked elements
 */
@Composable
private fun BoxScope.LockIndicator() {
    Surface(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .size(20.dp),
        color = Color.Gray.copy(alpha = 0.8f),
        shape = RoundedCornerShape(4.dp)
    ) {
        // Lock icon would go here
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Simple lock representation
        }
    }
}

/**
 * Ghost preview of element being dragged
 */
@Composable
fun DragGhost(
    element: ResumeElement,
    position: GridPosition,
    gridConfig: GridConfig,
    isValid: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current.density
    val cellSizePx = gridConfig.cellSizeDp * density

    val x = position.col * cellSizePx
    val y = position.row * cellSizePx
    val width = position.colSpan * cellSizePx
    val height = position.rowSpan * cellSizePx

    Box(
        modifier = Modifier
            .offset {
                IntOffset(x.roundToInt(), y.roundToInt())
            }
            .size(
                width = GridUtils.pxToDp(width, density),
                height = GridUtils.pxToDp(height, density)
            )
            .graphicsLayer {
                alpha = 0.5f
            }
            .background(
                color = if (isValid) {
                    Color(0xFF4CAF50).copy(alpha = 0.3f)
                } else {
                    Color(0xFFF44336).copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 2.dp,
                color = if (isValid) {
                    Color(0xFF4CAF50)
                } else {
                    Color(0xFFF44336)
                },
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        content()
    }
}

/**
 * Resize handle positions
 */
enum class ResizeHandle {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
}

/**
 * Helper to calculate new position after resize
 */
fun calculateResizedPosition(
    originalPosition: GridPosition,
    handle: ResizeHandle,
    deltaX: Float,
    deltaY: Float,
    cellSizePx: Float
): GridPosition {
    val deltaCol = (deltaX / cellSizePx).roundToInt()
    val deltaRow = (deltaY / cellSizePx).roundToInt()

    return when (handle) {
        ResizeHandle.TOP_LEFT -> originalPosition.copy(
            row = originalPosition.row + deltaRow,
            col = originalPosition.col + deltaCol,
            rowSpan = (originalPosition.rowSpan - deltaRow).coerceAtLeast(1),
            colSpan = (originalPosition.colSpan - deltaCol).coerceAtLeast(1)
        )
        ResizeHandle.TOP_RIGHT -> originalPosition.copy(
            row = originalPosition.row + deltaRow,
            rowSpan = (originalPosition.rowSpan - deltaRow).coerceAtLeast(1),
            colSpan = (originalPosition.colSpan + deltaCol).coerceAtLeast(1)
        )
        ResizeHandle.BOTTOM_LEFT -> originalPosition.copy(
            col = originalPosition.col + deltaCol,
            rowSpan = (originalPosition.rowSpan + deltaRow).coerceAtLeast(1),
            colSpan = (originalPosition.colSpan - deltaCol).coerceAtLeast(1)
        )
        ResizeHandle.BOTTOM_RIGHT -> originalPosition.copy(
            rowSpan = (originalPosition.rowSpan + deltaRow).coerceAtLeast(1),
            colSpan = (originalPosition.colSpan + deltaCol).coerceAtLeast(1)
        )
        else -> originalPosition // Edge handles not implemented yet
    }
}
