package com.phamnhantucode.aicareercoach.ui.accountsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.user.User
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class AccountSettingsUiState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = true,
    val isSignedIn: Boolean = false,
    val fullName: String? = null,
    val primaryEmail: String? = null,
    val profileImageUrl: String? = null,
    val connectedAccounts: List<ConnectedAccountUiState> = emptyList()
)

data class ConnectedAccountUiState(
    val providerName: String,
    val emailAddress: String?
)

class AccountSettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AccountSettingsUiState())
    val uiState = _uiState.asStateFlow()

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
                    isLoading = !isInitialized,
                    isSignedIn = user != null,
                    fullName = user?.let(::resolveDisplayName),
                    primaryEmail = user?.let(::resolvePrimaryEmail),
                    profileImageUrl = user?.imageUrl,
                    connectedAccounts = user?.verifiedExternalAccounts
                        ?.map(::resolveConnectedAccount)
                        .orEmpty()
                )
            }
        }.launchIn(viewModelScope)
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
