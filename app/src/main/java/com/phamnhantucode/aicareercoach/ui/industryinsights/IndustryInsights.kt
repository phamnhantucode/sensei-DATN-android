package com.phamnhantucode.aicareercoach.ui.industryinsights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LineAxis
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.WbSunny
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.ui.components.InsetAwareColumn
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private enum class MarketOutlook {
    POSITIVE, NEUTRAL, NEGATIVE
}

private enum class DemandLevel {
    HIGH, MEDIUM, LOW
}

private data class IndustryInsight(
    val id: String,
    val name: String,
    val marketOutlook: MarketOutlook,
    val growthRate: Float,
    val demandLevel: DemandLevel,
    val topSkills: List<String>,
    val salaryRanges: List<SalaryRange>,
    val keyTrends: List<String>,
    val recommendedSkills: List<String>,
    val lastUpdated: LocalDate,
    val nextUpdate: LocalDate,
)

private data class SalaryRange(
    val role: String,
    val location: String,
    val min: Int,
    val median: Int,
    val max: Int,
)

@Composable
fun IndustryInsightsScreen(
    onNavigateToResumeBuilder: () -> Unit = {},
    onNavigateToInterviewPrep: () -> Unit = {},
    onNavigateToCoverLetter: () -> Unit = {}
) {
    var darkTheme by remember { mutableStateOf(true) }
    val insightMap = remember { sampleInsights() }
    var selectedIndustryId by remember { mutableStateOf(insightMap.keys.first()) }
    val selectedInsight = insightMap[selectedIndustryId] ?: insightMap.values.first()

    AppTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            InsetAwareColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                HeaderSection(
                    darkTheme = darkTheme,
                    onThemeToggle = { darkTheme = !darkTheme },
                    onNavigateToResumeBuilder = onNavigateToResumeBuilder,
                    onNavigateToInterviewPrep = onNavigateToInterviewPrep,
                    onNavigateToCoverLetter = onNavigateToCoverLetter
                )

                Spacer(modifier = Modifier.height(16.dp))

                DataFreshnessRow(
                    lastUpdated = selectedInsight.lastUpdated,
                    nextUpdate = selectedInsight.nextUpdate
                )

                Spacer(modifier = Modifier.height(16.dp))

                IndustrySelector(
                    insights = insightMap.values.toList(),
                    selectedId = selectedIndustryId,
                    onIndustrySelected = { selectedIndustryId = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                MarketOverviewSection(insight = selectedInsight)

                Spacer(modifier = Modifier.height(24.dp))

                SalaryRangesCard(salaryRanges = selectedInsight.salaryRanges)

                Spacer(modifier = Modifier.height(24.dp))

                TrendsAndSkillsRow(insight = selectedInsight)

            }
        }
    }
}

@Composable
private fun HeaderSection(
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onNavigateToResumeBuilder: () -> Unit,
    onNavigateToInterviewPrep: () -> Unit,
    onNavigateToCoverLetter: () -> Unit
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
                        ),
                    contentAlignment = Alignment.Center
                ) {
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

                Box {
                    Button(
                        onClick = { growthToolsExpanded = true },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            growthToolsButtonWidth = coordinates.size.width
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Text(text = "Growth Tools", color = MaterialTheme.colorScheme.background)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            tint = MaterialTheme.colorScheme.background,
                            contentDescription = "Toggle growth tools"
                        )
                    }
                    DropdownMenu(
                        expanded = growthToolsExpanded,
                        onDismissRequest = { growthToolsExpanded = false },
                        modifier = if (growthToolsButtonWidth > 0) {
                            Modifier.width(with(density) { growthToolsButtonWidth.toDp() })
                        } else {
                            Modifier
                        }
                    ) {
                        growthTools.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(text = item) },
                                onClick = {
                                    growthToolsExpanded = false
                                    when (item) {
                                        "Build Resume" -> onNavigateToResumeBuilder()
                                        "Interview Prep" -> onNavigateToInterviewPrep()
                                        "Cover Letter" -> onNavigateToCoverLetter()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
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
            IconButton(
                onClick = onThemeToggle,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = if (darkTheme) Icons.Outlined.WbSunny else Icons.Outlined.Analytics,
                    contentDescription = "Toggle theme"
                )
            }
        }
    }
}

@Composable
private fun DataFreshnessRow(
    lastUpdated: LocalDate,
    nextUpdate: LocalDate,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.padding(end = 8.dp)
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
}

