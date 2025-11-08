package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
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
    zoomLevel: Float = 1f,
    isSelected: Boolean = false,
    isDragging: Boolean = false,
    onDragStart: (ResumeElement) -> Unit = { _ -> },
    onDrag: (ResumeElement, GridPosition) -> Unit = { _, _ -> },
    onDragEnd: (ResumeElement, GridPosition) -> Unit = { _, _ -> },
    onResize: (ResumeElement, GridPosition) -> Unit = { _, _ -> },
    onSelect: (ResumeElement) -> Unit = { _ -> },
    onDeselect: () -> Unit = {},
    onOpenProperties: (ResumeElement) -> Unit = { _ -> },
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current.density
    val cellSizePx = gridConfig.cellSizeDp * density * zoomLevel

    // Offset states for drag tracking
    var offsetX by remember(element.id) { mutableFloatStateOf(0f) }
    var offsetY by remember(element.id) { mutableFloatStateOf(0f) }
    var currentDragPosition by remember(element.id) { mutableStateOf(element.position) }

    // Calculate base position in pixels (no animation - instant update)
    val baseX = element.position.col * cellSizePx
    val baseY = element.position.row * cellSizePx

    // Track the last known position to detect when it changes
    var lastPosition by remember(element.id) { mutableStateOf(element.position) }

    // When element position changes, reset offsets synchronously
    if (element.position != lastPosition) {
        offsetX = 0f
        offsetY = 0f
        lastPosition = element.position
    }

    // Calculate size in pixels
    // For ShapeElements with custom dimensions, use those instead of grid-based sizing
    val width = if (element is ResumeElement.ShapeElement && element.customWidthDp != null) {
        element.customWidthDp * density * zoomLevel
    } else {
        element.position.colSpan * cellSizePx
    }

    val height = if (element is ResumeElement.ShapeElement && element.customHeightDp != null) {
        element.customHeightDp * density * zoomLevel
    } else {
        element.position.rowSpan * cellSizePx
    }

    // For thin elements (like dividers), use a minimum interaction height to make selection easier
    // Visual rendering stays thin, but the clickable/selectable area is larger
    val minInteractionHeightPx = 24f * density
    val interactionHeight = if (element is ResumeElement.ShapeElement &&
        (element.shapeType == ShapeType.DIVIDER || element.shapeType == ShapeType.LINE) &&
        height < minInteractionHeightPx) {
        minInteractionHeightPx
    } else {
        height
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
                height = GridUtils.pxToDp(interactionHeight, density)
            )
            .graphicsLayer {
                // Scale up slightly when dragging for visual feedback
                val scale = if (isDragging) 1.05f else 1f
                scaleX = scale
                scaleY = scale

                // Add shadow/elevation effect (but not for dividers/lines)
                val isDividerOrLine = element is ResumeElement.ShapeElement &&
                    (element.shapeType == ShapeType.DIVIDER || element.shapeType == ShapeType.LINE)
                shadowElevation = if (isDividerOrLine) {
                    0f
                } else {
                    if (isDragging) 8f else if (isSelected) 4f else 0f
                }
            }
            .pointerInput(element.id, element.position.row, element.position.col, gridConfig, isSelected, width, interactionHeight) {
                awaitEachGesture {
                    val down = awaitFirstDown()

                    // Check if touch is on a resize handle (only for selected, unlocked elements)
                    if (isSelected && !element.locked) {
                        val handleHitSize = 16.dp.toPx() // Hit area for handles (2x the 8dp visual size)
                        val touchX = down.position.x
                        val touchY = down.position.y

                        if (isTouchOnResizeHandle(touchX, touchY, width, height, handleHitSize)) {
                            // Touch is on a resize handle - don't consume, let resize handle process it
                            return@awaitEachGesture
                        }
                    }

                    var hasDragged = false
                    var dragStartCalled = false

                    // Try to detect drag
                    val dragResult = drag(down.id) { change ->
                        if (!hasDragged) {
                            hasDragged = true
                            if (!dragStartCalled) {
                                onDragStart(element)
                                dragStartCalled = true
                            }
                        }

                        val dragAmount = change.positionChange()
                        change.consume()

                        offsetX += dragAmount.x
                        offsetY += dragAmount.y

                        // Calculate grid bounds in pixels
                        val maxX = (gridConfig.columns - element.position.colSpan) * cellSizePx
                        val maxY = (gridConfig.rows - element.position.rowSpan) * cellSizePx

                        // Clamp offsets to keep element within bounds
                        val clampedX = (baseX + offsetX).coerceIn(0f, maxX)
                        val clampedY = (baseY + offsetY).coerceIn(0f, maxY)

                        // Update offsets to clamped values
                        offsetX = clampedX - baseX
                        offsetY = clampedY - baseY

                        // Calculate current grid position with snap
                        val (newRow, newCol) = GridUtils.offsetToGridPosition(
                            offsetX = clampedX,
                            offsetY = clampedY,
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
                    }

                    // Handle end of gesture
                    if (!hasDragged) {
                        // This was a tap, not a drag
                        if (isSelected) {
                            // Already selected - open properties panel
                            onOpenProperties(element)
                        } else {
                            // Not selected - select it
                            onSelect(element)
                        }
                    } else {
                        // This was a drag - handle position update
                        val finalPosition = if (gridConfig.snapToGrid) {
                            val snappedX = GridUtils.snapToGrid(baseX + offsetX, cellSizePx)
                            val snappedY = GridUtils.snapToGrid(baseY + offsetY, cellSizePx)

                            val row = GridUtils.pxToGrid(snappedY, cellSizePx)
                            val col = GridUtils.pxToGrid(snappedX, cellSizePx)

                            val position = GridPosition(
                                row = row,
                                col = col,
                                rowSpan = element.position.rowSpan,
                                colSpan = element.position.colSpan
                            )

                            // Clamp to ensure it's within bounds
                            GridUtils.clampPosition(position, gridConfig)
                        } else {
                            // Clamp the current drag position to bounds
                            GridUtils.clampPosition(currentDragPosition, gridConfig)
                        }

                        onDragEnd(element, finalPosition)
                    }
                }
            }
    ) {
        // Selection border
        if (isSelected) {
            SelectionBorder(
                element = element,
                gridConfig = gridConfig,
                cellSizePx = cellSizePx,
                zoomLevel = zoomLevel,
                onResize = onResize
            )
        }

        // Lock indicator
        if (element.locked) {
            LockIndicator()
        }

        // Properties button (floating near selected element)
        if (isSelected && !element.locked) {
            PropertiesButton(
                onOpenProperties = { onOpenProperties(element) }
            )
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
    gridConfig: GridConfig,
    cellSizePx: Float,
    zoomLevel: Float,
    onResize: (ResumeElement, GridPosition) -> Unit
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
        ResizeHandles(
            element = element,
            cellSizePx = cellSizePx,
            zoomLevel = zoomLevel,
            onResize = onResize
        )
    }
}

