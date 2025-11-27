package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.pagination.OverflowInfo
import kotlin.math.roundToInt

/**
 * Visual indicator for content overflow
 * 
 * Shows when content exceeds page boundaries with option to auto-split
 */
@Composable
fun OverflowIndicator(
    overflowInfo: OverflowInfo?,
    onAutoSplit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = overflowInfo != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        overflowInfo?.let { info ->
            OverflowCard(
                overflowInfo = info,
                onAutoSplit = onAutoSplit,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun OverflowCard(
    overflowInfo: OverflowInfo,
    onAutoSplit: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = pulseAlpha),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Warning icon
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Content overflows page",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "${overflowInfo.overflowAmountDp.roundToInt()}dp overflow • ${overflowInfo.overflowingElementIds.size} element(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
            
            // Auto-split button
            FilledTonalButton(
                onClick = onAutoSplit,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Auto-split",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/**
 * Page boundary line indicator
 * 
 * Shows a visual line at the page boundary in the editor canvas
 */
@Composable
fun PageBoundaryLine(
    isOverflowing: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isOverflowing) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(300),
        label = "boundaryColor"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        animatedColor,
                        animatedColor,
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * Overflow badge for elements that exceed page bounds
 */
@Composable
fun OverflowBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.error,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "Overflow",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onError
            )
        }
    }
}

/**
 * Continuation badge for elements that continue from previous page
 */
@Composable
fun ContinuationBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondary,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Continued",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

/**
 * Compact overflow notification toast
 */
@Composable
fun OverflowToast(
    visible: Boolean,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    }
    
    // Auto-dismiss after delay
    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(3000)
            onDismiss()
        }
    }
}
