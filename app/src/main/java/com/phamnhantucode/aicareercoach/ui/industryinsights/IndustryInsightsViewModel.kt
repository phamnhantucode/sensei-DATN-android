package com.phamnhantucode.aicareercoach.ui.industryinsights

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.data.industry.IndustryInsightsRepository
import com.phamnhantucode.aicareercoach.data.industry.IndustryInsightsRepository.IndustryInsightRecord
import com.phamnhantucode.aicareercoach.data.industry.IndustryInsightsRepository.IndustryInsightLoadResult
import com.phamnhantucode.aicareercoach.data.industry.IndustryInsightsRepository.SalaryRangeRecord
import com.phamnhantucode.aicareercoach.ui.onboarding.IndustriesData
import com.phamnhantucode.aicareercoach.utils.IndustryFormatUtils
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IndustryInsightsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val insights: List<IndustryInsightUiModel> = emptyList(),
    val selectedIndustryId: String? = null,
    val errorMessage: String? = null,
    val userProfileImageUrl: String? = null,
    val creditBalance: Int? = null,
) {
    val selectedInsight: IndustryInsightUiModel?
        get() = selectedIndustryId?.let { id ->
            insights.firstOrNull { it.id == id }
        } ?: insights.firstOrNull()
}

class IndustryInsightsViewModel(
    private val repository: IndustryInsightsRepository = IndustryInsightsRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(IndustryInsightsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        // Subscribe to Clerk user changes to update profile image
        Clerk.userFlow.onEach { user ->
            _uiState.update { state ->
                state.copy(userProfileImageUrl = user?.imageUrl)
            }
        }.launchIn(viewModelScope)

        refreshInsights()
    }

    fun refreshInsights(forceRefresh: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            val hasExistingUiData = _uiState.value.insights.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hasExistingUiData,
                    isRefreshing = hasExistingUiData,
                    errorMessage = null
                )
            }

            val loadResult = try {
                repository.loadIndustryInsights(forceRefresh)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Log.e(TAG, "Failed to load industry insights.", error)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.localizedMessage ?: "Unable to load industry insights."
                    )
                }
                return@launch
            }

            handleLoadedInsights(loadResult)
            if (!loadResult.needsRefresh) return@launch

            val existing = loadResult.insight
            try {
                val refreshed = repository.refreshIndustryInsights(
                    industry = loadResult.industry,
                    authorizationHeader = loadResult.authorizationHeader
                )
                val insight = refreshed.toUiModel()
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        insights = listOf(insight),
                        selectedIndustryId = insight.id,
                        errorMessage = null
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Log.e(TAG, "Failed to refresh industry insights.", error)
                _uiState.update { state ->
                    state.copy(
                        isLoading = existing == null,
                        isRefreshing = false,
                        errorMessage = error.localizedMessage
                            ?: "Unable to refresh industry insights."
                    )
                }
            }
        }
    }

    fun selectIndustry(industryId: String) {
        _uiState.update { state ->
            if (state.selectedIndustryId == industryId) state
            else state.copy(selectedIndustryId = industryId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun handleLoadedInsights(loadResult: IndustryInsightLoadResult) {
        val existingRecord = loadResult.insight
        if (existingRecord != null) {
            val insight = existingRecord.toUiModel()
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    isRefreshing = loadResult.needsRefresh,
                    insights = listOf(insight),
                    selectedIndustryId = insight.id,
                    errorMessage = null,
                    creditBalance = loadResult.creditBalance
                )
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    isRefreshing = false,
                    insights = emptyList(),
                    selectedIndustryId = null,
                    errorMessage = null,
                    creditBalance = loadResult.creditBalance
                )
            }
        }
    }

    private fun IndustryInsightRecord.toUiModel(): IndustryInsightUiModel {
        val zone = ZoneId.systemDefault()
        val resolvedLastUpdated = lastUpdated.takeUnless { it == Instant.EPOCH } ?: Instant.now()
        val resolvedNextUpdate = nextUpdate.takeUnless { it == Instant.EPOCH }
            ?: resolvedLastUpdated.plus(7, ChronoUnit.DAYS)
        return IndustryInsightUiModel(
            id = id,
            name = IndustryFormatUtils.formatIndustryForDisplay(
                formattedIndustry = industry,
                industries = IndustriesData.industries,
                getId = { it.id },
                getName = { it.name }
            ),
            marketOutlook = marketOutlook.toMarketOutlook(),
            growthRate = growthRate,
            demandLevel = demandLevel.toDemandLevel(),
            topSkills = topSkills,
            salaryRanges = salaryRanges.map { it.toUiModel() },
            keyTrends = keyTrends,
            recommendedSkills = recommendedSkills,
            lastUpdated = resolvedLastUpdated.atZone(zone).toLocalDate(),
            nextUpdate = resolvedNextUpdate.atZone(zone).toLocalDate(),
        )
    }

    private fun SalaryRangeRecord.toUiModel(): SalaryRangeUiModel {
        return SalaryRangeUiModel(
            role = role,
            location = location,
            min = min,
            median = median,
            max = max,
        )
    }

    private fun String.toMarketOutlook(): MarketOutlook {
        return when (uppercase(Locale.US)) {
            "POSITIVE" -> MarketOutlook.POSITIVE
            "NEGATIVE" -> MarketOutlook.NEGATIVE
            else -> MarketOutlook.NEUTRAL
        }
    }

    private fun String.toDemandLevel(): DemandLevel {
        return when (uppercase(Locale.US)) {
            "HIGH" -> DemandLevel.HIGH
            "LOW" -> DemandLevel.LOW
            else -> DemandLevel.MEDIUM
        }
    }

    companion object {
        private const val TAG = "IndustryInsightsVM"
    }
}
