package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.GridUtils
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*

@Composable
fun ContainerElementRenderer(
    element: ResumeElement.ContainerElement,
    children: List<ResumeElement>,
    gridConfig: GridConfig,
    zoomLevel: Float,
    renderChild: @Composable (ResumeElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val cellSizePx = gridConfig.cellSizeDp * density * zoomLevel
    
    val backgroundColor = element.style.backgroundColor?.let { Color(it) } ?: Color.Transparent
    val borderColor = element.style.borderColor?.let { Color(it) }
    val borderWidth = element.style.borderWidth.dp
    val cornerRadius = element.style.borderRadius.dp
    
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (element.style.shadowBlur > 0) {
                    Modifier // Shadow handling would go here
                } else {
                    Modifier
                }
            )
            .background(backgroundColor, shape)
            .then(
                if (borderColor != null && element.style.borderWidth > 0) {
                    Modifier.border(borderWidth, borderColor, shape)
                } else {
                    Modifier
                }
            )
            .then(
                if (element.clipContent) {
                    Modifier.clip(shape)
                } else {
                    Modifier
                }
            )
            .padding(
                start = element.padding.left.dp,
                top = element.padding.top.dp,
                end = element.padding.right.dp,
                bottom = element.padding.bottom.dp
            )
    ) {
        // Render children using absolute positioning (relative to container top-left)
        // Note: ContainerElement currently only supports absolute positioning
        // Layout modes (AUTO_HORIZONTAL, AUTO_VERTICAL, etc.) can be added later
        children.forEach { child ->
            // Position the child manually using Box + offset
            // The child's position (row/col) is relative to the container
            val childX = child.position.col * cellSizePx
            val childY = child.position.row * cellSizePx
            
            // Calculate child size
            val childWidth = child.position.colSpan * cellSizePx
            val childHeight = child.position.rowSpan * cellSizePx
            
            Box(
                modifier = Modifier
                    .offset(
                        x = GridUtils.pxToDp(childX, density),
                        y = GridUtils.pxToDp(childY, density)
                    )
                    .size(
                        width = GridUtils.pxToDp(childWidth, density),
                        height = GridUtils.pxToDp(childHeight, density)
                    )
            ) {
                renderChild(child)
            }
        }
    }
}
