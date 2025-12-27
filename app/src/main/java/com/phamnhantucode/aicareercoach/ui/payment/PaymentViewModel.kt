package com.phamnhantucode.aicareercoach.ui.payment

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phamnhantucode.aicareercoach.data.neon.NeonAuth
import com.phamnhantucode.aicareercoach.data.payment.PaymentRepository
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentUiState(
    val isLoading: Boolean = false,
    val isReady: Boolean = false,
    val paymentIntent: String? = null,
    val ephemeralKey: String? = null,
    val customer: String? = null,
    val publishableKey: String? = null,
    val error: String? = null,
    val paymentResult: String? = null
)

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val paymentRepository = PaymentRepository()
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState = _uiState.asStateFlow()

    fun preparePaymentSheet() {
        _uiState.update { it.copy(isLoading = true, error = null, paymentResult = null) }
        viewModelScope.launch {
            try {
                val token = NeonAuth.fetchNeonAuthToken()
                if (token == null) {
                    _uiState.update { it.copy(isLoading = false, error = "User not authenticated") }
                    return@launch
                }

                val result = paymentRepository.fetchPaymentConfig(token)
                result.fold(
                    onSuccess = { config ->
                        _uiState.update {
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
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to fetch payment config"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    fun onPaymentResult(paymentSheetResult: PaymentSheetResult) {
        when(paymentSheetResult) {
            is PaymentSheetResult.Completed -> {
                Log.d("PaymentViewModel", "Payment completed")
                _uiState.update { it.copy(paymentResult = "Payment completed successfully!", isReady = false) }
            }
            is PaymentSheetResult.Canceled -> {
                Log.d("PaymentViewModel", "Payment canceled")
                _uiState.update { it.copy(paymentResult = "Payment canceled") }
            }
            is PaymentSheetResult.Failed -> {
                Log.e("PaymentViewModel", "Payment failed", paymentSheetResult.error)
                _uiState.update { it.copy(error = "Payment failed: ${paymentSheetResult.error.localizedMessage}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
