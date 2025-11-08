package com.phamnhantucode.aicareercoach.ui.accountsettings

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.user.User
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.session.fetchToken
import com.phamnhantucode.aicareercoach.data.clerk.ClerkUserUpdateService
import com.phamnhantucode.aicareercoach.data.neon.NeonAuth
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService.UserProfileUpdate
import com.phamnhantucode.aicareercoach.data.preferences.PreferencesRepository
import com.phamnhantucode.aicareercoach.data.preferences.ThemeMode
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val editProfileState: EditProfileUiState = EditProfileUiState()
)

data class ConnectedAccountUiState(
    val providerName: String,
    val emailAddress: String?
)

class AccountSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository.getInstance(application)
    private val clerkUserUpdateService = ClerkUserUpdateService(application)

    private val _uiState = MutableStateFlow(AccountSettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _signOutSuccess = MutableSharedFlow<Unit>()
    val signOutSuccess: SharedFlow<Unit> = _signOutSuccess.asSharedFlow()

    // Cache the last saved profile data to use when reopening the dialog
    // This ensures we show the latest data even if Clerk hasn't synced yet
    private var cachedProfileData: EditProfileFormData? = null

    companion object {
        private const val TAG = "AccountSettingsViewModel"
    }

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
                    _signOutSuccess.emit(Unit)
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

    // Profile editing functions

    fun openEditProfileDialog() {
        viewModelScope.launch {
            val user = Clerk.user
            if (user == null) {
                Log.w(TAG, "Cannot edit profile: user not signed in")
                return@launch
            }

            // If we have cached data from a recent save, use it
            // Otherwise fetch from server
            val formData = if (cachedProfileData != null) {
                Log.d(TAG, "Using cached profile data")
                cachedProfileData!!
            } else {
                // Fetch Neon profile data with auth token
                val neonUser = try {
                    val authToken = NeonAuth.fetchNeonAuthToken()
                    if (authToken != null) {
                        NeonUserService.getUser(
                            clerkUserId = user.id,
                            authToken = authToken
                        )
                    } else {
                        Log.w(TAG, "No Neon auth token available")
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching Neon user profile", e)
                    null
                }

                EditProfileFormData(
                    firstName = user.firstName.orEmpty(),
                    lastName = user.lastName.orEmpty(),
                    imageUri = user.imageUrl,
                    industry = neonUser?.industry,
                    experienceYears = neonUser?.experienceYears?.toString().orEmpty(),
                    skills = neonUser?.skills?.joinToString(", ").orEmpty(),
                    bio = neonUser?.bio
                )
            }

            _uiState.update { currentState ->
                currentState.copy(
                    editProfileState = EditProfileUiState(
                        isOpen = true,
                        formData = formData,
                        originalData = formData
                    )
                )
            }
        }
    }

    fun updateEditProfileFormData(formData: EditProfileFormData) {
        _uiState.update { currentState ->
            currentState.copy(
                editProfileState = currentState.editProfileState.copy(
                    formData = formData,
                    error = null // Clear error when user makes changes
                )
            )
        }
    }

    fun cancelEditProfile() {
        // Don't clear cache on cancel - user might want to reopen with same data
        _uiState.update { currentState ->
            currentState.copy(
                editProfileState = EditProfileUiState()
            )
        }
    }

    fun saveProfileChanges() {
        val currentFormData = _uiState.value.editProfileState.formData

        // Validate form data
        val validationResult = currentFormData.validate()
        if (validationResult is ValidationResult.Invalid) {
            _uiState.update { currentState ->
                currentState.copy(
                    editProfileState = currentState.editProfileState.copy(
                        error = validationResult.errors.joinToString("\n")
                    )
                )
            }
            return
        }

        val user = Clerk.user
        if (user == null) {
            _uiState.update { currentState ->
                currentState.copy(
                    editProfileState = currentState.editProfileState.copy(
                        error = "User not signed in"
                    )
                )
            }
            return
        }

        _uiState.update { currentState ->
            currentState.copy(
                editProfileState = currentState.editProfileState.copy(
                    isSaving = true,
                    error = null
                )
            )
        }

        viewModelScope.launch {
            try {
                // Step 1: Update Clerk user data (name and image)
                val nameChanged = currentFormData.firstName != user.firstName ||
                        currentFormData.lastName != user.lastName
                val imageChanged = currentFormData.imageUri != user.imageUrl &&
                        currentFormData.imageUri != null &&
                        currentFormData.imageUri.startsWith("content://") // Local URI

                if (nameChanged) {
                    Log.d(TAG, "Updating user name in Clerk")
                    val nameResult = clerkUserUpdateService.updateUserName(
                        firstName = currentFormData.firstName,
                        lastName = currentFormData.lastName
                    )

                    if (nameResult is ClerkUserUpdateService.UpdateResult.Error) {
                        _uiState.update { currentState ->
                            currentState.copy(
                                editProfileState = currentState.editProfileState.copy(
                                    isSaving = false,
                                    error = "Failed to update name: ${nameResult.message}"
                                )
                            )
                        }
                        return@launch
                    }
                }

                if (imageChanged) {
                    Log.d(TAG, "Updating profile image in Clerk")
                    val imageUri = Uri.parse(currentFormData.imageUri)
                    val imageResult = clerkUserUpdateService.updateProfileImage(imageUri)

                    if (imageResult is ClerkUserUpdateService.UpdateResult.Error) {
                        _uiState.update { currentState ->
                            currentState.copy(
                                editProfileState = currentState.editProfileState.copy(
                                    isSaving = false,
                                    error = "Failed to update image: ${imageResult.message}"
                                )
                            )
                        }
                        return@launch
                    }
                }

                // Step 2: Update Neon user profile
                Log.d(TAG, "Updating user profile in Neon")
                val neonProfile = UserProfileUpdate(
                    industry = currentFormData.industry,
                    experienceYears = currentFormData.getExperienceYearsInt(),
                    skills = currentFormData.getSkillsList(),
                    bio = currentFormData.bio
                )

                // Get auth token for Neon
                val neonAuthToken = NeonAuth.fetchNeonAuthToken()
                if (neonAuthToken == null) {
                    Log.w(TAG, "No Neon auth token available, skipping Neon update")
                } else {
                    NeonUserService.updateUserProfile(
                        clerkUserId = user.id,
                        profile = neonProfile,
                        authToken = neonAuthToken
                    )
                }

                // Success - cache the saved data and update UI
                Log.d(TAG, "Profile updated successfully")

                // Cache the saved form data for next time dialog is opened
                cachedProfileData = currentFormData.copy()

                // Manually update UI state with new values immediately
                _uiState.update { currentState ->
                    currentState.copy(
                        fullName = "${currentFormData.firstName} ${currentFormData.lastName}".trim(),
                        profileImageUrl = if (currentFormData.imageUri?.startsWith("content://") == true) {
                            // For local URIs, keep the old URL until Clerk updates
                            currentState.profileImageUrl
                        } else {
                            currentFormData.imageUri
                        },
                        editProfileState = EditProfileUiState(
                            isOpen = false,
                            saveSuccess = true
                        )
                    )
                }

                // Trigger Clerk to refresh user data in the background
                // The userFlow will update when Clerk fetches the latest data
                try {
                    val session = Clerk.session
                    if (session != null) {
                        // Fetching a new token often triggers user data refresh
                        session.fetchToken()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not refresh Clerk session", e)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile changes", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        editProfileState = currentState.editProfileState.copy(
                            isSaving = false,
                            error = "Error: ${e.message ?: "Unknown error occurred"}"
                        )
                    )
                }
            }
        }
    }
}
