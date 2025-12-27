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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ContentCopy
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
import com.phamnhantucode.aicareercoach.ui.components.CreditExhaustedDialog
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Screen for displaying and managing the user's resumes.
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

    val isParsing by viewModel.isParsing.collectAsState()
    val parsingStage by viewModel.parsingStage.collectAsState()
    val parsingError by viewModel.parsingError.collectAsState()
    val parsedResume by viewModel.parsedResume.collectAsState()
    

    val isEnhancing by viewModel.isEnhancing.collectAsState()
    val enhancementSuggestions by viewModel.enhancementSuggestions.collectAsState()
    val enhancementError by viewModel.enhancementError.collectAsState()
    var showEnhancementDialog by remember { mutableStateOf<String?>(null) }
    

    val pdfPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.parseResumeFromPdf(context, it)
        }
    }

    val showCreditDialog by viewModel.showCreditDialog.collectAsState()

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
                actions = {
                    // Upload PDF Button - more prominent
                    FilledTonalButton(
                        onClick = { 
                            pdfPickerLauncher.launch(arrayOf("application/pdf")) 
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import CV", style = MaterialTheme.typography.labelLarge)
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
                        onCreateNew = { onNavigateToResumeBuilder(null) },
                        onImportCV = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
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
                            // Resume List Item with updated Dropdown UI
                            ResumeListItem(
                                resume = resumeWithThumbnail.resume,
                                thumbnail = resumeWithThumbnail.thumbnail,
                                isDeleting = isDeleting && pendingDeleteId == resumeWithThumbnail.resume.id,
                                onClick = { onNavigateToResumeBuilder(resumeWithThumbnail.resume.id) },
                                onDelete = {
                                    val name = resumeWithThumbnail.resume.personalInfo.fullName.ifBlank { "Untitled Resume" }
                                    showDeleteConfirmation = resumeWithThumbnail.resume.id
                                },
                                onEnhance = {
                                    showEnhancementDialog = resumeWithThumbnail.resume.id
                                },
                                onDuplicate = {
                                    viewModel.duplicateResume(resumeWithThumbnail.resume.id)
                                }
                            )
                        }
                    }
                }
            }
            

            if (parsingStage != ResumeListViewModel.ParsingStage.IDLE) {
                CVImportDialog(
                    parsingStage = parsingStage,
                    parsingError = parsingError,
                    parsedResume = parsedResume,
                    onRetry = { viewModel.retryParsing(context) },
                    onDismiss = { viewModel.dismissParsingDialog() },
                    onViewResume = { resume ->
                        viewModel.dismissParsingDialog()
                        onNavigateToResumeBuilder(resume.id)
                    }
                )
            }
        }
    }


    showEnhancementDialog?.let { resumeId ->
        val resume = resumes.find { it.resume.id == resumeId }?.resume
        if (resume != null) {
            ResumeEnhancementDialog(
                resume = resume,
                isEnhancing = isEnhancing,
                suggestions = enhancementSuggestions,
                error = enhancementError,
                onDismiss = {
                    showEnhancementDialog = null
                    viewModel.clearEnhancementDialog()
                },
                onEnhance = { jobDescription ->
                    viewModel.enhanceResume(resumeId, jobDescription)
                },
                onApplySuggestion = { suggestionType ->
                    viewModel.applyEnhancement(suggestionType)
                }
            )
        }
    }


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

    if (showCreditDialog) {
        CreditExhaustedDialog(
            onDismiss = { viewModel.dismissCreditDialog() },
            onPurchase = { viewModel.openPurchaseScreen() }
        )
    }
}



