package com.phamnhantucode.aicareercoach.ui.resumebuilder

import androidx.compose.animation.*
import com.phamnhantucode.aicareercoach.ui.components.ShimmerBox
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Resume List Screen
 * Shows all user's resumes and allows creating new ones or editing existing ones
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeListScreen(
    onBack: () -> Unit,
    onNavigateToResumeBuilder: (resumeId: String?) -> Unit
) {
    val context = LocalContext.current
    val viewModel: ResumeListViewModel = viewModel {
        ResumeListViewModel(context)
    }

    // Refresh data when screen becomes visible (e.g., navigating back)
    LaunchedEffect(Unit) {
        viewModel.loadResumes()
    }

    val resumes by viewModel.resumes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Track pending deletion for undo
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteName by remember { mutableStateOf<String>("") }

    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }

    // Handle deletion with undo snackbar
    fun handleDelete(resumeId: String, resumeName: String) {
        pendingDeleteId = resumeId
        pendingDeleteName = resumeName
        viewModel.deleteResume(resumeId) { success ->
            if (success) {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "\"$resumeName\" deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete()
                    } else {
                        viewModel.confirmDelete()
                    }
                    pendingDeleteId = null
                }
            } else {
                pendingDeleteId = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Resumes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToResumeBuilder(null) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new resume"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Resume")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    ResumeListLoading()
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadResumes() }) {
                            Text("Retry")
                        }
                    }
                }

                resumes.isEmpty() -> {
                    EmptyResumesPlaceholder(
                        onCreateNew = { onNavigateToResumeBuilder(null) }
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = resumes,
                            key = { it.resume.id }
                        ) { resumeWithThumbnail ->
                            SwipeableResumeListItem(
                                resume = resumeWithThumbnail.resume,
                                thumbnail = resumeWithThumbnail.thumbnail,
                                isDeleting = isDeleting && pendingDeleteId == resumeWithThumbnail.resume.id,
                                onClick = { onNavigateToResumeBuilder(resumeWithThumbnail.resume.id) },
                                onDelete = {
                                    val name = resumeWithThumbnail.resume.personalInfo.fullName.ifBlank { "Untitled Resume" }
                                    showDeleteConfirmation = resumeWithThumbnail.resume.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteConfirmation?.let { resumeId ->
        val resumeToDelete = resumes.find { it.resume.id == resumeId }?.resume
        val resumeName = resumeToDelete?.personalInfo?.fullName?.ifBlank { "Untitled Resume" } ?: "Untitled Resume"
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Resume") },
            text = {
                Text(
                    "Are you sure you want to delete \"$resumeName\"? This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        handleDelete(resumeId, resumeName)
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !isDeleting
                ) {
                    if (isDeleting && pendingDeleteId == resumeId) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = null },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Swipeable Resume List Item with swipe-to-delete gesture
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableResumeListItem(
    resume: Resume,
    thumbnail: String?,
    isDeleting: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
                false // Don't auto-dismiss, let the dialog confirm
            } else {
                false
            }
        },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                },
                label = "swipe_background"
            )
            val scale by animateFloatAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0.8f,
                label = "icon_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.scale(scale),
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        },
        content = {
            ResumeListItem(
                resume = resume,
                thumbnail = thumbnail,
                isDeleting = isDeleting,
                onClick = onClick,
                onDelete = onDelete
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}

@Composable
private fun ResumeListItem(
    resume: Resume,
    thumbnail: String?,
    isDeleting: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail section with delete button overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
            ) {
                // Thumbnail or fallback avatar
                if (thumbnail != null && thumbnail.isNotBlank()) {
                    // Check if thumbnail is a URL (from Cloudinary) or Base64
                    val isUrl = thumbnail.startsWith("http://") || thumbnail.startsWith("https://")

                    if (isUrl) {
                        // Load image from URL using Coil
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(thumbnail)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Resume preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Decode Base64 thumbnail (legacy format)
                        val imageBitmap = remember(thumbnail) {
                            try {
                                val bytes = android.util.Base64.decode(thumbnail, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    ?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Resume preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Fallback if thumbnail decode fails
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                FallbackAvatarContent(resume)
                            }
                        }
                    }
                } else {
                    // No thumbnail available, show initials avatar
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        FallbackAvatarContent(resume)
                    }
                }

                // Delete button overlay in top-right corner
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = CircleShape
                        ),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.error,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete resume",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Resume info section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = resume.personalInfo.fullName.ifBlank { "Untitled Resume" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Show email if available
                if (resume.personalInfo.email.isNotBlank()) {
                    Text(
                        text = resume.personalInfo.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Resume stats
                val statsText = buildList {
                    if (resume.workExperiences.isNotEmpty()) {
                        add("${resume.workExperiences.size} experience${if (resume.workExperiences.size > 1) "s" else ""}")
                    }
                    if (resume.skills.isNotEmpty()) {
                        add("${resume.skills.size} skill${if (resume.skills.size > 1) "s" else ""}")
                    }
                    if (resume.education.isNotEmpty()) {
                        add("${resume.education.size} education")
                    }
                }.joinToString(" · ")

                if (statsText.isNotEmpty()) {
                    Text(
                        text = statsText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumeListLoading() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(4) {
            ResumeLoadingCard(shimmerProgress = shimmerProgress)
        }
    }
}

@Composable
private fun ResumeLoadingCard(shimmerProgress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail placeholder
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f),
                shimmerProgress = shimmerProgress
            )

            // Resume info section placeholders
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title placeholder
                ShimmerBox(
                    modifier = Modifier
                        .width(200.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    shimmerProgress = shimmerProgress
                )

                // Email placeholder
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    shimmerProgress = shimmerProgress
                )

                // Stats placeholder
                ShimmerBox(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    shimmerProgress = shimmerProgress
                )
            }
        }
    }
}

@Composable
private fun EmptyResumesPlaceholder(
    onCreateNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No resumes yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create your first resume to get started with your job search",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCreateNew,
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Resume")
        }
    }
}

@Composable
private fun FallbackAvatarContent(resume: Resume) {
    val initials = resume.personalInfo.fullName
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    if (initials.isNotEmpty()) {
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    } else {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(48.dp)
        )
    }
}
