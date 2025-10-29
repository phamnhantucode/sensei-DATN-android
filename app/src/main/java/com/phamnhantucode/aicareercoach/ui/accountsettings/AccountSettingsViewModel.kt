package com.phamnhantucode.aicareercoach.ui.accountsettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.user.User
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.phamnhantucode.aicareercoach.data.preferences.PreferencesRepository
import com.phamnhantucode.aicareercoach.data.preferences.ThemeMode
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountSettingsUiState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = true,
    val isSignedIn: Boolean = false,
    val isSigningOut: Boolean = false,
    val fullName: String? = null,
    val primaryEmail: String? = null,
    val profileImageUrl: String? = null,
    val connectedAccounts: List<ConnectedAccountUiState> = emptyList(),
    val signOutError: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

data class ConnectedAccountUiState(
    val providerName: String,
    val emailAddress: String?
)

class AccountSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository.getInstance(application)

    private val _uiState = MutableStateFlow(AccountSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Combine Clerk auth state with theme preference
        combine(
            Clerk.isInitialized,
            Clerk.userFlow,
            preferencesRepository.themeModeFlow
        ) { isInitialized, user, themeMode ->
            Triple(isInitialized, user, themeMode)
        }.onEach { (isInitialized, user, themeMode) ->
            _uiState.update { current ->
                current.copy(
                    isInitialized = isInitialized,
                    isLoading = !isInitialized,
                    isSignedIn = user != null,
                    fullName = user?.let(::resolveDisplayName),
                    primaryEmail = user?.let(::resolvePrimaryEmail),
                    profileImageUrl = user?.imageUrl,
                    connectedAccounts = user?.verifiedExternalAccounts
                        ?.map(::resolveConnectedAccount)
                        .orEmpty(),
                    themeMode = themeMode
                )
            }
        }.launchIn(viewModelScope)
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(themeMode)
        }
    }

    fun signOut() {
        val currentState = _uiState.value
        if (!currentState.isSignedIn || currentState.isSigningOut) return

        _uiState.update { it.copy(isSigningOut = true, signOutError = null) }

        viewModelScope.launch {
            Clerk
                .signOut()
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(isSigningOut = false, signOutError = null)
                    }
                }
                .onFailure { failure ->
                    _uiState.update { state ->
                        state.copy(
                            isSigningOut = false,
                            signOutError = failure.longErrorMessageOrNull
                                ?: "Unable to sign out. Please try again."
                        )
                    }
                }
        }
    }

    fun clearSignOutError() {
        _uiState.update { it.copy(signOutError = null) }
    }

    private fun resolveDisplayName(user: User): String {
        val first = user.firstName?.takeUnless { it.isBlank() }
        val last = user.lastName?.takeUnless { it.isBlank() }
        val combined = listOfNotNull(first, last).joinToString(" ").takeIf { it.isNotBlank() }
        return combined
            ?: user.username?.takeUnless { it.isBlank() }
            ?: resolvePrimaryEmail(user).orEmpty()
    }

    private fun resolvePrimaryEmail(user: User): String? {
        val primaryId = user.primaryEmailAddressId
        val primary = primaryId?.let { id ->
            user.emailAddresses.firstOrNull { it.id == id }
        }
        val fallback = primary ?: user.emailAddresses.firstOrNull()
        return fallback?.emailAddress
    }

    private fun resolveConnectedAccount(externalAccount: ExternalAccount): ConnectedAccountUiState {
        return ConnectedAccountUiState(
            providerName = externalAccount.provider.formatAsDisplayName(),
            emailAddress = externalAccount.emailAddress.takeUnless { it.isBlank() }
        )
    }

    private fun String.formatAsDisplayName(): String {
        if (isBlank()) return ""
        return replaceFirstChar { char ->
            if (char.isLowerCase()) {
                char.titlecase(Locale.getDefault())
            } else {
                char.toString()
            }
        }
    }
}
