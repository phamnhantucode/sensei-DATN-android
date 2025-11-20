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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phamnhantucode.aicareercoach.BuildConfig
import kotlin.math.roundToInt
import android.text.TextPaint
import android.text.StaticLayout
import android.text.Layout
import androidx.compose.ui.platform.LocalContext
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*

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
    enabled: Boolean = true,
    onDragStart: (ResumeElement) -> Unit = { _ -> },
    onDrag: (ResumeElement, GridPosition) -> Unit = { _, _ -> },
    onDragEnd: (ResumeElement, GridPosition) -> Unit = { _, _ -> },
    onResize: (ResumeElement, GridPosition) -> Unit = { _, _ -> },
    onSelect: (ResumeElement) -> Unit = { _ -> },
    onDeselect: () -> Unit = {},
    onOpenProperties: (ResumeElement) -> Unit = { _ -> },
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
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
    } else if (element.position.widthMode == SizeMode.WRAP_CONTENT) {
        // For wrap content, we'll use a placeholder width that will be adjusted by the content
        element.position.colSpan * cellSizePx // Use colSpan as max width
    } else {
        element.position.colSpan * cellSizePx
    }

    val height = if (element is ResumeElement.ShapeElement && element.customHeightDp != null) {
        element.customHeightDp * density * zoomLevel
    } else if (element.position.heightMode == SizeMode.WRAP_CONTENT) {
        // Calculate actual content height for wrap content mode
        val actualContentHeight = calculateContentHeight(element, width, density, zoomLevel, context)
        actualContentHeight ?: (element.position.rowSpan * cellSizePx) // Fallback to rowSpan if calculation fails
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
            .then(
                if (enabled) {
                    Modifier.pointerInput(element.id, element.position.row, element.position.col, gridConfig, isSelected, width, interactionHeight) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            // Consume down event to prevent parent from handling it
                            down.consume()

                            // Check if touch is on a resize handle (only for selected, unlocked elements)
                            if (isSelected && !element.locked) {
                                val handleHitSize = 16.dp.toPx() // Hit area for handles (2x the 8dp visual size)
                                val touchX = down.position.x
                                val touchY = down.position.y

                                if (isTouchOnResizeHandle(touchX, touchY, width, interactionHeight, handleHitSize)) {
                                    // Touch is on a resize handle - let resize handle process it
                                    return@awaitEachGesture
                                }
                            }

                            var hasDragged = false
                            var dragStartCalled = false

                            // Try to detect drag
                            if (!element.locked) {
                                // Element is unlocked - allow dragging
                                drag(down.id) { change ->
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

                                    currentDragPosition = element.position.copy(
                                        row = newRow,
                                        col = newCol
                                    )

                                    onDrag(element, currentDragPosition)
                                }
                            } else {
                                // Element is locked - consume the drag gesture without allowing movement
                                drag(down.id) { change ->
                                    // Consume the gesture but don't update position
                                    change.consume()
                                }
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

                                    val position = element.position.copy(
                                        row = row,
                                        col = col
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
                } else {
                    Modifier
                }
            )
    ) {
        // Content (rendered first, at the bottom)
        content()

        // Selection border (rendered second, above content)
        if (isSelected) {
            SelectionBorder(
                element = element,
                gridConfig = gridConfig,
                cellSizePx = cellSizePx,
                zoomLevel = zoomLevel,
                enabled = enabled,
                onResize = onResize
            )
        }

        // Lock indicator (rendered third, above selection border) - only when selected
        if (isSelected && element.locked) {
            LockIndicator()
        }

        // Template tag indicator - shows when element is selected and has a tag
        if (isSelected && element.userInfoTag != null && element.userInfoTag != UserInfoTag.NONE) {
            TemplateTagIndicator(tag = element.userInfoTag!!)
        }

        // Properties button (rendered last, on top of everything)
        if (isSelected && !element.locked) {
            PropertiesButton(
                onOpenProperties = { onOpenProperties(element) }
            )
        }
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
    enabled: Boolean,
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
    if (!element.locked && enabled) {
        ResizeHandles(
            element = element,
            cellSizePx = cellSizePx,
            zoomLevel = zoomLevel,
            gridConfig = gridConfig,
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
    gridConfig: GridConfig,
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

        // Check if we should enforce 1:1 aspect ratio (for circle shapes)
        val enforceSquare = element is ResumeElement.ShapeElement && element.shapeType == ShapeType.CIRCLE

        val newPosition = calculateResizedPosition(
            originalPosition = basePosition,
            handle = handle,
            deltaX = accumulatedDeltaX,
            deltaY = accumulatedDeltaY,
            cellSizePx = cellSizePx,
            enforceSquare = enforceSquare,
            gridConfig = gridConfig
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
            .size(20.dp)
            .zIndex(100f), // High z-index to appear above everything including shapes
        color = Color.Gray.copy(alpha = 0.8f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
        }
    }
}

/**
 * Template tag indicator
 * Shows which user info tag is applied to this element when selected
 * Displays a purple badge with the tag name (e.g., NAME, EMAIL, PHONE)
 */
@Composable
private fun BoxScope.TemplateTagIndicator(tag: UserInfoTag) {
    Surface(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(4.dp)
            .zIndex(100f),
        color = Color(0xFF9C27B0).copy(alpha = 0.9f), // Purple color for template tags
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tag.name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
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
    val context = LocalContext.current
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
    } else if (element.position.heightMode == SizeMode.WRAP_CONTENT) {
        // Calculate actual content height for wrap content mode
        // We use the element's original position heightMode, but the current width (which depends on colSpan)
        val actualContentHeight = calculateContentHeight(element, width, density, zoomLevel, context)
        actualContentHeight ?: (position.rowSpan * cellSizePx)
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
    cellSizePx: Float,
    enforceSquare: Boolean = false,
    gridConfig: GridConfig? = null
): GridPosition {
    val deltaCol = (deltaX / cellSizePx).roundToInt()
    val deltaRow = (deltaY / cellSizePx).roundToInt()

    val newPosition = when (handle) {
        ResizeHandle.TOP_LEFT -> {
            var newRowSpan = (originalPosition.rowSpan - deltaRow).coerceAtLeast(1)
            var newColSpan = (originalPosition.colSpan - deltaCol).coerceAtLeast(1)

            // Enforce 1:1 aspect ratio for squares/circles
            if (enforceSquare) {
                val minSpan = minOf(newRowSpan, newColSpan)
                newRowSpan = minSpan
                newColSpan = minSpan
            }

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
            var newRowSpan = (originalPosition.rowSpan - deltaRow).coerceAtLeast(1)
            var newColSpan = (originalPosition.colSpan + deltaCol).coerceAtLeast(1)

            // Enforce 1:1 aspect ratio for squares/circles
            if (enforceSquare) {
                val minSpan = minOf(newRowSpan, newColSpan)
                newRowSpan = minSpan
                newColSpan = minSpan
            }

            val newRow = originalPosition.row + (originalPosition.rowSpan - newRowSpan)

            originalPosition.copy(
                row = newRow.coerceAtLeast(0),
                rowSpan = newRowSpan,
                colSpan = newColSpan
            )
        }
        ResizeHandle.BOTTOM_LEFT -> {
            var newRowSpan = (originalPosition.rowSpan + deltaRow).coerceAtLeast(1)
            var newColSpan = (originalPosition.colSpan - deltaCol).coerceAtLeast(1)

            // Enforce 1:1 aspect ratio for squares/circles
            if (enforceSquare) {
                val minSpan = minOf(newRowSpan, newColSpan)
                newRowSpan = minSpan
                newColSpan = minSpan
            }

            val newCol = originalPosition.col + (originalPosition.colSpan - newColSpan)

            originalPosition.copy(
                col = newCol.coerceAtLeast(0),
                rowSpan = newRowSpan,
                colSpan = newColSpan
            )
        }
        ResizeHandle.BOTTOM_RIGHT -> {
            var newRowSpan = (originalPosition.rowSpan + deltaRow).coerceAtLeast(1)
            var newColSpan = (originalPosition.colSpan + deltaCol).coerceAtLeast(1)

            // Enforce 1:1 aspect ratio for squares/circles
            if (enforceSquare) {
                val minSpan = minOf(newRowSpan, newColSpan)
                newRowSpan = minSpan
                newColSpan = minSpan
            }

            originalPosition.copy(
                rowSpan = newRowSpan,
                colSpan = newColSpan
            )
        }
        else -> originalPosition // Edge handles not implemented yet
    }

    // Clamp to grid bounds if gridConfig is provided
    return if (gridConfig != null) {
        GridUtils.clampPosition(newPosition, gridConfig)
    } else {
        newPosition
    }
}

/**
 * Calculate the actual content height for an element when heightMode is WRAP_CONTENT
 */
private fun calculateContentHeight(
    element: ResumeElement,
    width: Float,
    density: Float,
    zoomLevel: Float,
    context: android.content.Context
): Float? {
    return when (element) {
        is ResumeElement.TextElement -> {
            calculateTextContentHeight(element, width, density, zoomLevel, context)
        }
        is ResumeElement.WorkExperienceElement -> {
            calculateWorkExperienceContentHeight(element, width, density, zoomLevel, context)
        }
        is ResumeElement.ProjectElement -> {
            calculateProjectContentHeight(element, width, density, zoomLevel, context)
        }
        // Add more element types as needed
        else -> null
    }
}

/**
 * Calculate text content height using StaticLayout (same as renderer)
 */
private fun calculateTextContentHeight(
    element: ResumeElement.TextElement,
    width: Float,
    density: Float,
    zoomLevel: Float,
    context: android.content.Context
): Float {
    if (element.content.isEmpty()) {
        return 16f * density * zoomLevel // Minimum height for empty text
    }

    // Calculate base dimensions (unscaled) - matching TextElementRenderer logic
    val basePadding = 8f * density // 8dp in pixels
    val baseWidth = (width / zoomLevel) - (basePadding * 2)
    
    // Create text paint with base text size (no zoom) - matching TextElementRenderer
    val textPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = element.textStyle.fontSize * density // No zoom applied
        color = element.textStyle.color.toInt()
        
        // Use Poppins font with proper weight mapping
        typeface = FontManager.getPoppinsTypeface(
            context,
            element.textStyle.fontWeight,
            element.textStyle.isItalic
        )
        
        isUnderlineText = element.textStyle.isUnderlined
        letterSpacing = element.textStyle.letterSpacing
    }
    
    // Create layout with base width - matching TextElementRenderer
    val layoutAlignment = when (element.alignment) {
        TextAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
        TextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
        TextAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        TextAlignment.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL
    }
    
    val lineSpacingMultiplier = element.textStyle.lineHeight?.let {
        it / element.textStyle.fontSize
    } ?: 1.0f
    
    val builder = StaticLayout.Builder
        .obtain(element.content, 0, element.content.length, textPaint, baseWidth.toInt().coerceAtLeast(1))
        .setAlignment(layoutAlignment)
        .setLineSpacing(0f, lineSpacingMultiplier)
        .setIncludePad(false)
        .setMaxLines(element.maxLines ?: Int.MAX_VALUE)
    
    if (element.alignment == TextAlignment.JUSTIFY && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        builder.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        builder.setJustificationMode(Layout.JUSTIFICATION_MODE_NONE)
    }
    
    val layout = builder.build()
    
    // Calculate actual content height
    val topPadding = layout.getLineTop(0).toFloat()
    val textHeight = layout.height.toFloat() - topPadding
    
    // Add padding and apply zoom
    val totalHeight = (textHeight + (basePadding * 2)) * zoomLevel
    
    return totalHeight
}

/**
 * Calculate work experience content height using StaticLayout (matching PDF renderer logic)
 */
private fun calculateWorkExperienceContentHeight(
    element: ResumeElement.WorkExperienceElement,
    width: Float,
    density: Float,
    zoomLevel: Float,
    context: android.content.Context
): Float {
    if (element.items.isEmpty()) {
        return 16f * density * zoomLevel // Minimum height for empty content
    }

    // Calculate base dimensions (unscaled)
    val basePadding = 8f * density // 8dp in pixels
    val baseWidth = (width / zoomLevel) - (basePadding * 2)
    
    // Create text paints for different styles (no zoom applied)
    val titlePaint = createWorkExperienceTextPaint(element.titleStyle, density, context)
    val companyPaint = createWorkExperienceTextPaint(element.companyStyle, density, context)
    val datePaint = createWorkExperienceTextPaint(element.dateStyle, density, context)
    val locationPaint = createWorkExperienceTextPaint(element.locationStyle, density, context)
    val responsibilityPaint = createWorkExperienceTextPaint(element.responsibilityStyle, density, context)
    
    // Calculate spacing values
    val spacing = element.spacing * density
    val itemSpacing = element.itemSpacing * density
    val responsibilitySpacing = element.responsibilitySpacing * density
    
    var totalHeight = 0f
    
    // Calculate height for each work experience item
    element.items.forEachIndexed { index, item ->
        var itemHeight = 0f
        var partCount = 0
        
        when (element.displayStyle) {
            WorkExperienceDisplayStyle.STANDARD -> {
                // Job Title
                if (item.jobTitle.isNotEmpty()) {
                    val layout = createSimpleLayout(item.jobTitle, titlePaint, baseWidth)
                    itemHeight += layout.height.toFloat()
                    partCount++
                }
                
                // Company (and date if shown)
                if (item.company.isNotEmpty()) {
                    val dateText = if (element.showDates) formatDateRange(item, element) else ""
                    val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
                    val availableCompanyWidth = if (dateText.isNotEmpty()) baseWidth - dateWidth - itemSpacing else baseWidth
                    
                    val companyLayout = createSimpleLayout(item.company, companyPaint, availableCompanyWidth)
                    itemHeight += companyLayout.height.toFloat()
                    partCount++
                } else if (element.showDates) {
                    val dateMetrics = datePaint.fontMetrics
                    itemHeight += dateMetrics.descent - dateMetrics.ascent
                    partCount++
                }
                
                // Location
                if (element.showLocation && item.location.isNotEmpty()) {
                    val locationLayout = createSimpleLayout(item.location, locationPaint, baseWidth)
                    itemHeight += locationLayout.height.toFloat()
                    partCount++
                }
            }
            WorkExperienceDisplayStyle.COMPACT -> {
                // Title + Company on same line
                val titleCompany = buildString {
                    if (item.jobTitle.isNotEmpty()) append(item.jobTitle)
                    if (item.jobTitle.isNotEmpty() && item.company.isNotEmpty()) append(" at ")
                    if (item.company.isNotEmpty()) append(item.company)
                }
                
                if (titleCompany.isNotEmpty()) {
                    val dateText = if (element.showDates) formatDateRange(item, element) else ""
                    val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
                    val availableTitleWidth = if (dateText.isNotEmpty()) baseWidth - dateWidth - itemSpacing else baseWidth
                    
                    val titleLayout = createSimpleLayout(titleCompany, titlePaint, availableTitleWidth)
                    itemHeight += titleLayout.height.toFloat()
                    partCount++
                }
                
                // Location
                if (element.showLocation && item.location.isNotEmpty()) {
                    val locationLayout = createSimpleLayout(item.location, locationPaint, baseWidth)
                    itemHeight += locationLayout.height.toFloat()
                    partCount++
                }
            }
            WorkExperienceDisplayStyle.DETAILED -> {
                // Job Title
                if (item.jobTitle.isNotEmpty()) {
                    val layout = createSimpleLayout(item.jobTitle, titlePaint, baseWidth)
                    itemHeight += layout.height.toFloat()
                    partCount++
                }
                
                // Company
                if (item.company.isNotEmpty()) {
                    val companyLayout = createSimpleLayout(item.company, companyPaint, baseWidth)
                    itemHeight += companyLayout.height.toFloat()
                    partCount++
                }
                
                // Location (and date if shown)
                if (element.showLocation && item.location.isNotEmpty()) {
                    val dateText = if (element.showDates) formatDateRange(item, element) else ""
                    val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
                    val availableLocationWidth = if (dateText.isNotEmpty()) baseWidth - dateWidth - itemSpacing else baseWidth
                    
                    val locationLayout = createSimpleLayout(item.location, locationPaint, availableLocationWidth)
                    itemHeight += locationLayout.height.toFloat()
                    partCount++
                } else if (element.showDates) {
                    val dateMetrics = datePaint.fontMetrics
                    itemHeight += dateMetrics.descent - dateMetrics.ascent
                    partCount++
                }
            }
        }
        
        // Add responsibilities height
        if (item.responsibilities.isNotEmpty()) {
            item.responsibilities.forEachIndexed { respIndex, responsibility ->
                if (responsibility.text.isNotEmpty()) {
                    val bullet = getBulletCharacter(element.bulletStyle, respIndex, responsibility)
                    val bulletWidth = responsibilityPaint.measureText("$bullet ")
                    val textWidth = baseWidth - bulletWidth
                    
                    val textLayout = createSimpleLayout(responsibility.text, responsibilityPaint, textWidth)
                    itemHeight += textLayout.height.toFloat()
                    
                    // Add spacing between responsibilities (not after last)
                    if (respIndex < item.responsibilities.size - 1) {
                        itemHeight += responsibilitySpacing
                    }
                }
            }
            
            // Add spacing before responsibilities if there's content above
            if (partCount > 0) {
                itemHeight += itemSpacing
            }
        }
        
        totalHeight += itemHeight
        
        // Add spacing between items (but not after last item)
        if (index < element.items.size - 1) {
            totalHeight += spacing
        }
    }
    
    // Add padding and apply zoom
    val finalHeight = (totalHeight + (basePadding * 2)) * zoomLevel
    
    return finalHeight
}

/**
 * Create text paint for work experience fields
 */
private fun createWorkExperienceTextPaint(
    textStyle: TextStyle,
    density: Float,
    context: android.content.Context
): TextPaint {
    return TextPaint().apply {
        isAntiAlias = true
        textSize = textStyle.fontSize * density
        color = textStyle.color.toInt()
        
        typeface = FontManager.getPoppinsTypeface(context, textStyle)
        
        isUnderlineText = textStyle.isUnderlined
        letterSpacing = textStyle.letterSpacing
    }
}

/**
 * Create simple text layout for measurements
 */
private fun createSimpleLayout(
    text: String,
    paint: TextPaint,
    width: Float
): StaticLayout {
    return StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1f)
        .setIncludePad(false)
        .build()
}

/**
 * Get bullet character based on bullet style
 */
private fun getBulletCharacter(
    bulletStyle: BulletStyle,
    index: Int,
    responsibility: ResponsibilityItem
): String {
    return when (bulletStyle) {
        BulletStyle.DISC -> "•"
        BulletStyle.DASH -> "-"
        BulletStyle.ARROW -> "→"
        BulletStyle.CHEVRON -> "›"
        BulletStyle.NUMBERED -> "${index + 1}."
        BulletStyle.CUSTOM_ICON -> responsibility.customBullet ?: "•"
        BulletStyle.NONE -> ""
    }
}

/**
 * Format date range for display
 */
private fun formatDateRange(
    item: WorkExperienceItem,
    element: ResumeElement.WorkExperienceElement
): String {
    val start = formatDate(item.startDate, element.dateFormat)
    val end = if (item.isCurrentRole) "Present" else formatDate(item.endDate, element.dateFormat)
    
    return when {
        start.isNotEmpty() && end.isNotEmpty() -> "$start${element.dateSeparator}$end"
        start.isNotEmpty() -> start
        end.isNotEmpty() -> end
        else -> ""
    }
}

/**
 * Format a single date string based on date format
 */
private fun formatDate(dateString: String, dateFormat: DateFormat): String {
    if (dateString.isEmpty()) return ""
    
    return try {
        val date = java.time.LocalDate.parse(dateString)
        
        when (dateFormat) {
            DateFormat.MMM_YYYY -> date.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"))
            DateFormat.MM_YYYY -> date.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"))
            DateFormat.FULL -> date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
            DateFormat.SHORT -> date.format(java.time.format.DateTimeFormatter.ofPattern("M/yy"))
            DateFormat.YYYY -> date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy"))
        }
    } catch (e: Exception) {
        dateString // Return original if parsing fails
    }
}

/**
 * Calculate project content height using StaticLayout (matching PDF renderer logic)
 */
private fun calculateProjectContentHeight(
    element: ResumeElement.ProjectElement,
    width: Float,
    density: Float,
    zoomLevel: Float,
    context: android.content.Context
): Float {
    if (element.items.isEmpty()) {
        return 16f * density * zoomLevel // Minimum height for empty content
    }

    // Calculate base dimensions (unscaled)
    val basePadding = 8f * density // 8dp in pixels
    val baseWidth = (width / zoomLevel) - (basePadding * 2)
    
    // Create text paints for different styles (no zoom applied)
    val namePaint = createWorkExperienceTextPaint(element.nameStyle, density, context)
    val descriptionPaint = createWorkExperienceTextPaint(element.descriptionStyle, density, context)
    val datePaint = createWorkExperienceTextPaint(element.dateStyle, density, context)
    val technologyPaint = createWorkExperienceTextPaint(element.technologyStyle, density, context)
    val highlightPaint = createWorkExperienceTextPaint(element.highlightStyle, density, context)
    val linkPaint = createWorkExperienceTextPaint(element.linkStyle, density, context)
    
    // Calculate spacing values
    val spacing = element.spacing * density
    val itemSpacing = element.itemSpacing * density
    val highlightSpacing = element.highlightSpacing * density
    
    var totalHeight = 0f
    
    // Calculate height for each project item
    element.items.forEachIndexed { index, item ->
        var itemHeight = 0f
        var partCount = 0
        
        when (element.displayStyle) {
            ProjectDisplayStyle.STANDARD -> {
                // Project Name and Date on same row
                if (item.name.isNotEmpty()) {
                    val layout = createSimpleLayout(item.name, namePaint, baseWidth)
                    itemHeight += layout.height.toFloat()
                    partCount++
                }
                
                // Technologies (as tags with padding)
                if (element.showTechnologies && item.technologies.isNotEmpty()) {
                    val technologies = item.technologies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (technologies.isNotEmpty()) {
                        val tagPadding = 8f * density
                        val verticalPadding = 6f * density
                        val fontMetrics = technologyPaint.fontMetrics
                        val textActualHeight = fontMetrics.descent - fontMetrics.ascent
                        val tagHeight = textActualHeight + verticalPadding * 2
                        itemHeight += tagHeight
                        partCount++
                    }
                }
                
                // Description
                if (element.showDescription && item.description.isNotEmpty()) {
                    val descLayout = createSimpleLayout(item.description, descriptionPaint, baseWidth)
                    itemHeight += descLayout.height.toFloat()
                    partCount++
                }
                
                // Link
                if (element.showLink && item.link.isNotEmpty()) {
                    val linkLayout = createSimpleLayout(item.link, linkPaint, baseWidth)
                    itemHeight += linkLayout.height.toFloat()
                    partCount++
                }
                
                // Add spacing between parts
                if (partCount > 1) {
                    itemHeight += itemSpacing * (partCount - 1)
                }
            }
            ProjectDisplayStyle.COMPACT -> {
                // Name + Date on same line
                val nameAndDate = buildString {
                    if (item.name.isNotEmpty()) append(item.name)
                }
                
                if (nameAndDate.isNotEmpty()) {
                    val dateText = if (element.showDates) formatProjectDateRange(item) else ""
                    val dateWidth = if (dateText.isNotEmpty()) datePaint.measureText(dateText) else 0f
                    val availableNameWidth = if (dateText.isNotEmpty()) baseWidth - dateWidth - itemSpacing else baseWidth
                    
                    val nameLayout = createSimpleLayout(nameAndDate, namePaint, availableNameWidth)
                    itemHeight += nameLayout.height.toFloat()
                    partCount++
                }
                
                // Technologies (as tags with padding)
                if (element.showTechnologies && item.technologies.isNotEmpty()) {
                    val technologies = item.technologies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (technologies.isNotEmpty()) {
                        val tagPadding = 8f * density
                        val verticalPadding = 6f * density
                        val fontMetrics = technologyPaint.fontMetrics
                        val textActualHeight = fontMetrics.descent - fontMetrics.ascent
                        val tagHeight = textActualHeight + verticalPadding * 2
                        itemHeight += tagHeight
                        partCount++
                    }
                }
                
                // Description
                if (element.showDescription && item.description.isNotEmpty()) {
                    val descLayout = createSimpleLayout(item.description, descriptionPaint, baseWidth)
                    itemHeight += descLayout.height.toFloat()
                    partCount++
                }
                
                // Link
                if (element.showLink && item.link.isNotEmpty()) {
                    val linkLayout = createSimpleLayout(item.link, linkPaint, baseWidth)
                    itemHeight += linkLayout.height.toFloat()
                    partCount++
                }
                
                // Add spacing between parts
                if (partCount > 1) {
                    itemHeight += itemSpacing * (partCount - 1)
                }
            }
            ProjectDisplayStyle.DETAILED -> {
                // Project Name
                if (item.name.isNotEmpty()) {
                    val layout = createSimpleLayout(item.name, namePaint, baseWidth)
                    itemHeight += layout.height.toFloat()
                    partCount++
                }
                
                // Date
                if (element.showDates) {
                    val dateText = formatProjectDateRange(item)
                    if (dateText.isNotEmpty()) {
                        val dateMetrics = datePaint.fontMetrics
                        itemHeight += dateMetrics.descent - dateMetrics.ascent
                        partCount++
                    }
                }
                
                // Technologies (as tags with padding)
                if (element.showTechnologies && item.technologies.isNotEmpty()) {
                    val technologies = item.technologies.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (technologies.isNotEmpty()) {
                        val tagPadding = 8f * density
                        val verticalPadding = 6f * density
                        val fontMetrics = technologyPaint.fontMetrics
                        val textActualHeight = fontMetrics.descent - fontMetrics.ascent
                        val tagHeight = textActualHeight + verticalPadding * 2
                        itemHeight += tagHeight
                        partCount++
                    }
                }
                
                // Link
                if (element.showLink && item.link.isNotEmpty()) {
                    val linkLayout = createSimpleLayout(item.link, linkPaint, baseWidth)
                    itemHeight += linkLayout.height.toFloat()
                    partCount++
                }
                
                // Description
                if (element.showDescription && item.description.isNotEmpty()) {
                    val descLayout = createSimpleLayout(item.description, descriptionPaint, baseWidth)
                    itemHeight += descLayout.height.toFloat()
                    partCount++
                }
                
                // Add spacing between parts
                if (partCount > 1) {
                    itemHeight += itemSpacing * (partCount - 1)
                }
            }
        }
        
        // Add highlights height
        if (item.highlights.isNotEmpty()) {
            item.highlights.forEachIndexed { highlightIndex, highlight ->
                if (highlight.text.isNotEmpty()) {
                    val bullet = getBulletCharacter(element.bulletStyle, highlightIndex, highlight)
                    val bulletWidth = highlightPaint.measureText("$bullet ")
                    val textWidth = baseWidth - bulletWidth
                    
                    val textLayout = createSimpleLayout(highlight.text, highlightPaint, textWidth)
                    itemHeight += textLayout.height.toFloat()
                    
                    // Add spacing between highlights (not after last)
                    if (highlightIndex < item.highlights.size - 1) {
                        itemHeight += highlightSpacing
                    }
                }
            }
            
            // Add spacing before highlights if there's content above
            if (partCount > 0) {
                itemHeight += itemSpacing
            }
        }
        
        totalHeight += itemHeight
        
        // Add spacing between items (but not after last item)
        if (index < element.items.size - 1) {
            totalHeight += spacing
        }
    }
    
    // Add padding and apply zoom
    val finalHeight = (totalHeight + (basePadding * 2)) * zoomLevel
    
    return finalHeight
}

/**
 * Get bullet character for project highlights
 */
private fun getBulletCharacter(
    bulletStyle: BulletStyle,
    index: Int,
    highlight: ProjectHighlight
): String {
    return when (bulletStyle) {
        BulletStyle.DISC -> "•"
        BulletStyle.DASH -> "-"
        BulletStyle.ARROW -> "→"
        BulletStyle.CHEVRON -> "›"
        BulletStyle.NUMBERED -> "${index + 1}."
        BulletStyle.CUSTOM_ICON -> highlight.customBullet ?: "•"
        BulletStyle.NONE -> ""
    }
}

/**
 * Format project date range for display
 */
private fun formatProjectDateRange(item: ProjectItem): String {
    val start = if (item.startDate.isNotEmpty()) item.startDate else ""
    val end = if (item.isOngoing) "Ongoing" else item.endDate
    
    return when {
        start.isNotEmpty() && end.isNotEmpty() -> "$start - $end"
        start.isNotEmpty() -> start
        end.isNotEmpty() -> end
        else -> ""
    }
}
