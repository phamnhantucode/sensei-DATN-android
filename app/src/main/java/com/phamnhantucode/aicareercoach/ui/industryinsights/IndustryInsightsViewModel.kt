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
import com.phamnhantucode.aicareercoach.ui.onboarding.FormData
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService

data class IndustryInsightsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val insights: List<IndustryInsightUiModel> = emptyList(),
    val selectedIndustryId: String? = null,
    val errorMessage: String? = null,
    val userProfileImageUrl: String? = null,
    val creditBalance: Int? = null,
    val isOnboardingRequired: Boolean = false,
    val isSubmittingOnboarding: Boolean = false,
) {
    val selectedInsight: IndustryInsightUiModel?
        get() = selectedIndustryId?.let { id ->
            insights.firstOrNull { it.id == id }
        } ?: insights.firstOrNull()
}

class IndustryInsightsViewModel(
    private val repository: IndustryInsightsRepository = IndustryInsightsRepository(),
    private val paymentRepository: com.phamnhantucode.aicareercoach.data.payment.PaymentRepository = com.phamnhantucode.aicareercoach.data.payment.PaymentRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(IndustryInsightsUiState())
    val uiState = _uiState.asStateFlow()
    
    // Payment specific state
    private val _paymentState = MutableStateFlow(PaymentState())
    val paymentState = _paymentState.asStateFlow()

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

    fun preparePaymentSheet() {
        _paymentState.update { it.copy(isLoading = true, error = null, paymentResult = null) }
        viewModelScope.launch {
            try {
                val token = com.phamnhantucode.aicareercoach.data.neon.NeonAuth.fetchNeonAuthToken()
                if (token == null) {
                    _paymentState.update { it.copy(isLoading = false, error = "User not authenticated") }
                    return@launch
                }

                val result = paymentRepository.fetchPaymentConfig(token)
                result.fold(
                    onSuccess = { config ->
                        _paymentState.update {
                            it.copy(
                                isLoading = false,
                                isReady = true,
                                paymentIntent = config.paymentIntent,
                                ephemeralKey = config.ephemeralKey,
                                customer = config.customer,
                                publishableKey = config.publishableKey
                            )
                        }
                    },
                    onFailure = { error ->
                        _paymentState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to fetch payment config"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _paymentState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    fun onPaymentResult(paymentSheetResult: com.stripe.android.paymentsheet.PaymentSheetResult) {
        when(paymentSheetResult) {
            is com.stripe.android.paymentsheet.PaymentSheetResult.Completed -> {
                Log.d("IndustryInsightsViewModel", "Payment completed")
                _paymentState.update { it.copy(paymentResult = "Payment completed successfully!", isReady = false) }
                // Refresh credits
                refreshInsights(forceRefresh = true)
            }
            is com.stripe.android.paymentsheet.PaymentSheetResult.Canceled -> {
                Log.d("IndustryInsightsViewModel", "Payment canceled")
                _paymentState.update { it.copy(paymentResult = "Payment canceled") }
            }
            is com.stripe.android.paymentsheet.PaymentSheetResult.Failed -> {
                Log.e("IndustryInsightsViewModel", "Payment failed", paymentSheetResult.error)
                _paymentState.update { it.copy(error = "Payment failed: ${paymentSheetResult.error.localizedMessage}") }
            }
        }
    }
    
    fun resetPaymentState() {
        _paymentState.update { PaymentState() }
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

    fun submitOnboarding(formData: FormData) {
        if (_uiState.value.isSubmittingOnboarding) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingOnboarding = true, errorMessage = null) }
            try {
                val industries = IndustriesData.industries
                val selectedIndustry = industries.find { it.id == formData.industryId }
                val user = Clerk.user

                if (selectedIndustry == null) {
                    throw IllegalStateException("Please select an industry.")
                } else if (user == null) {
                    throw IllegalStateException("User session unavailable. Please sign in again.")
                } else if (formData.subIndustry.isBlank()) {
                    throw IllegalStateException("Please select a specialization.")
                }

                // Format industry data for API (industryId---sub-industry-kebab)
                val formattedIndustry = IndustryFormatUtils.formatIndustryForApi(
                    industryId = selectedIndustry!!.id,
                    subIndustry = formData.subIndustry
                )

                // Step 1: Get auth token
                val authToken = com.phamnhantucode.aicareercoach.data.neon.NeonAuth.fetchNeonAuthToken()
                    ?: throw IllegalStateException("Unable to fetch authentication token.")

                // Step 2: Ensure IndustryInsight exists BEFORE creating user
                val authorizationHeader = "Bearer $authToken"
                repository.ensureIndustryInsightExists(
                    industry = formattedIndustry,
                    authorizationHeader = authorizationHeader,
                )

                // Step 3: Create/update user (ensure exists)
                val email = user.emailAddresses.firstOrNull()?.emailAddress ?: throw IllegalStateException("User email not found")
                NeonUserService.syncUser(user.id, email)

                // Step 4: Update additional user profile fields
                val skills = formData.skills.split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                val experienceYears = formData.experienceYears.trim().toIntOrNull()
                val profile = NeonUserService.UserProfileUpdate(
                    industry = formattedIndustry,
                    experienceYears = experienceYears,
                    skills = skills,
                    bio = formData.bio.takeIf { it.isNotBlank() },
                )

                NeonUserService.updateUserProfile(
                    clerkUserId = user.id,
                    update = profile
                )

                // On success, refresh the whole screen
                refreshInsights(forceRefresh = true)

            } catch (e: Exception) {
               _uiState.update {
                   it.copy(
                       isSubmittingOnboarding = false,
                       errorMessage = e.localizedMessage ?: "Failed to save profile. Please try again."
                   )
               }
            } finally {
                // We keep isSubmittingOnboarding true if successful, until refresh finishes? 
                // Using refreshInsights will reset states.
                // But refreshInsights logic sets isLoading = true etc.
                // Let's ensure we reset isSubmittingOnboarding in case of success by the refresh call logic 
                // or here if we want to be safe.
                // If refresh started, it will update state.
            }
        }
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
                    creditBalance = loadResult.creditBalance,
                    isOnboardingRequired = false,
                    isSubmittingOnboarding = false
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
                    creditBalance = loadResult.creditBalance,
                    isOnboardingRequired = true // No insights found means potentially new user
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

data class PaymentState(
    val isLoading: Boolean = false,
    val isReady: Boolean = false,
    val paymentIntent: String? = null,
    val ephemeralKey: String? = null,
    val customer: String? = null,
    val publishableKey: String? = null,
    val error: String? = null,
    val paymentResult: String? = null
)