@Composable
private fun ResumeListItem(
    resume: Resume,
    thumbnail: String?,
    isDeleting: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEnhance: () -> Unit,
    onDuplicate: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    // Track background brightness for icon visibility
    var isBackgroundDark by remember { mutableStateOf(false) }

    // Determine the icon color based on background darkness
    val iconTint = if (isBackgroundDark) Color.White else MaterialTheme.colorScheme.surface

    // Checks if the top part of the bitmap is dark
    fun checkBrightness(bitmap: android.graphics.Bitmap) {
        // We only care about the top part where icons are (approx top 40dp)
        // Let's sample the top 20% of the image to be safe
        try {
            val width = bitmap.width
            val height = (bitmap.height * 0.2).toInt().coerceAtLeast(1)
            
            var r = 0L
            var g = 0L
            var b = 0L
            var pixelCount = 0
            
            // Sample pixels with a stride to save performance
            val step = 10.coerceAtMost(width / 2)
            
            for (x in 0 until width step step) {
                for (y in 0 until height step step) {
                    val pixel = bitmap.getPixel(x, y)
                    r += android.graphics.Color.red(pixel)
                    g += android.graphics.Color.green(pixel)
                    b += android.graphics.Color.blue(pixel)
                    pixelCount++
                }
            }
            
            if (pixelCount > 0) {
                val avgR = r / pixelCount
                val avgG = g / pixelCount
                val avgB = b / pixelCount
                
                // Calculate luminance: 0.299R + 0.587G + 0.114B
                val luminance = 0.299 * avgR + 0.587 * avgG + 0.114 * avgB
                
                // If luminance < 128, it's dark
                isBackgroundDark = luminance < 128
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
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
            modifier = Modifier.fillMaxSize()
        ) {
            // Thumbnail section with menu button overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant) // Default bg
            ) {
                // Determine what to show and setup side-effects for brightness check
                if (thumbnail != null && thumbnail.isNotBlank()) {
                    val isUrl = thumbnail.startsWith("http://") || thumbnail.startsWith("https://")

                    if (isUrl) {
                        // Load image from URL using Coil
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(thumbnail)
                                .crossfade(true)
                                .allowHardware(false) // Important for reading pixels
                                .listener(
                                    onSuccess = { _, result ->
                                        val drawable = result.drawable
                                        if (drawable is android.graphics.drawable.BitmapDrawable) {
                                            checkBrightness(drawable.bitmap)
                                        }
                                    }
                                )
                                .build(),
                            contentDescription = "Resume preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Decode Base64
                        LaunchedEffect(thumbnail) {
                            try {
                                val bytes = android.util.Base64.decode(thumbnail, android.util.Base64.DEFAULT)
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bitmap != null) {
                                    checkBrightness(bitmap)
                                }
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                        
                        // We still need to display it
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
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Fallback inside the box
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                FallbackAvatarContent(resume)
                                // If fallback, background is primaryContainer (usually dark-ish or colored). 
                                // Let's stick to default onSurface or contentColorFor(primaryContainer)
                                // But here we are modifying isBackgroundDark.
                                // Let's check primaryContainer brightness?
                                // Usually it allows onPrimaryContainer.
                                SideEffect {
                                    // For simplicity, let's assume primaryContainer is "dark enough" to need light icons 
                                    // OR "light" to need dark icons.
                                    // Actually, standard Material3: onPrimaryContainer is the color to use.
                                    // But our logic uses isBackgroundDark -> White vs OnSurface.
                                    // Let's reset to false for consistency or use the colorscheme directly in logic?
                                    // easier: just set iconTint logic below to handle fallback case if needed.
                                    // For now, let's assume it's NOT dark background for standard visibility unless primaryContainer is very dark.
                                    isBackgroundDark = true
                                }
                            }
                        }
                    }
                } else {
                    // No thumbnail -> Fallback
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        FallbackAvatarContent(resume)
                        SideEffect {
                            isBackgroundDark = true
                        }
                    }
                }

                // Action buttons overlay in top corners
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(4.dp), // Reduced padding to move icons closer to edge if needed
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // AI Enhancement button in top-left
                    IconButton(
                        onClick = onEnhance,
                        enabled = !isDeleting,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = iconTint,
                            containerColor = Color.Black.copy(alpha = 0.25f) // Slight scrim for better visibility
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Enhance",
                            modifier = Modifier.size(20.dp) // Slightly larger
                        )
                    }
                    
                    // Menu button in top-right
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            enabled = !isDeleting,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = iconTint,
                                containerColor = Color.Black.copy(alpha = 0.1f) // Slight scrim
                            )
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = iconTint,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    modifier = Modifier.size(20.dp) 
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = "Duplicate",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onDuplicate()
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "Delete",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Resume info section - auto height
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Last Modified
                val lastModifiedText = remember(resume.lastModified) {
                    resume.lastModified?.let {
                        val now = java.time.LocalDateTime.now()
                        val diff = java.time.Duration.between(it, now)
                        when {
                            diff.toMinutes() < 1 -> "Just now"
                            diff.toHours() < 1 -> "${diff.toMinutes()}m ago"
                            diff.toHours() < 24 -> "${diff.toHours()}h ago"
                            diff.toDays() < 7 -> "${diff.toDays()}d ago"
                            else -> it.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                        }
                    } ?: "Recently"
                }

                Text(
                    text = "Edited $lastModifiedText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thumbnail placeholder
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shimmerProgress = shimmerProgress
            )

            // Resume info section placeholders - auto height
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

                // Last edited placeholder
                ShimmerBox(
                    modifier = Modifier
                        .width(100.dp)
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
    onCreateNew: () -> Unit,
    onImportCV: () -> Unit = {}
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
            text = "Create a new resume from scratch or import an existing CV",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Primary action - Create new
        Button(
            onClick = onCreateNew,
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Resume")
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Secondary action - Import CV
        OutlinedButton(
            onClick = onImportCV,
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Existing CV")
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

/**
 * Enhanced CV Import Dialog with progress stages, success preview, and error handling
 */
@Composable
private fun CVImportDialog(
    parsingStage: ResumeListViewModel.ParsingStage,
    parsingError: String?,
    parsedResume: Resume?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onViewResume: (Resume) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 300.dp, max = 400.dp)
                .padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            when (parsingStage) {
                ResumeListViewModel.ParsingStage.ERROR -> {
                    // Error state
                    CVImportErrorContent(
                        error = parsingError ?: "An unknown error occurred",
                        onRetry = onRetry,
                        onDismiss = onDismiss
                    )
                }
                ResumeListViewModel.ParsingStage.COMPLETED -> {
                    // Success state with preview
                    CVImportSuccessContent(
                        resume = parsedResume,
                        onViewResume = { parsedResume?.let { onViewResume(it) } },
                        onDismiss = onDismiss
                    )
                }
                else -> {
                    // Progress states
                    CVImportProgressContent(
                        stage = parsingStage
                    )
                }
            }
        }
    }
}

