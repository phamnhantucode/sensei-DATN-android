package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import com.phamnhantucode.aicareercoach.ui.components.CreditExhaustedDialog

/**
 * AI-powered responsibility improvement dialog
 * Shows improvement options and generates multiple AI suggestions
 */
@Composable
fun ResponsibilityAIDialog(
    currentText: String,
    jobTitle: String,
    company: String,
    onDismiss: () -> Unit,
    onSelectSuggestion: (String) -> Unit,
    userContext: com.phamnhantucode.aicareercoach.data.ai.ResponsibilityAIRepository.UserContext? = null,
    otherResponsibilities: List<String> = emptyList(),
    onNavigateToPurchase: () -> Unit = {},
    viewModel: ResponsibilityAIViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val options by viewModel.options.collectAsState()
    val showCreditDialog by viewModel.showCreditDialog.collectAsState()


    LaunchedEffect(Unit) {
        viewModel.reset()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Improve with AI",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))


                Text(
                    text = "$jobTitle at $company",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))


                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (uiState) {
                        is ResponsibilityAIState.Initial -> {
                            InitialOptionsView(
                                currentText = currentText,
                                options = options,
                                onToggleOption = { viewModel.toggleOption(it) }
                            )
                        }
                        is ResponsibilityAIState.Loading -> {
                            LoadingView()
                        }
                        is ResponsibilityAIState.Success -> {
                            SuggestionsView(
                                result = (uiState as ResponsibilityAIState.Success).result,
                                onSelectSuggestion = {
                                    onSelectSuggestion(it)
                                    onDismiss()
                                }
                            )
                        }
                        is ResponsibilityAIState.Error -> {
                            ErrorView(
                                message = (uiState as ResponsibilityAIState.Error).message,
                                onRetry = {
                                    viewModel.generateSuggestions(
                                        currentText = currentText,
                                        jobTitle = jobTitle,
                                        company = company,
                                        userContext = userContext,
                                        otherResponsibilities = otherResponsibilities
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }


                    AnimatedVisibility(
                        visible = uiState is ResponsibilityAIState.Initial,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Button(
                            onClick = {
                                viewModel.generateSuggestions(
                                    currentText = currentText,
                                    jobTitle = jobTitle,
                                    company = company,
                                    userContext = userContext,
                                    otherResponsibilities = otherResponsibilities
                                )
                            },
                            enabled = options.hasAnySelected()
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate")
                        }
                    }
                }
            }
            }
        }

    if (showCreditDialog) {
        CreditExhaustedDialog(
            onDismiss = { viewModel.dismissCreditDialog() },
            onPurchase = { 
                viewModel.dismissCreditDialog()
                onNavigateToPurchase()
            }
        )
    }
}


/**
 * Initial view with options selection
 */
@Composable
private fun InitialOptionsView(
    currentText: String,
    options: ResponsibilityImprovementOptions,
    onToggleOption: (OptionType) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Current Text",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Text(
                text = "Select improvement options:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            ImprovementOptionCard(
                title = "Make more professional",
                description = "Enhance language to be more formal and business-appropriate",
                icon = Icons.Default.BusinessCenter,
                isSelected = options.makeProfessional,
                onClick = { onToggleOption(OptionType.PROFESSIONAL) }
            )
        }

        item {
            ImprovementOptionCard(
                title = "Add metrics/numbers",
                description = "Suggest quantifiable achievements (e.g., 'increased by 20%')",
                icon = Icons.Default.ShowChart,
                isSelected = options.addMetrics,
                onClick = { onToggleOption(OptionType.ADD_METRICS) }
            )
        }

        item {
            ImprovementOptionCard(
                title = "Make more concise",
                description = "Shorten the text while keeping key points",
                icon = Icons.Default.Compress,
                isSelected = options.makeConcise,
                onClick = { onToggleOption(OptionType.CONCISE) }
            )
        }

        item {
            ImprovementOptionCard(
                title = "Make more detailed",
                description = "Expand with more context and specific actions",
                icon = Icons.Default.Article,
                isSelected = options.makeDetailed,
                onClick = { onToggleOption(OptionType.DETAILED) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                text = "Formatting:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            ImprovementOptionCard(
                title = "Format as bullet points",
                description = "Output text with bullet points (•) at the start of each line",
                icon = Icons.Default.FormatListBulleted,
                isSelected = options.formatAsBullets,
                onClick = { onToggleOption(OptionType.FORMAT_AS_BULLETS) }
            )
        }
    }
}

/**
 * Option card that can be selected/deselected
 */
@Composable
private fun ImprovementOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Loading view
 */
@Composable
private fun LoadingView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Generating suggestions...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "AI is crafting improved versions of your responsibility",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Suggestions view showing 3 AI-generated options
 */
@Composable
private fun SuggestionsView(
    result: ResponsibilityImprovementResult,
    onSelectSuggestion: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Choose your favorite version:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        itemsIndexed(result.suggestions) { index, suggestion ->
            SuggestionCard(
                number = index + 1,
                text = suggestion,
                onClick = { onSelectSuggestion(suggestion) }
            )
        }
    }
}

/**
 * Individual suggestion card
 */
@Composable
private fun SuggestionCard(
    number: Int,
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$number",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Use this")
            }
        }
    }
}

/**
 * Error view
 */
@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}
