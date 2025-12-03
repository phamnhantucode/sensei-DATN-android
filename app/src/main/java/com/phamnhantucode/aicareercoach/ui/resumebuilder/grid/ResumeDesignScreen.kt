package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phamnhantucode.aicareercoach.data.local.GridResumeEntity
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Resume Design Screen
 * Shows templates and saved designs, allows user to create new or edit existing designs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeDesignScreen(
    onBack: () -> Unit,
    onNavigateToGridEditor: (designId: String?, templateAssetPath: String?, isNewDesign: Boolean, linkedResumeId: String?) -> Unit
) {
    val context = LocalContext.current
    val viewModel: ResumeDesignViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        ResumeDesignViewModel(context)
    }

    val designs by viewModel.designs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val isLoadingTemplates by viewModel.isLoadingTemplates.collectAsState()

    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resume Designs") },
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
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
            // Error message
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Templates Section
            TemplatesSection(
                templates = templates,
                isLoading = isLoadingTemplates,
                onTemplateClick = { template ->
                    onNavigateToGridEditor(null, template.assetPath, false, null)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // My Designs Section
            MyDesignsSection(
                designs = designs,
                isLoading = isLoading,
                onDesignClick = { design ->
                    onNavigateToGridEditor(design.id, null, false, design.id)
                },
                onDeleteClick = { designId ->
                    showDeleteConfirmation = designId
                }
            )
            }

            // Floating Action Button for New Design
            ExtendedFloatingActionButton(
                onClick = { onNavigateToGridEditor(null, null, true, null) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Design"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Design")
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteConfirmation?.let { designId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Design") },
            text = { Text("Are you sure you want to delete this design? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDesign(designId)
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Represents different variants of resume cards with their specific data and actions
 */
private sealed class ResumeCardVariant {
    data class Template(
        val template: ResumeTemplate,
        val onTemplateClick: (ResumeTemplate) -> Unit
    ) : ResumeCardVariant()

    data class Design(
        val design: GridResumeEntity,
        val onDesignClick: (GridResumeEntity) -> Unit,
        val onDeleteClick: (String) -> Unit
    ) : ResumeCardVariant()
}

/**
 * Unified resume card component supporting both templates and designs
 * Uses A4 aspect ratio (0.707) for accurate resume representation
 */
@Composable
private fun ResumeCard(
    variant: ResumeCardVariant,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .aspectRatio(0.707f) // A4 aspect ratio (width/height)
            .clickable {
                when (variant) {
                    is ResumeCardVariant.Template -> variant.onTemplateClick(variant.template)
                    is ResumeCardVariant.Design -> variant.onDesignClick(variant.design)
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Thumbnail section
                ResumeCardThumbnail(variant = variant)

                Spacer(modifier = Modifier.height(8.dp))

                // Info section
                ResumeCardInfo(variant = variant)
            }

            // Actions overlay (delete button for designs)
            ResumeCardActions(variant = variant)
        }
    }
}

/**
 * Thumbnail section with unified empty state handling
 */
@Composable
private fun ResumeCardThumbnail(variant: ResumeCardVariant) {
    val thumbnail = when (variant) {
        is ResumeCardVariant.Template -> variant.template.thumbnail
        is ResumeCardVariant.Design -> variant.design.thumbnail
    }

    val contentDescription = when (variant) {
        is ResumeCardVariant.Template -> "Template preview: ${variant.template.name}"
        is ResumeCardVariant.Design -> "Resume preview: ${variant.design.name}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.707f) // A4 aspect ratio for thumbnail
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail.isNotEmpty()) {
            val thumbnailBitmap = remember(thumbnail) {
                decodeBase64Thumbnail(thumbnail)
            }

            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Fallback if decode fails
                ThumbnailPlaceholder()
            }
        } else {
            // Empty state fallback
            ThumbnailPlaceholder()
        }
    }
}

/**
 * Unified placeholder for missing or failed thumbnails
 */
@Composable
private fun ThumbnailPlaceholder() {
    Icon(
        imageVector = Icons.Default.Description,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(40.dp)
    )
}

/**
 * Info section showing name and description/date
 */
@Composable
private fun ResumeCardInfo(variant: ResumeCardVariant) {
    Column {
        // Name/Title
        Text(
            text = when (variant) {
                is ResumeCardVariant.Template -> variant.template.name
                is ResumeCardVariant.Design -> variant.design.name
            },
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle (description for templates, date for designs)
        Text(
            text = when (variant) {
                is ResumeCardVariant.Template -> variant.template.description
                is ResumeCardVariant.Design -> formatDate(variant.design.updatedAt)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp
        )
    }
}

/**
 * Actions overlay (currently only delete button for designs)
 */
@Composable
private fun ResumeCardActions(variant: ResumeCardVariant) {
    if (variant is ResumeCardVariant.Design) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(
                onClick = { variant.onDeleteClick(variant.design.id) },
                modifier = Modifier
                    .padding(4.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete design",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Templates Section Component
 */
@Composable
private fun TemplatesSection(
    templates: List<ResumeTemplate>,
    isLoading: Boolean,
    onTemplateClick: (ResumeTemplate) -> Unit
) {
    Column {
        Text(
            text = "Templates",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "Choose a template to start with",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            templates.isEmpty() -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No templates available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(templates, key = { it.id }) { template ->
                        ResumeCard(
                            variant = ResumeCardVariant.Template(
                                template = template,
                                onTemplateClick = onTemplateClick
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * My Designs Section Component
 */
@Composable
private fun MyDesignsSection(
    designs: List<GridResumeEntity>,
    isLoading: Boolean,
    onDesignClick: (GridResumeEntity) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Column {
        Text(
            text = "My Designs",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "Your saved resume designs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            designs.isEmpty() -> {
                EmptyDesignsPlaceholder()
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(designs, key = { it.id }) { design ->
                        ResumeCard(
                            variant = ResumeCardVariant.Design(
                                design = design,
                                onDesignClick = onDesignClick,
                                onDeleteClick = onDeleteClick
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty Designs Placeholder
 */
@Composable
private fun EmptyDesignsPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No saved designs yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create your first design using a template or start from scratch",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Helper function to format date
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Helper function to decode Base64 thumbnail to Bitmap
 */
private fun decodeBase64Thumbnail(base64String: String): android.graphics.Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64String, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

/**
 * Resume Design Content - for embedding in tabs
 * Shows current design (if exists) and template selection
 * One form has only 1 design - user can change design by selecting a template
 */
@Composable
fun ResumeDesignContent(
    resumeId: String,
    onNavigateToGridEditor: (designId: String?, templateAssetPath: String?, isNewDesign: Boolean, linkedResumeId: String?) -> Unit
) {
    val context = LocalContext.current
    val viewModel: ResumeDesignViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        ResumeDesignViewModel(context)
    }

    // Refresh designs when this composable becomes visible (e.g., navigating back from editor)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadDesigns()
    }

    val designs by viewModel.designs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val isLoadingTemplates by viewModel.isLoadingTemplates.collectAsState()

    // Get the design for this specific resume (matched by ID)
    val currentDesign = designs.find { it.id == resumeId }
    val hasDesign = currentDesign != null

    var showChangeTemplateConfirmation by remember { mutableStateOf<ResumeTemplate?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Error message
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Current Design Section (if exists)
            if (hasDesign) {
                CurrentDesignSection(
                    design = currentDesign!!,
                    isLoading = isLoading,
                    onEditClick = {
                        onNavigateToGridEditor(currentDesign.id, null, false, resumeId)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Templates Section - for selecting/changing template
            TemplateSelectorSection(
                templates = templates,
                isLoading = isLoadingTemplates,
                hasExistingDesign = hasDesign,
                onTemplateClick = { template ->
                    if (hasDesign) {
                        // Show confirmation when changing template
                        showChangeTemplateConfirmation = template
                    } else {
                        // No existing design, directly create with template - pass resumeId to link design
                        onNavigateToGridEditor(null, template.assetPath, false, resumeId)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Change Template Confirmation Dialog
    showChangeTemplateConfirmation?.let { template ->
        AlertDialog(
            onDismissRequest = { showChangeTemplateConfirmation = null },
            title = { Text("Change Template") },
            text = { Text("This will replace your current design with the selected template. Your form data will be preserved but the layout will change. Continue?") },
            confirmButton = {
                Button(
                    onClick = {
                        // Delete current design and create new one with template
                        currentDesign?.let { viewModel.deleteDesign(it.id) }
                        onNavigateToGridEditor(null, template.assetPath, false, resumeId)
                        showChangeTemplateConfirmation = null
                    }
                ) {
                    Text("Change Template")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeTemplateConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Current Design Section - shows the user's current design
 */
@Composable
private fun CurrentDesignSection(
    design: GridResumeEntity,
    isLoading: Boolean,
    onEditClick: () -> Unit
) {
    Column {
        Text(
            text = "Your Design",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "Click to edit your resume design",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Large clickable card for current design
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f) // A4 aspect ratio
                    .clickable { onEditClick() },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Thumbnail
                    if (design.thumbnail.isNotEmpty()) {
                        val thumbnailBitmap = remember(design.thumbnail) {
                            decodeBase64Thumbnail(design.thumbnail)
                        }
                        if (thumbnailBitmap != null) {
                            Image(
                                bitmap = thumbnailBitmap.asImageBitmap(),
                                contentDescription = "Current design preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            DesignPlaceholder()
                        }
                    } else {
                        DesignPlaceholder()
                    }

                    // Edit overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to Edit",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Design info
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = design.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = "Last updated: ${formatDate(design.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Design Placeholder for empty/failed thumbnails
 */
@Composable
private fun DesignPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
    }
}

/**
 * Template Selector Section - for selecting or changing templates
 */
@Composable
private fun TemplateSelectorSection(
    templates: List<ResumeTemplate>,
    isLoading: Boolean,
    hasExistingDesign: Boolean,
    onTemplateClick: (ResumeTemplate) -> Unit
) {
    Column {
        Text(
            text = if (hasExistingDesign) "Change Template" else "Select a Template",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = if (hasExistingDesign) 
                "Select a different template to change your design layout" 
            else 
                "Choose a template to start designing your resume",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            templates.isEmpty() -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No templates available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(templates, key = { it.id }) { template ->
                        ResumeCard(
                            variant = ResumeCardVariant.Template(
                                template = template,
                                onTemplateClick = onTemplateClick
                            )
                        )
                    }
                }
            }
        }
    }
}
