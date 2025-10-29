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
import com.phamnhantucode.aicareercoach.data.interview.InterviewPrepRepository
import com.phamnhantucode.aicareercoach.data.neon.NeonAuth
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

enum class LoginNavigationTarget {
    Industry,
    Onboarding,
}

class LoginViewModel(
    private val interviewPrepRepository: InterviewPrepRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private var lastSyncedUserId: String? = null
    private var neonAuthToken: String? = null
    private var neonTokenJob: Job? = null
    private var neonUserSyncJob: Job? = null
    private var postSignInCheckJob: Job? = null
    private var hasIssuedPostSignInNavigation = false
    private var questionPreloadJob: Job? = null

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
            if (user == null) {
                _uiState.update { current -> current.copy(navigationTarget = null) }
                lastSyncedUserId = null
                neonAuthToken = null
                neonTokenJob?.cancel()
                neonTokenJob = null
                neonUserSyncJob?.cancel()
                neonUserSyncJob = null
                hasIssuedPostSignInNavigation = false
            } else if (!hasIssuedPostSignInNavigation) {
                val state = _uiState.value
                if (!state.requiresVerification && state.navigationTarget == null) {
                    evaluatePostSignInNavigationAsync()
                }
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
                    _uiState.update { state ->
                        state.copy(
                            isProcessing = false
                        )
                    }
                    refreshNeonAuthTokenAsync()
                    evaluatePostSignInNavigationAsync()
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
                                verificationEmail = null,
                                navigationTarget = LoginNavigationTarget.Onboarding
                            )
                        }
                        hasIssuedPostSignInNavigation = true
                        syncNewClerkUserAsync()
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
                            verificationEmail = null,
                            navigationTarget = LoginNavigationTarget.Onboarding
                        )
                    }
                    hasIssuedPostSignInNavigation = true
                    syncNewClerkUserAsync()
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

    fun currentNeonAuthToken(): String? = neonAuthToken

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
                    _uiState.update { state ->
                        state.copy(
                            isProcessing = false
                        )
                    }
                    refreshNeonAuthTokenAsync()
                    evaluatePostSignInNavigationAsync()
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

    private fun refreshNeonAuthTokenAsync() {
        neonTokenJob?.cancel()
        neonTokenJob = viewModelScope.launch {
            neonAuthToken = try {
                NeonAuth.fetchNeonAuthToken()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Log.e(TAG, "Failed to refresh Neon auth token.", error)
                null
            }
        }
    }

    private fun syncNewClerkUserAsync(initialUser: User? = null) {
        neonUserSyncJob?.cancel()
        neonUserSyncJob = viewModelScope.launch {
            try {
                val user = initialUser ?: awaitClerkUser() ?: return@launch
                val userId = user.id
                if (userId == lastSyncedUserId) return@launch
                val authToken = NeonAuth.fetchNeonAuthToken() ?: return@launch
                NeonUserService.upsertUser(user, authToken)
                lastSyncedUserId = userId
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Log.e(TAG, "Failed to sync new Clerk user with Neon.", error)
            }
        }
    }

    private suspend fun awaitClerkUser(): User? {
        Clerk.user?.let { return it }
        return try {
            Clerk.userFlow.filterNotNull().first()
        } catch (cancellation: CancellationException) {
            throw cancellation
        }
    }


    fun consumeNavigationTarget() {
        _uiState.update { it.copy(navigationTarget = null) }
    }

    private fun evaluatePostSignInNavigationAsync() {
        postSignInCheckJob?.cancel()
        postSignInCheckJob = viewModelScope.launch {
            try {
                val user = Clerk.user
                if (user == null) return@launch
                val neonUser = com.phamnhantucode.aicareercoach.data.neon.NeonUserService.getUser(
                    clerkUserId = user.id,
                    authToken = NeonAuth.fetchNeonAuthToken()
                )
                val industry = neonUser?.industry?.trim()
                val needsOnboarding = industry.isNullOrBlank() || industry.equals("null", ignoreCase = true)
                _uiState.update { state ->
                    state.copy(
                        navigationTarget = if (needsOnboarding) LoginNavigationTarget.Onboarding else LoginNavigationTarget.Industry
                    )
                }
                hasIssuedPostSignInNavigation = true

                // Preload interview questions in background after successful user info fetch
                // This improves UX by having questions ready when user navigates to interview prep
                preloadInterviewQuestionsInBackground()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // If profile fetch fails, default to onboarding to be safe
                _uiState.update { state ->
                    state.copy(navigationTarget = LoginNavigationTarget.Onboarding)
                }
                hasIssuedPostSignInNavigation = true
            }
        }
    }

    /**
     * Preloads quiz and interview questions in the background after user login.
     * This runs independently and won't block navigation or show errors to the user.
     */
    private fun preloadInterviewQuestionsInBackground() {
        questionPreloadJob?.cancel()
        questionPreloadJob = viewModelScope.launch {
            try {
                interviewPrepRepository?.preloadQuestionPools()
                Log.d(TAG, "Successfully preloaded interview question pools in background")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                // Log but don't show error to user - this is a background optimization
                Log.w(TAG, "Failed to preload interview questions in background", error)
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
    val errorMessage: String? = null,
    val navigationTarget: LoginNavigationTarget? = null,
) {
    val requiresVerification: Boolean
        get() = verificationEmail != null
}
