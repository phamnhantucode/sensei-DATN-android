package com.phamnhantucode.aicareercoach.ui.industryinsights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LineAxis
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.phamnhantucode.aicareercoach.ui.components.InsetAwareColumn
import com.phamnhantucode.aicareercoach.ui.onboarding.FormData
import com.phamnhantucode.aicareercoach.ui.onboarding.OnboardingContent
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun IndustryInsightsScreen(
    onNavigateToResumeBuilder: () -> Unit = {},
    onNavigateToInterviewPrep: () -> Unit = {},
    onNavigateToCoverLetter: () -> Unit = {},
    onNavigateToAccountSettings: () -> Unit = {},
    onNavigateToPro: () -> Unit = {},
    viewModel: IndustryInsightsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshInsights()
    }

    IndustryInsightsLayout(
        uiState = uiState,
        onRefresh = { viewModel.refreshInsights(forceRefresh = true) },
        onDismissError = viewModel::clearError,
        onIndustrySelected = viewModel::selectIndustry,
        onNavigateToResumeBuilder = onNavigateToResumeBuilder,
        onNavigateToInterviewPrep = onNavigateToInterviewPrep,
        onNavigateToCoverLetter = onNavigateToCoverLetter,
        onNavigateToAccountSettings = onNavigateToAccountSettings,
        creditBalance = uiState.creditBalance,
        onNavigateToPro = onNavigateToPro,
        onSubmitOnboarding = { viewModel.submitOnboarding(it) }
    )
}