/**
 * Progress content showing parsing stages
 */
@Composable
private fun CVImportProgressContent(
    stage: ResumeListViewModel.ParsingStage
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "Importing Your CV",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Progress steps
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ParsingStepItem(
                stepNumber = 1,
                title = "Reading PDF file",
                isCompleted = stage.ordinal > ResumeListViewModel.ParsingStage.READING_PDF.ordinal,
                isActive = stage == ResumeListViewModel.ParsingStage.READING_PDF
            )
            ParsingStepItem(
                stepNumber = 2,
                title = "Extracting text content",
                isCompleted = stage.ordinal > ResumeListViewModel.ParsingStage.EXTRACTING_TEXT.ordinal,
                isActive = stage == ResumeListViewModel.ParsingStage.EXTRACTING_TEXT
            )
            ParsingStepItem(
                stepNumber = 3,
                title = "Analyzing with AI",
                isCompleted = stage.ordinal > ResumeListViewModel.ParsingStage.ANALYZING_WITH_AI.ordinal,
                isActive = stage == ResumeListViewModel.ParsingStage.ANALYZING_WITH_AI
            )
            ParsingStepItem(
                stepNumber = 4,
                title = "Saving resume",
                isCompleted = stage.ordinal > ResumeListViewModel.ParsingStage.SAVING_RESUME.ordinal,
                isActive = stage == ResumeListViewModel.ParsingStage.SAVING_RESUME
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "This may take a moment...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Individual parsing step item
 */
@Composable
private fun ParsingStepItem(
    stepNumber: Int,
    title: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Step indicator
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    when {
                        isCompleted -> MaterialTheme.colorScheme.primary
                        isActive -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            } else if (isActive) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isCompleted -> MaterialTheme.colorScheme.primary
                isActive -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Success content with parsed resume preview
 */
@Composable
private fun CVImportSuccessContent(
    resume: Resume?,
    onViewResume: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "CV Imported Successfully!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your resume has been created and is ready to edit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // Parsed data summary
        resume?.let { r ->
            Spacer(modifier = Modifier.height(20.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Name
                    if (r.personalInfo.fullName.isNotBlank()) {
                        Text(
                            text = r.personalInfo.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Email
                    if (r.personalInfo.email.isNotBlank()) {
                        Text(
                            text = r.personalInfo.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ImportedDataStat(
                            icon = Icons.Default.Work,
                            count = r.workExperiences.size,
                            label = "Experience"
                        )
                        ImportedDataStat(
                            icon = Icons.Default.School,
                            count = r.education.size,
                            label = "Education"
                        )
                        ImportedDataStat(
                            icon = Icons.Default.Build,
                            count = r.skills.size,
                            label = "Skills"
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Close")
            }
            Button(
                onClick = onViewResume,
                modifier = Modifier.weight(1f)
            ) {
                Text("Edit Resume")
            }
        }
    }
}

/**
 * Stat item for imported data summary
 */
@Composable
private fun ImportedDataStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Error content with retry option
 */
@Composable
private fun CVImportErrorContent(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Error icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    MaterialTheme.colorScheme.errorContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Import Failed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tips card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Tips for best results:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "• Use a text-based PDF (not scanned images)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Ensure the PDF is not password protected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Check your internet connection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Try Again")
            }
        }
    }
}
