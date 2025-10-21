package com.phamnhantucode.aicareercoach.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.session.fetchToken
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.attemptVerification
import com.clerk.api.signup.prepareVerification
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.user.User
import com.phamnhantucode.aicareercoach.BuildConfig
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private var lastSyncedUserId: String? = null
    private var syncInProgress = false

    init {
        combine(
            Clerk.isInitialized,
            Clerk.userFlow
        ) { isInitialized, user ->
            isInitialized to user
        }.onEach { (isInitialized, user) ->
            _uiState.update { current ->
                current.copy(
                    isInitialized = isInitialized,
                    isSignedIn = user != null
                )
            }
            if (user != null) {
                syncUserIfNeeded(user)
            } else {
                lastSyncedUserId = null
            }
        }.launchIn(viewModelScope)
    }

    fun signIn(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Please enter both your email and password.")
            }
            return
        }
        _uiState.update {
            it.copy(
                isProcessing = true,
                errorMessage = null,
                verificationEmail = null
            )
        }
        viewModelScope.launch {
            SignIn
                .create(
                    SignIn.CreateParams.Strategy.Password(
                        identifier = trimmedEmail,
                        password = password
                    )
                )
                .onSuccess {
                    _uiState.update { state -> state.copy(isProcessing = false) }
                }
                .onFailure { failure ->
                    _uiState.update { state ->
                        state.copy(
                            isProcessing = false,
                            errorMessage = failure.longErrorMessageOrNull
                                ?: "Unable to sign in. Please try again."
                        )
                    }
                }
        }
    }

    fun signUp(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Please enter both your email and a password.")
            }
            return
        }
        _uiState.update {
            it.copy(
                isProcessing = true,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            SignUp
                .create(
                    SignUp.CreateParams.Standard(
                        emailAddress = trimmedEmail,
                        password = password
                    )
                )
                .onSuccess { signUp ->
                    if (signUp.status == SignUp.Status.COMPLETE) {
                        _uiState.update { state ->
                            state.copy(
                                isProcessing = false,
                                verificationEmail = null
                            )
                        }
                        return@onSuccess
                    }

                    when (
                        val verificationResult =
                            signUp.prepareVerification(
                                SignUp.PrepareVerificationParams.Strategy.EmailCode()
                            )
                    ) {
                        is ClerkResult.Failure -> {
                            _uiState.update { state ->
                                state.copy(
                                    isProcessing = false,
                                    errorMessage = verificationResult.longErrorMessageOrNull
                                        ?: "We couldn't send a verification code. Please try again."
                                )
                            }
                            return@onSuccess
                        }

                        is ClerkResult.Success -> {
                            _uiState.update { state ->
                                state.copy(
                                    isProcessing = false,
                                    verificationEmail = trimmedEmail
                                )
                            }
                        }
                    }
                }
                .onFailure { failure ->
                    _uiState.update { state ->
                        state.copy(
                            isProcessing = false,
                            errorMessage = failure.longErrorMessageOrNull
                                ?: "Unable to create your account. Please try again."
                        )
                    }
                }
        }
    }

    fun verifyCode(code: String) {
        val trimmedCode = code.trim()
        if (trimmedCode.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Enter the verification code sent to your email.")
            }
            return
        }

        val pendingSignUp = Clerk.signUp
        if (pendingSignUp == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "Your verification session expired. Please sign up again.",
                    verificationEmail = null
                )
            }
            return
        }

        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

        viewModelScope.launch {
            pendingSignUp
                .attemptVerification(SignUp.AttemptVerificationParams.EmailCode(trimmedCode))
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isProcessing = false,
                            verificationEmail = null
                        )
                    }
                }
                .onFailure { failure ->
                    _uiState.update { state ->
                        state.copy(
                            isProcessing = false,
                            errorMessage = failure.longErrorMessageOrNull
                                ?: "Verification failed. Double-check the code and try again."
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetVerification() {
        _uiState.update { it.copy(verificationEmail = null) }
    }

    fun signInWithGoogle() {
        if (!_uiState.value.isInitialized) {
            _uiState.update {
                it.copy(errorMessage = "Please wait for Clerk to initialize.")
            }
            return
        }

        _uiState.update {
            it.copy(
                isProcessing = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            SignIn
                .authenticateWithRedirect(
                    SignIn.AuthenticateWithRedirectParams.OAuth(
                        provider = OAuthProvider.GOOGLE
                    )
                )
                .onSuccess { result ->
                    // OAuth authentication successful
                    // The result contains either a SignIn or SignUp
                    _uiState.update { state ->
                        state.copy(isProcessing = false)
                    }
                }
                .onFailure { failure ->
                    _uiState.update { state ->
                        state.copy(
                            isProcessing = false,
                            errorMessage = failure.longErrorMessageOrNull
                                ?: "Unable to sign in with Google. Please try again."
                        )
                    }
                }
        }
    }

    private fun syncUserIfNeeded(user: User) {
        val userId = user.id
        if (syncInProgress) return
        if (userId == lastSyncedUserId) return

        viewModelScope.launch {
            syncInProgress = true
            try {
                fetchNeonAuthToken { authToken ->
                    NeonUserService.upsertUser(user, authToken)
                    lastSyncedUserId = userId
                }
            } catch (cancellation: CancellationException) {
                // Coroutine was cancelled (e.g., navigation or ViewModel cleared)
                // This is expected behavior, so just propagate it without logging
                throw cancellation
            } catch (error: Exception) {
                Log.e(TAG, "Failed to sync user ${user.id} with Neon.", error)
            } finally {
                syncInProgress = false
            }
        }
    }

    private suspend fun fetchNeonAuthToken(onSuccess: suspend (String) -> Unit) {
        // Check if coroutine is still active before proceeding
        if (!coroutineContext.isActive) return

        val fallbackToken = BuildConfig.NEON_API_KEY.takeUnless { it.isBlank() }
        val basicAuthConfigured =
            BuildConfig.NEON_DB_ROLE.isNotBlank() && BuildConfig.NEON_DB_PASSWORD.isNotBlank()
        val session = Clerk.session
        if (session == null) {
            Log.w(TAG, "Clerk session unavailable; cannot fetch Neon auth token.")
            if (fallbackToken != null) {
                onSuccess(fallbackToken)
            } else if (basicAuthConfigured) {
                onSuccess("")
            } else {
                Log.w(TAG, "Neon fallback API key missing or blank; sync skipped.")
            }
            return
        }

        val cachedSessionToken = session.lastActiveToken?.jwt?.takeUnless { it.isBlank() }
        if (cachedSessionToken != null) {
            onSuccess(cachedSessionToken)
            return
        }

        val clerkResult = try {
            session.fetchToken()
        } catch (cancellation: CancellationException) {
            if (fallbackToken != null) {
                Log.w(TAG, "Clerk session token fetch cancelled; using Neon API key fallback.")
                onSuccess(fallbackToken)
            } else if (basicAuthConfigured) {
                Log.w(TAG, "Clerk session token fetch cancelled; using Neon role/password fallback.")
                onSuccess("")
            } else {
                throw cancellation
            }
            return
        } catch (error: Exception) {
            Log.e(TAG, "Failed to fetch Clerk session token for Neon.", error)
            if (fallbackToken != null) {
                onSuccess(fallbackToken)
            } else if (basicAuthConfigured) {
                onSuccess("")
            } else {
                Log.w(TAG, "Neon fallback API key missing or blank; sync skipped.")
            }
            return
        }

        when (clerkResult) {
            is ClerkResult.Success -> {
                val jwt = clerkResult.value.jwt?.takeUnless { it.isBlank() }
                when {
                    jwt != null -> onSuccess(jwt)
                    fallbackToken != null -> onSuccess(fallbackToken)
                    basicAuthConfigured -> onSuccess("")
                    else -> Log.w(TAG, "Clerk returned an empty Neon auth token; sync skipped.")
                }
            }
            is ClerkResult.Failure -> {
                Log.e(
                    TAG,
                    clerkResult.longErrorMessageOrNull
                        ?: "Failed to fetch Clerk session token for Neon."
                )
                if (fallbackToken != null) {
                    onSuccess(fallbackToken)
                } else if (basicAuthConfigured) {
                    onSuccess("")
                } else {
                    Log.w(TAG, "Neon fallback API key missing or blank; sync skipped.")
                }
            }
        }
    }

    companion object {
        private const val TAG = "LoginViewModel"
    }
}

data class LoginUiState(
    val isInitialized: Boolean = false,
    val isProcessing: Boolean = false,
    val isSignedIn: Boolean = false,
    val verificationEmail: String? = null,
    val errorMessage: String? = null
) {
    val requiresVerification: Boolean
        get() = verificationEmail != null
}
