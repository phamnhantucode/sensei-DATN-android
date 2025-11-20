package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ImageScale
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement

/**
 * Renders an image element on the resume
 */
@Composable
fun ImageElementRenderer(
    element: ResumeElement.ImageElement,
    modifier: Modifier = Modifier
) {
    val backgroundColor = element.style.backgroundColor?.let { Color(it) }
    val borderColor = element.style.borderColor?.let { Color(it) }

    // Determine shape based on isCircle flag
    val shape: Shape = if (element.isCircle) {
        CircleShape
    } else {
        RoundedCornerShape(element.cornerRadius.dp.coerceAtLeast(element.style.borderRadius.dp))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                // For circles, ensure 1:1 aspect ratio
                if (element.isCircle) {
                    Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true)
                } else {
                    Modifier
                }
            )
            .then(
                if (backgroundColor != null) {
                    Modifier.background(
                        color = backgroundColor,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (borderColor != null && element.style.borderWidth > 0) {
                    Modifier.border(
                        width = element.style.borderWidth.dp,
                        color = borderColor,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (element.style.shadowBlur > 0) {
                    Modifier.shadow(
                        elevation = element.style.shadowBlur.dp,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
    ) {
        if (element.imageUrl.isNotEmpty()) {
            // Load image using Coil
            SubcomposeAsyncImage(
                model = element.imageUrl,
                contentDescription = element.description.ifEmpty { "Resume image" },
                contentScale = element.contentScale.toContentScale(),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
                loading = {
                    // Loading placeholder
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Loading",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                    }
                },
                error = {
                    // Error placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = "Failed to load",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Failed to load image",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            )
        } else {
            // Empty state placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "No image",
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add Image",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * Converts custom ImageScale to Compose ContentScale
 */
private fun ImageScale.toContentScale(): ContentScale {
    return when (this) {
        ImageScale.FIT -> ContentScale.Fit
        ImageScale.FILL -> ContentScale.Crop
        ImageScale.STRETCH -> ContentScale.FillBounds
    }
}
