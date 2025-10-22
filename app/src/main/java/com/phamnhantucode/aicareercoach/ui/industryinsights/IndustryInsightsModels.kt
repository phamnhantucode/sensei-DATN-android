package com.phamnhantucode.aicareercoach.ui.industryinsights

import java.time.LocalDate

enum class MarketOutlook {
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
}

enum class DemandLevel {
    HIGH,
    MEDIUM,
    LOW,
}

data class SalaryRangeUiModel(
    val role: String,
    val location: String,
    val min: Int,
    val median: Int,
    val max: Int,
)

data class IndustryInsightUiModel(
    val id: String,
    val name: String,
    val marketOutlook: MarketOutlook,
    val growthRate: Float,
    val demandLevel: DemandLevel,
    val topSkills: List<String>,
    val salaryRanges: List<SalaryRangeUiModel>,
    val keyTrends: List<String>,
    val recommendedSkills: List<String>,
    val lastUpdated: LocalDate,
    val nextUpdate: LocalDate,
)