/**
 * Resize handles at corners and edges
 */
@Composable
private fun BoxScope.ResizeHandles(
    element: ResumeElement,
    cellSizePx: Float,
    zoomLevel: Float,
    onResize: (ResumeElement, GridPosition) -> Unit
) {
    val handleSize = 8.dp
    val handleColor = Color(0xFF2196F3)

    // Key by element.id to reset state when element changes
    var accumulatedDeltaX by remember(element.id) { mutableFloatStateOf(0f) }
    var accumulatedDeltaY by remember(element.id) { mutableFloatStateOf(0f) }

    // Use rememberUpdatedState to always get the latest position without recomposition
    val currentPosition = rememberUpdatedState(element.position)

    // Capture the position at the START of resize gesture (nullable, only set during gesture)
    var gestureStartPosition by remember(element.id) { mutableStateOf<GridPosition?>(null) }

    val onResizeStart: () -> Unit = {
        // Capture the CURRENT position when resize starts
        gestureStartPosition = currentPosition.value
        accumulatedDeltaX = 0f
        accumulatedDeltaY = 0f
    }

    val handleResize: (ResizeHandle, Float, Float) -> Unit = { handle, deltaX, deltaY ->
        accumulatedDeltaX += deltaX
        accumulatedDeltaY += deltaY

        // Use captured gesture start position, fallback to current if not set
        val basePosition = gestureStartPosition ?: currentPosition.value

        val newPosition = calculateResizedPosition(
            originalPosition = basePosition,
            handle = handle,
            deltaX = accumulatedDeltaX,
            deltaY = accumulatedDeltaY,
            cellSizePx = cellSizePx
        )
        onResize(element, newPosition)
    }

    val resetAccumulated: () -> Unit = {
        accumulatedDeltaX = 0f
        accumulatedDeltaY = 0f
        gestureStartPosition = null  // Clear gesture state so next resize captures fresh position
    }

    // Top-left
    ResizeHandle(
        handle = ResizeHandle.TOP_LEFT,
        size = handleSize,
        color = handleColor,
        alignment = Alignment.TopStart,
        onResize = handleResize,
        onResizeStart = onResizeStart,
        onResizeEnd = resetAccumulated
    )

    // Top-right
    ResizeHandle(
        handle = ResizeHandle.TOP_RIGHT,
        size = handleSize,
        color = handleColor,
        alignment = Alignment.TopEnd,
        onResize = handleResize,
        onResizeStart = onResizeStart,
        onResizeEnd = resetAccumulated
    )

    // Bottom-left
    ResizeHandle(
        handle = ResizeHandle.BOTTOM_LEFT,
        size = handleSize,
        color = handleColor,
        alignment = Alignment.BottomStart,
        onResize = handleResize,
        onResizeStart = onResizeStart,
        onResizeEnd = resetAccumulated
    )

    // Bottom-right
    ResizeHandle(
        handle = ResizeHandle.BOTTOM_RIGHT,
        size = handleSize,
        color = handleColor,
        alignment = Alignment.BottomEnd,
        onResize = handleResize,
        onResizeStart = onResizeStart,
        onResizeEnd = resetAccumulated
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
    onResize: (ResizeHandle, Float, Float) -> Unit,
    onResizeStart: () -> Unit = {},
    onResizeEnd: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .size(size * 2) // Increase hit area for easier dragging
            .zIndex(10f) // Ensure handles are on top and receive events first
            .pointerInput(handle) {
                detectDragGestures(
                    onDragStart = {
                        onResizeStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onResize(handle, dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = {
                        onResizeEnd()
                    }
                )
            }
    ) {
        // Visual handle (smaller than the hit area)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(size)
                .background(color, RoundedCornerShape(size / 2))
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
 * Properties button for opening property panel
 */
@Composable
private fun BoxScope.PropertiesButton(
    onOpenProperties: () -> Unit
) {
    androidx.compose.material3.FloatingActionButton(
        onClick = onOpenProperties,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 28.dp, y = (-8).dp)
            .size(28.dp),
        containerColor = Color(0xFF2196F3),
        contentColor = Color.White,
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Properties",
            modifier = Modifier.size(16.dp)
        )
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
    zoomLevel: Float = 1f,
    isValid: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current.density
    val cellSizePx = gridConfig.cellSizeDp * density * zoomLevel

    val x = position.col * cellSizePx
    val y = position.row * cellSizePx

    // Calculate width and height, respecting custom dimensions for ShapeElements
    val width = if (element is ResumeElement.ShapeElement && element.customWidthDp != null) {
        element.customWidthDp * density * zoomLevel
    } else {
        position.colSpan * cellSizePx
    }

    val height = if (element is ResumeElement.ShapeElement && element.customHeightDp != null) {
        element.customHeightDp * density * zoomLevel
    } else {
        position.rowSpan * cellSizePx
    }

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
 * Check if a touch position is on any resize handle
 * @param touchX Touch position X relative to element
 * @param touchY Touch position Y relative to element
 * @param elementWidth Element width in pixels
 * @param elementHeight Element height in pixels
 * @param handleSizePx Hit area size in pixels (typically 16dp converted to px)
 * @return true if touch is on a resize handle
 */
fun isTouchOnResizeHandle(
    touchX: Float,
    touchY: Float,
    elementWidth: Float,
    elementHeight: Float,
    handleSizePx: Float
): Boolean {
    // Top-left corner
    if (touchX <= handleSizePx && touchY <= handleSizePx) {
        return true
    }

    // Top-right corner
    if (touchX >= elementWidth - handleSizePx && touchY <= handleSizePx) {
        return true
    }

    // Bottom-left corner
    if (touchX <= handleSizePx && touchY >= elementHeight - handleSizePx) {
        return true
    }

    // Bottom-right corner
    if (touchX >= elementWidth - handleSizePx && touchY >= elementHeight - handleSizePx) {
        return true
    }

    return false
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

    val newPosition = when (handle) {
        ResizeHandle.TOP_LEFT -> {
            val newRowSpan = (originalPosition.rowSpan - deltaRow).coerceAtLeast(1)
            val newColSpan = (originalPosition.colSpan - deltaCol).coerceAtLeast(1)
            val newRow = originalPosition.row + (originalPosition.rowSpan - newRowSpan)
            val newCol = originalPosition.col + (originalPosition.colSpan - newColSpan)

            originalPosition.copy(
                row = newRow.coerceAtLeast(0),
                col = newCol.coerceAtLeast(0),
                rowSpan = newRowSpan,
                colSpan = newColSpan
            )
        }
        ResizeHandle.TOP_RIGHT -> {
            val newRowSpan = (originalPosition.rowSpan - deltaRow).coerceAtLeast(1)
            val newColSpan = (originalPosition.colSpan + deltaCol).coerceAtLeast(1)
            val newRow = originalPosition.row + (originalPosition.rowSpan - newRowSpan)

            originalPosition.copy(
                row = newRow.coerceAtLeast(0),
                rowSpan = newRowSpan,
                colSpan = newColSpan
            )
        }
        ResizeHandle.BOTTOM_LEFT -> {
            val newRowSpan = (originalPosition.rowSpan + deltaRow).coerceAtLeast(1)
            val newColSpan = (originalPosition.colSpan - deltaCol).coerceAtLeast(1)
            val newCol = originalPosition.col + (originalPosition.colSpan - newColSpan)

            originalPosition.copy(
                col = newCol.coerceAtLeast(0),
                rowSpan = newRowSpan,
                colSpan = newColSpan
            )
        }
        ResizeHandle.BOTTOM_RIGHT -> originalPosition.copy(
            rowSpan = (originalPosition.rowSpan + deltaRow).coerceAtLeast(1),
            colSpan = (originalPosition.colSpan + deltaCol).coerceAtLeast(1)
        )
        else -> originalPosition // Edge handles not implemented yet
    }

    return newPosition
}