@Composable
private fun IndustrySelector(
    insights: List<IndustryInsight>,
    selectedId: String,
    onIndustrySelected: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(insights) { insight ->
            ElevatedFilterChip(
                selected = insight.id == selectedId,
                onClick = { onIndustrySelected(insight.id) },
                label = { Text(insight.name) },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun MarketOverviewSection(
    insight: IndustryInsight,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MarketOutlookCard(
                modifier = Modifier.weight(1f),
                outlook = insight.marketOutlook,
                nextUpdate = insight.nextUpdate
            )
            GrowthCard(
                modifier = Modifier.weight(1f),
                growthRate = insight.growthRate
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DemandLevelCard(
                modifier = Modifier.weight(1f),
                level = insight.demandLevel
            )
            HighlightSkillsCard(
                modifier = Modifier.weight(1f),
                skills = insight.topSkills
            )
        }
    }
}

@Composable
private fun MarketOutlookCard(
    modifier: Modifier = Modifier,
    outlook: MarketOutlook,
    nextUpdate: LocalDate,
) {
    val (icon, tint) = marketOutlookVisuals(outlook)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint
                )
            }
            Text(
                text = outlook.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Next update ${relativeDate(nextUpdate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GrowthCard(
    modifier: Modifier = Modifier,
    growthRate: Float,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                Icon(
                    imageVector = Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "${String.format("%.1f", growthRate)}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { (growthRate / 20f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
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

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    text = "Demand Level",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = color
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.6f))
            )
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
        )
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
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    salaryRanges: List<SalaryRange>,
) {
    val maxSalary = salaryRanges.maxOfOrNull { it.max }?.coerceAtLeast(1) ?: 1

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    text = "Minimum, median, and maximum annual compensation (USD)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            salaryRanges.forEach { range ->
                SalaryRangeRow(range = range, maxSalary = maxSalary)
                if (range != salaryRanges.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SalaryRangeRow(
    range: SalaryRange,
    maxSalary: Int,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = range.role,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = range.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$${range.median / 1000}k median",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SalaryBar(
                modifier = Modifier.weight(1f),
                label = "Min",
                value = range.min,
                maxSalary = maxSalary,
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            SalaryBar(
                modifier = Modifier.weight(1f),
                label = "Median",
                value = range.median,
                maxSalary = maxSalary,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            )
            SalaryBar(
                modifier = Modifier.weight(1f),
                label = "Max",
                value = range.max,
                maxSalary = maxSalary,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SalaryBar(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    maxSalary: Int,
    color: Color,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((value / maxSalary.toFloat()).coerceIn(0f, 1f))
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(color)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$${value / 1000}k",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TrendsAndSkillsRow(insight: IndustryInsight) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        KeyTrendsCard(
            modifier = Modifier.weight(1f),
            trends = insight.keyTrends,
        )
        RecommendedSkillsCard(
            modifier = Modifier.weight(1f),
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
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Key Industry Trends",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Signals shaping the market right now",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trends.forEach { trend ->
                TrendBullet(text = trend)
            }
        }
    }
}

@Composable
private fun RecommendedSkillsCard(
    modifier: Modifier = Modifier,
    skills: List<String>,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Recommended Skills",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Focus areas to grow your edge",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            skills.forEach { skill ->
                SkillChip(
                    label = skill,
                    emphasized = true
                )
            }
            OutlinedButton(
                onClick = { },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Add to learning plan")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        }
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
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TrendBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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

private fun sampleInsights(): Map<String, IndustryInsight> {
    val today = LocalDate.now()
    return listOf(
        IndustryInsight(
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
                SalaryRange("Machine Learning Engineer", "Remote · US", 115000, 152000, 185000),
                SalaryRange("Senior Software Engineer", "SF Bay Area · US", 135000, 168000, 210000),
                SalaryRange("Product Manager", "Austin · US", 110000, 145000, 185000),
                SalaryRange("Security Engineer", "Seattle · US", 120000, 150000, 195000),
                SalaryRange("Data Scientist", "New York · US", 118000, 155000, 190000)
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
        IndustryInsight(
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
                SalaryRange("Quantitative Analyst", "New York · US", 125000, 160000, 210000),
                SalaryRange("Risk Manager", "Chicago · US", 105000, 138000, 175000),
                SalaryRange("FinTech Product Lead", "Remote · US", 115000, 148000, 185000),
                SalaryRange("Data Engineer", "Toronto · CA", 95000, 130000, 168000),
                SalaryRange("Compliance Officer", "London · UK", 80000, 110000, 140000)
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
        IndustryInsight(
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
                SalaryRange("Clinical Data Scientist", "Boston · US", 98000, 132000, 170000),
                SalaryRange("Digital Health PM", "Remote · US", 105000, 140000, 175000),
                SalaryRange("Healthcare Analyst", "Los Angeles · US", 88000, 118000, 145000),
                SalaryRange("Telemedicine Lead", "Remote · US", 95000, 125000, 158000),
                SalaryRange("Regulatory Specialist", "Berlin · DE", 70000, 96000, 125000)
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
    IndustryInsightsScreen()
}