@Composable
private fun IndustryInsightsLayout(
    uiState: IndustryInsightsUiState,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onIndustrySelected: (String) -> Unit,
    onNavigateToResumeBuilder: () -> Unit,
    onNavigateToInterviewPrep: () -> Unit,
    onNavigateToCoverLetter: () -> Unit,
    onNavigateToAccountSettings: () -> Unit,
    onNavigateToPro: () -> Unit,
    creditBalance: Int?,
    onSubmitOnboarding: (FormData) -> Unit
) {
    val selectedInsight = uiState.selectedInsight

    Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                uiState.isOnboardingRequired -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            HeaderSection(
                                userProfileImageUrl = uiState.userProfileImageUrl,
                                onNavigateToResumeBuilder = onNavigateToResumeBuilder,
                                onNavigateToInterviewPrep = onNavigateToInterviewPrep,
                                onNavigateToCoverLetter = onNavigateToCoverLetter,
                                onNavigateToAccountSettings = onNavigateToAccountSettings,
                                creditBalance = creditBalance,
                                onNavigateToPro = onNavigateToPro
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            IndustryInsightsTitle()
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            OnboardingContent(
                                isSubmitting = uiState.isSubmittingOnboarding,
                                submitError = uiState.errorMessage,
                                loadingMessage = if (uiState.isSubmittingOnboarding) "Setting up profile..." else null,
                                onComplete = onSubmitOnboarding
                            )
                        }
                    }
                }

                selectedInsight == null && uiState.isLoading -> {
                    IndustryInsightsLoading()
                }

                selectedInsight == null && uiState.errorMessage != null -> {
                    IndustryInsightsError(
                        message = uiState.errorMessage ?: "Unable to load industry insights.",
                        onRetry = onRefresh
                    )
                }

                selectedInsight != null -> {
                    InsetAwareColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        HeaderSection(
                            userProfileImageUrl = uiState.userProfileImageUrl,
                            onNavigateToResumeBuilder = onNavigateToResumeBuilder,
                            onNavigateToInterviewPrep = onNavigateToInterviewPrep,
                            onNavigateToCoverLetter = onNavigateToCoverLetter,
                            onNavigateToAccountSettings = onNavigateToAccountSettings,
                            creditBalance = creditBalance,
                            onNavigateToPro = onNavigateToPro
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        uiState.errorMessage?.let { message ->
                            ErrorBanner(
                                message = message,
                                onDismiss = onDismissError
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        DataFreshnessRow(
                            lastUpdated = selectedInsight.lastUpdated,
                            nextUpdate = selectedInsight.nextUpdate,
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = onRefresh
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        IndustrySelector(
                            insights = uiState.insights,
                            selectedId = selectedInsight.id,
                            onIndustrySelected = onIndustrySelected
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        MarketOverviewSection(insight = selectedInsight)

                        Spacer(modifier = Modifier.height(24.dp))

                        SalaryRangesCard(salaryRanges = selectedInsight.salaryRanges)

                        Spacer(modifier = Modifier.height(24.dp))

                        TrendsAndSkillsRow(insight = selectedInsight)
                    }
                }

                else -> {
                    IndustryInsightsLoading()
                }
            }
        }
}

@Composable
private fun HeaderSection(
    userProfileImageUrl: String?,
    onNavigateToResumeBuilder: () -> Unit,
    onNavigateToInterviewPrep: () -> Unit,
    onNavigateToCoverLetter: () -> Unit,
    onNavigateToAccountSettings: () -> Unit,
    creditBalance: Int?,
    onNavigateToPro: () -> Unit
) {
    var growthToolsExpanded by remember { mutableStateOf(false) }
    var growthToolsButtonWidth by remember { mutableStateOf(0) }
    val growthTools = remember {
        listOf("Build Resume", "Cover Letter", "Interview Prep")
    }
    val density = LocalDensity.current

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            var showUpgradeDialog by remember { mutableStateOf(false) }

            if (showUpgradeDialog) {
                UpgradeDialog(
                    onDismiss = { showUpgradeDialog = false },
                    onUpgrade = {
                        showUpgradeDialog = false
                        onNavigateToPro()
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                        .clickable { onNavigateToAccountSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    if (!userProfileImageUrl.isNullOrBlank()) {
                        // Show user's actual profile image
                        val context = LocalContext.current
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(userProfileImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "User avatar",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback to placeholder icon
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "User avatar",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Upgrade Button
                    Button(
                        onClick = { showUpgradeDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Upgrade",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Credit Balance Display
                    if (creditBalance != null) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .clickable { onNavigateToPro() }, // Make clickable to upgrade
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AttachMoney, // Or another appropriate icon
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$creditBalance Credits",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Box {
                    Surface(
                        onClick = { growthToolsExpanded = true },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            growthToolsButtonWidth = coordinates.size.width
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Growth Tools", color = MaterialTheme.colorScheme.background)
                            Icon(
                                imageVector = Icons.Filled.ExpandMore,
                                tint = MaterialTheme.colorScheme.background,
                                contentDescription = "Toggle growth tools"
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = growthToolsExpanded,
                        onDismissRequest = { growthToolsExpanded = false },
                        modifier = Modifier
                            .then(
                                if (growthToolsButtonWidth > 0) {
                                    Modifier.width(with(density) { growthToolsButtonWidth.toDp() })
                                } else {
                                    Modifier
                                }
                            )
                            .background(MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        growthTools.forEachIndexed { index, item ->
                            val icon = when (item) {
                                "Build Resume" -> Icons.Filled.Person
                                "Interview Prep" -> Icons.Filled.Groups
                                "Cover Letter" -> Icons.Outlined.Insights
                                else -> Icons.Filled.ChevronRight
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = item,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                onClick = {
                                    growthToolsExpanded = false
                                    when (item) {
                                        "Build Resume" -> onNavigateToResumeBuilder()
                                        "Interview Prep" -> onNavigateToInterviewPrep()
                                        "Cover Letter" -> onNavigateToCoverLetter()
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            if (index < growthTools.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    }
}

@Composable
private fun DataFreshnessRow(
    lastUpdated: LocalDate,
    nextUpdate: LocalDate,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = "Last updated: ${lastUpdated.format(formatter)}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = "Next update ${relativeDate(nextUpdate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh insights",
                    tint = if (isRefreshing)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun IndustryInsightsLoading() {
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header shimmer
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(16.dp)),
            shimmerProgress = shimmerProgress
        )
        
        // Title shimmer
        ShimmerBox(
            modifier = Modifier
                .width(200.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp)),
            shimmerProgress = shimmerProgress
        )
        
        // Industry chips shimmer
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                ShimmerBox(
                    modifier = Modifier
                        .width(100.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    shimmerProgress = shimmerProgress
                )
            }
        }
        
        // Stats cards shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(2) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    shimmerProgress = shimmerProgress
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(2) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    shimmerProgress = shimmerProgress
                )
            }
        }
        
        // Salary card shimmer
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(16.dp)),
            shimmerProgress = shimmerProgress
        )
    }
}

@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    shimmerProgress: Float
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )
    
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(shimmerProgress * 1000f - 500f, 0f),
                    end = Offset(shimmerProgress * 1000f, 0f)
                )
            )
    )
}

@Composable
private fun IndustryInsightsError(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun IndustrySelector(
    insights: List<IndustryInsightUiModel>,
    selectedId: String,
    onIndustrySelected: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(insights) { index, insight ->
            val isSelected = insight.id == selectedId
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            
            val scale by animateFloatAsState(
                targetValue = when {
                    isPressed -> 0.95f
                    isSelected -> 1.0f
                    else -> 1.0f
                },
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "chipScale"
            )
            
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                animationSpec = tween(300),
                label = "chipColor"
            )
            
            Surface(
                modifier = Modifier
                    .scale(scale)
                    .graphicsLayer {
                        shadowElevation = if (isSelected) 8f else 2f
                    },
                shape = RoundedCornerShape(24.dp),
                color = containerColor,
                onClick = { onIndustrySelected(insight.id) },
                interactionSource = interactionSource
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Industry icon
                    val icon = getIndustryIcon(insight.id)
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    Text(
                        text = insight.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    // Demand indicator dot
                    if (isSelected) {
                        val demandColor = when (insight.demandLevel) {
                            DemandLevel.HIGH -> Color(0xFF22C55E)
                            DemandLevel.MEDIUM -> Color(0xFFF97316)
                            DemandLevel.LOW -> MaterialTheme.colorScheme.error
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(demandColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpgradeDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Upgrade to Pro",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Unlock the full potential of your career journey with Pro features:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                ProFeatureItem(text = "Unlimited Resume Reviews")
                ProFeatureItem(text = "Advanced Industry Insights")
                ProFeatureItem(text = "Mock Interview Sessions")
                ProFeatureItem(text = "Priority Support")
            }
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Upgrade Now")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
private fun ProFeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun getIndustryIcon(industryId: String): ImageVector {
    return when (industryId.lowercase()) {
        "technology" -> Icons.Filled.ShowChart
        "finance" -> Icons.Filled.AttachMoney
        "healthcare" -> Icons.Filled.Verified
        else -> Icons.Outlined.Insights
    }
}

@Composable
private fun MarketOverviewSection(
    insight: IndustryInsightUiModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Quick Stats Summary Row
        QuickStatsSummary(insight = insight)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MarketOutlookCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                outlook = insight.marketOutlook,
                nextUpdate = insight.nextUpdate
            )
            GrowthCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                growthRate = insight.growthRate
            )
        }

        DemandLevelCard(
            modifier = Modifier.fillMaxWidth(),
            level = insight.demandLevel
        )

        HighlightSkillsCard(
            modifier = Modifier.fillMaxWidth(),
            skills = insight.topSkills
        )
    }
}

@Composable
private fun QuickStatsSummary(
    insight: IndustryInsightUiModel
) {
    val avgSalary = insight.salaryRanges.map { it.median }.average().roundToInt()
    val topSkillsCount = insight.topSkills.size
    val trendsCount = insight.keyTrends.size
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickStatItem(
                icon = Icons.Filled.AttachMoney,
                value = "$${avgSalary / 1000}k",
                label = "Avg Salary",
                color = MaterialTheme.colorScheme.primary
            )
            
            QuickStatDivider()
            
            QuickStatItem(
                icon = Icons.Filled.TrendingUp,
                value = "${String.format("%.1f", insight.growthRate)}%",
                label = "Growth",
                color = if (insight.growthRate >= 0) Color(0xFF22C55E) else MaterialTheme.colorScheme.error
            )
            
            QuickStatDivider()
            
            QuickStatItem(
                icon = Icons.Filled.Groups,
                value = insight.demandLevel.name.lowercase().replaceFirstChar { it.uppercase() },
                label = "Demand",
                color = when (insight.demandLevel) {
                    DemandLevel.HIGH -> Color(0xFF22C55E)
                    DemandLevel.MEDIUM -> Color(0xFFF97316)
                    DemandLevel.LOW -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun QuickStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickStatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun MarketOutlookCard(
    modifier: Modifier = Modifier,
    outlook: MarketOutlook,
    nextUpdate: LocalDate,
) {
    val (icon, tint) = marketOutlookVisuals(outlook)
    
    val gradientColors = when (outlook) {
        MarketOutlook.POSITIVE -> listOf(
            Color(0xFF22C55E).copy(alpha = 0.1f),
            Color(0xFF22C55E).copy(alpha = 0.02f)
        )
        MarketOutlook.NEUTRAL -> listOf(
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.02f)
        )
        MarketOutlook.NEGATIVE -> listOf(
            MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.error.copy(alpha = 0.02f)
        )
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = gradientColors,
                        center = Offset(0f, 0f),
                        radius = 500f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Market Outlook",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = tint
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = outlook.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    // Status indicator
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = tint.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = when (outlook) {
                                MarketOutlook.POSITIVE -> "↑"
                                MarketOutlook.NEUTRAL -> "→"
                                MarketOutlook.NEGATIVE -> "↓"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = tint,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Text(
                    text = "Next update ${relativeDate(nextUpdate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GrowthCard(
    modifier: Modifier = Modifier,
    growthRate: Float,
) {
    // Animated progress value
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(growthRate) {
        animatedProgress.animateTo(
            targetValue = (growthRate / 20f).coerceIn(0f, 1f),
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    // Animated counter
    val displayValue = remember { Animatable(0f) }
    LaunchedEffect(growthRate) {
        displayValue.animateTo(
            targetValue = growthRate,
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Industry Growth",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (growthRate >= 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (growthRate >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = String.format("%.1f", displayValue.value),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            // Animated progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.value)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                )
            }
            
            Text(
                text = "Year-over-year growth rate",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DemandLevelCard(
    modifier: Modifier = Modifier,
    level: DemandLevel,
) {
    val (label, color) = when (level) {
        DemandLevel.HIGH -> "High" to Color(0xFF22C55E)
        DemandLevel.MEDIUM -> "Medium" to Color(0xFFF97316)
        DemandLevel.LOW -> "Low" to MaterialTheme.colorScheme.error
    }
    
    // Animated demand level indicator
    val demandProgress = remember { Animatable(0f) }
    LaunchedEffect(level) {
        demandProgress.animateTo(
            targetValue = when (level) {
                DemandLevel.HIGH -> 1f
                DemandLevel.MEDIUM -> 0.6f
                DemandLevel.LOW -> 0.3f
            },
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Demand Level",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = color
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                // Animated pulse for high demand
                if (level == DemandLevel.HIGH) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .alpha(pulseAlpha)
                            .background(color)
                    )
                }
            }
            
            // Demand level bar with segments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(5) { index ->
                    val segmentProgress = ((demandProgress.value * 5) - index).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (segmentProgress > 0) color.copy(alpha = 0.3f + (segmentProgress * 0.7f))
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
            
            Text(
                text = "Based on hiring velocity across the past 90 days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HighlightSkillsCard(
    modifier: Modifier = Modifier,
    skills: List<String>,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Skills",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
//                Icon(
//                    imageVector = Icons.Filled.ChevronRight,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.onSurfaceVariant
//                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skills.forEach { skill ->
                    SkillChip(label = skill)
                }
            }
        }
    }
}

@Composable
private fun SalaryRangesCard(
    salaryRanges: List<SalaryRangeUiModel>,
) {
    val maxSalary = salaryRanges.maxOfOrNull { it.max }?.coerceAtLeast(1) ?: 1
    var expandedRoleIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Salary Ranges by Role",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap a role to see detailed breakdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Filled.AttachMoney,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(4.dp)
                )
            }
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SalaryLegendItem(color = MaterialTheme.colorScheme.surfaceVariant, label = "Min")
                SalaryLegendItem(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), label = "Median")
                SalaryLegendItem(color = MaterialTheme.colorScheme.primary, label = "Max")
            }

            salaryRanges.forEachIndexed { index, range ->
                SalaryRangeRow(
                    range = range,
                    maxSalary = maxSalary,
                    isExpanded = expandedRoleIndex == index,
                    onClick = {
                        expandedRoleIndex = if (expandedRoleIndex == index) null else index
                    }
                )
                if (range != salaryRanges.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SalaryLegendItem(
    color: Color,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SalaryRangeRow(
    range: SalaryRangeUiModel,
    maxSalary: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(
                if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(if (isExpanded) 12.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = range.role,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = range.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Median salary badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "$${range.median / 1000}k",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Range visualization bar
        SalaryRangeVisualization(
            min = range.min,
            median = range.median,
            max = range.max,
            maxSalary = maxSalary
        )
        
        // Expanded details
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SalaryDetailItem(label = "Minimum", value = "$${range.min / 1000}k")
                SalaryDetailItem(label = "Median", value = "$${range.median / 1000}k")
                SalaryDetailItem(label = "Maximum", value = "$${range.max / 1000}k")
            }
        }
    }
}

@Composable
private fun SalaryRangeVisualization(
    min: Int,
    median: Int,
    max: Int,
    maxSalary: Int
) {
    val animatedMin = remember { Animatable(0f) }
    val animatedMedian = remember { Animatable(0f) }
    val animatedMax = remember { Animatable(0f) }
    
    LaunchedEffect(min, median, max) {
        animatedMin.animateTo((min.toFloat() / maxSalary).coerceIn(0f, 1f), tween(600))
    }
    LaunchedEffect(min, median, max) {
        animatedMedian.animateTo((median.toFloat() / maxSalary).coerceIn(0f, 1f), tween(800))
    }
    LaunchedEffect(min, median, max) {
        animatedMax.animateTo((max.toFloat() / maxSalary).coerceIn(0f, 1f), tween(1000))
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        // Background track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        // Range bar (from min to max)
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedMax.value)
                .padding(start = (animatedMin.value * 100).dp.coerceAtMost(200.dp))
                .height(8.dp)
                .align(Alignment.CenterStart)
                .offset(x = (animatedMin.value * 100).dp.coerceAtMost(50.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
            )
        }
        
        // Median indicator dot
        Box(
            modifier = Modifier
                .offset(x = (animatedMedian.value * 280).dp.coerceAtMost(280.dp))
                .size(16.dp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary)
            )
        }
    }
}

@Composable
private fun SalaryDetailItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrendsAndSkillsRow(insight: IndustryInsightUiModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        KeyTrendsCard(
            modifier = Modifier.fillMaxWidth(),
            trends = insight.keyTrends,
        )
        RecommendedSkillsCard(
            modifier = Modifier.fillMaxWidth(),
            skills = insight.recommendedSkills
        )
    }
}

@Composable
private fun KeyTrendsCard(
    modifier: Modifier = Modifier,
    trends: List<String>,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Key Industry Trends",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Signals shaping the market right now",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            trends.forEachIndexed { index, trend ->
                TrendBullet(
                    text = trend,
                    index = index + 1
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecommendedSkillsCard(
    modifier: Modifier = Modifier,
    skills: List<String>,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Recommended Skills",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Focus areas to grow your edge",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                skills.forEachIndexed { index, skill ->
                    AnimatedSkillChip(
                        label = skill,
                        index = index,
                        emphasized = true
                    )
                }
            }
            
//            Button(
//                onClick = { },
//                shape = RoundedCornerShape(12.dp),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.primary
//                ),
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Add to learning plan")
//                Spacer(modifier = Modifier.width(8.dp))
//                Icon(
//                    imageVector = Icons.Default.ChevronRight,
//                    contentDescription = null,
//                    modifier = Modifier.size(18.dp)
//                )
//            }
        }
    }
}

@Composable
private fun AnimatedSkillChip(
    label: String,
    index: Int,
    emphasized: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 100L)
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        )
    ) {
        SkillChip(label = label, emphasized = emphasized)
    }
}

@Composable
private fun SkillChip(
    label: String,
    emphasized: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (emphasized) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (emphasized) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal,
                color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TrendBullet(
    text: String,
    index: Int
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 150L)
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(400)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun marketOutlookVisuals(outlook: MarketOutlook): Pair<ImageVector, Color> {
    return when (outlook) {
        MarketOutlook.POSITIVE -> Icons.Filled.TrendingUp to MaterialTheme.colorScheme.primary
        MarketOutlook.NEUTRAL -> Icons.Filled.LineAxis to MaterialTheme.colorScheme.tertiary
        MarketOutlook.NEGATIVE -> Icons.Filled.TrendingDown to MaterialTheme.colorScheme.error
    }
}

private fun relativeDate(target: LocalDate): String {
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(today, target).toInt()

    return when {
        days == 0 -> "today"
        days > 0 -> if (days == 1) "in 1 day" else "in $days days"
        else -> {
            val pastDays = abs(days)
            if (pastDays == 1) "1 day ago" else "$pastDays days ago"
        }
    }
}

private fun sampleInsights(): Map<String, IndustryInsightUiModel> {
    val today = LocalDate.now()
    return listOf(
        IndustryInsightUiModel(
            id = "technology",
            name = "Technology",
            marketOutlook = MarketOutlook.POSITIVE,
            growthRate = 12.4f,
            demandLevel = DemandLevel.HIGH,
            topSkills = listOf(
                "AI/ML",
                "Cloud Architecture",
                "Cybersecurity",
                "Data Engineering",
                "Product Strategy"
            ),
            salaryRanges = listOf(
                SalaryRangeUiModel("Machine Learning Engineer", "Remote · US", 115000, 152000, 185000),
                SalaryRangeUiModel("Senior Software Engineer", "SF Bay Area · US", 135000, 168000, 210000),
                SalaryRangeUiModel("Product Manager", "Austin · US", 110000, 145000, 185000),
                SalaryRangeUiModel("Security Engineer", "Seattle · US", 120000, 150000, 195000),
                SalaryRangeUiModel("Data Scientist", "New York · US", 118000, 155000, 190000)
            ),
            keyTrends = listOf(
                "Generative AI adoption is driving demand for applied machine learning roles.",
                "Cloud-native modernization continues with strong platform engineering roadmaps.",
                "Zero-trust security programs are accelerating cross-functional security hiring."
            ),
            recommendedSkills = listOf(
                "Prompt Engineering",
                "MLOps",
                "Platform Architecture",
                "Secure SDLC",
                "Stakeholder Storytelling"
            ),
            lastUpdated = today.minusDays(3),
            nextUpdate = today.plusDays(4)
        ),
        IndustryInsightUiModel(
            id = "finance",
            name = "Financial Services",
            marketOutlook = MarketOutlook.POSITIVE,
            growthRate = 7.8f,
            demandLevel = DemandLevel.HIGH,
            topSkills = listOf(
                "Risk Analytics",
                "Python",
                "Financial Modelling",
                "RegTech"
            ),
            salaryRanges = listOf(
                SalaryRangeUiModel("Quantitative Analyst", "New York · US", 125000, 160000, 210000),
                SalaryRangeUiModel("Risk Manager", "Chicago · US", 105000, 138000, 175000),
                SalaryRangeUiModel("FinTech Product Lead", "Remote · US", 115000, 148000, 185000),
                SalaryRangeUiModel("Data Engineer", "Toronto · CA", 95000, 130000, 168000),
                SalaryRangeUiModel("Compliance Officer", "London · UK", 80000, 110000, 140000)
            ),
            keyTrends = listOf(
                "Open banking APIs are reshaping payment experiences worldwide.",
                "Automation of regulatory reporting is unlocking enterprise productivity.",
                "Digital asset custody services are gaining institutional adoption."
            ),
            recommendedSkills = listOf(
                "Python for Finance",
                "Regulation Technology",
                "Data Storytelling",
                "Sustainable Finance Strategy"
            ),
            lastUpdated = today.minusDays(5),
            nextUpdate = today.plusDays(2)
        ),
        IndustryInsightUiModel(
            id = "healthcare",
            name = "Healthcare & Life Sciences",
            marketOutlook = MarketOutlook.POSITIVE,
            growthRate = 9.1f,
            demandLevel = DemandLevel.HIGH,
            topSkills = listOf(
                "Clinical Data Science",
                "Telehealth Operations",
                "Regulatory Affairs",
                "Population Health"
            ),
            salaryRanges = listOf(
                SalaryRangeUiModel("Clinical Data Scientist", "Boston · US", 98000, 132000, 170000),
                SalaryRangeUiModel("Digital Health PM", "Remote · US", 105000, 140000, 175000),
                SalaryRangeUiModel("Healthcare Analyst", "Los Angeles · US", 88000, 118000, 145000),
                SalaryRangeUiModel("Telemedicine Lead", "Remote · US", 95000, 125000, 158000),
                SalaryRangeUiModel("Regulatory Specialist", "Berlin · DE", 70000, 96000, 125000)
            ),
            keyTrends = listOf(
                "Remote patient monitoring programs are becoming standard offerings.",
                "AI-assisted diagnostics are improving throughput across care teams.",
                "Personalized therapeutics are receiving increased investment attention."
            ),
            recommendedSkills = listOf(
                "FHIR & HL7 Integration",
                "Digital Therapeutics",
                "Clinical Trial Analytics",
                "Change Management"
            ),
            lastUpdated = today.minusDays(2),
            nextUpdate = today.plusDays(5)
        )
    ).associateBy { it.id }
}

@Composable
@Preview(showBackground = true)
private fun IndustryInsightsScreenPreview() {
    val sampleInsights = sampleInsights()
    val uiState = IndustryInsightsUiState(
        isLoading = false,
        insights = sampleInsights.values.toList(),
        selectedIndustryId = sampleInsights.keys.first(),
        errorMessage = null
    )
    AppTheme(darkTheme = true) {
        IndustryInsightsLayout(
            uiState = uiState,
            onRefresh = {},
            onDismissError = {},
            onIndustrySelected = {},
            onNavigateToResumeBuilder = {},
            onNavigateToInterviewPrep = {},
            onNavigateToCoverLetter = {},
            onNavigateToAccountSettings = {},
            onNavigateToPro = {},
            creditBalance = 10,
            onSubmitOnboarding = {}
        )
    }
}

@Composable
private fun IndustryInsightsTitle() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Industry Insights",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Stay on top of market outlooks, salary bands, and in-demand skills.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
