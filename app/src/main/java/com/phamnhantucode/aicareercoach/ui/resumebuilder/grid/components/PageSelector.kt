package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumePage

/**
 * Page selector component for multi-page resume editing
 * 
 * Displays page thumbnails in a horizontal scrollable list
 * Allows navigation between pages, adding new pages, and removing pages
 */
@Composable
fun PageSelector(
    pages: List<ResumePage>,
    currentPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: ((Int) -> Unit)? = null,
    onDuplicatePage: ((Int) -> Unit)? = null,
    pageThumbnails: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier,
    showRemoveButton: Boolean = true,
    showDuplicateButton: Boolean = true,
    compactMode: Boolean = false
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to current page when it changes
    LaunchedEffect(currentPageIndex) {
        listState.animateScrollToItem(currentPageIndex)
    }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(pages) { index, page ->
                PageThumbnail(
                    page = page,
                    pageNumber = index + 1,
                    thumbnailBase64 = pageThumbnails[page.id],
                    isSelected = index == currentPageIndex,
                    canRemove = pages.size > 1 && showRemoveButton,
                    canDuplicate = showDuplicateButton,
                    compactMode = compactMode,
                    onClick = { onPageSelected(index) },
                    onRemove = onRemovePage?.let { { it(index) } },
                    onDuplicate = onDuplicatePage?.let { { it(index) } }
                )
            }
            
            item {
                AddPageButton(
                    onClick = onAddPage,
                    compactMode = compactMode
                )
            }
        }
    }
}

/**
 * Individual page thumbnail in the selector
 */
@Composable
private fun PageThumbnail(
    page: ResumePage,
    pageNumber: Int,
    thumbnailBase64: String?,
    isSelected: Boolean,
    canRemove: Boolean,
    canDuplicate: Boolean,
    compactMode: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
    onDuplicate: (() -> Unit)?
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        label = "borderColor"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        label = "borderWidth"
    )
    
    val thumbnailWidth = if (compactMode) 40.dp else 56.dp
    val thumbnailHeight = if (compactMode) 56.dp else 80.dp
    
    Box(
        modifier = Modifier
            .width(thumbnailWidth)
            .height(thumbnailHeight)
    ) {
        // Page thumbnail
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
                .clickable { onClick() },
            color = Color(page.backgroundColor.toInt()),
            shadowElevation = if (isSelected) 4.dp else 1.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                // Display thumbnail or loading state
                if (thumbnailBase64 != null && thumbnailBase64.isNotEmpty()) {
                    // Check if thumbnail is a URL or Base64
                    val isUrl = thumbnailBase64.startsWith("http://") || thumbnailBase64.startsWith("https://")
                    
                    if (isUrl) {
                        // Load image from URL using Coil
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(thumbnailBase64)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Page $pageNumber preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Decode Base64 thumbnail (legacy format)
                        val thumbnailBitmap = remember(thumbnailBase64) {
                            decodeBase64Thumbnail(thumbnailBase64)
                        }

                        if (thumbnailBitmap != null) {
                            Image(
                                bitmap = thumbnailBitmap.asImageBitmap(),
                                contentDescription = "Page $pageNumber preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            PageNumberFallback(pageNumber, isSelected, compactMode)
                        }
                    }
                } else {
                    ThumbnailLoadingPlaceholder(page, pageNumber, isSelected, compactMode)
                }

                // Keep overflow indicator on top of thumbnail
                if (page.isOverflowPage) {
                    Text(
                        text = "↓",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            .padding(2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        
        // Context menu button (only when selected and can perform actions)
        if (isSelected && (canRemove || canDuplicate)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            CircleShape
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            CircleShape
                        )
                        .clickable { showContextMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Page options",
                        modifier = Modifier.size(8.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    if (canDuplicate && onDuplicate != null) {
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            onClick = {
                                showContextMenu = false
                                onDuplicate()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                    if (canRemove && onRemove != null) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showContextMenu = false
                                onRemove()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Add page button
 */
@Composable
private fun AddPageButton(
    onClick: () -> Unit,
    compactMode: Boolean
) {
    val width = if (compactMode) 40.dp else 56.dp
    val height = if (compactMode) 56.dp else 80.dp
    
    Surface(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add page",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (compactMode) 20.dp else 24.dp)
            )
        }
    }
}

/**
 * Page indicator (compact version showing just "Page X of Y")
 */
@Composable
fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousPage,
            enabled = currentPage > 0,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Previous page",
                modifier = Modifier.size(18.dp)
            )
        }
        
        Text(
            text = "${currentPage + 1} / $totalPages",
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 48.dp)
        )
        
        IconButton(
            onClick = onNextPage,
            enabled = currentPage < totalPages - 1,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Next page",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Loading placeholder while thumbnail is being generated
 */
@Composable
private fun ThumbnailLoadingPlaceholder(
    page: ResumePage,
    pageNumber: Int,
    isSelected: Boolean,
    compactMode: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(page.backgroundColor.toInt())),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!compactMode) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = pageNumber.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Fallback page number display when thumbnail fails to load
 */
@Composable
private fun PageNumberFallback(pageNumber: Int, isSelected: Boolean, compactMode: Boolean) {
    Text(
        text = pageNumber.toString(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (compactMode) 12.sp else 14.sp
        ),
        color = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}

/**
 * Decode Base64 string to Bitmap
 */
private fun decodeBase64Thumbnail(base64String: String): android.graphics.Bitmap? {
    return try {
        val decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.NO_WRAP)
        android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        android.util.Log.e("PageSelector", "Failed to decode thumbnail", e)
        null
    }
}
