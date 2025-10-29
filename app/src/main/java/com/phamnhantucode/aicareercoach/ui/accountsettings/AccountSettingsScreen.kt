package com.phamnhantucode.aicareercoach.ui.accountsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Observe sign-out success and navigate to login
    LaunchedEffect(viewModel) {
        viewModel.signOutSuccess.collectLatest {
            onLogout()
        }
    }

    AccountSettingsContent(
        uiState = uiState,
        onBack = onBack,
        onSignOut = viewModel::signOut,
        onThemeModeChange = viewModel::setThemeMode,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSettingsContent(
    uiState: AccountSettingsUiState,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onThemeModeChange: (com.phamnhantucode.aicareercoach.data.preferences.ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Account Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                AccountSettingsLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            !uiState.isSignedIn -> {
                AccountSettingsSignedOutState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileSection(uiState)
                    ConnectedAccountsSection(uiState.connectedAccounts)
                    AppSettingsSection(
                        isSigningOut = uiState.isSigningOut,
                        signOutError = uiState.signOutError,
                        themeMode = uiState.themeMode,
                        onSignOut = onSignOut,
                        onThemeModeChange = onThemeModeChange
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSettingsLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AccountSettingsSignedOutState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "You're signed out",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sign in to manage your profile and connected accounts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileSection(uiState: AccountSettingsUiState) {
    val context = LocalContext.current
    val displayName = uiState.fullName?.takeUnless { it.isBlank() } ?: "Signed-in user"
    val email = uiState.primaryEmail ?: "Email not available"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!uiState.profileImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uiState.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile image",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "User avatar",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectedAccountsSection(connectedAccounts: List<ConnectedAccountUiState>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Connected Accounts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (connectedAccounts.isEmpty()) {
                Text(
                    text = "No connected accounts yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    connectedAccounts.forEach { account ->
                        val providerName = account.providerName.ifBlank { "External account" }
                        Column {
                            Text(
                                text = providerName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            account.emailAddress?.let { emailAddress ->
                                Text(
                                    text = emailAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSettingsSection(
    isSigningOut: Boolean,
    signOutError: String?,
    themeMode: com.phamnhantucode.aicareercoach.data.preferences.ThemeMode,
    onSignOut: () -> Unit,
    onThemeModeChange: (com.phamnhantucode.aicareercoach.data.preferences.ThemeMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "App Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            ThemeModeSelector(
                selectedTheme = themeMode,
                onThemeSelected = onThemeModeChange
            )
            Spacer(modifier = Modifier.height(16.dp))
            LanguageSelector()
            Spacer(modifier = Modifier.height(16.dp))
            FontSelector()
            Spacer(modifier = Modifier.height(24.dp))
            if (!signOutError.isNullOrBlank()) {
                Text(
                    text = signOutError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Button(
                onClick = onSignOut,
                enabled = !isSigningOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSigningOut) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(text = "Sign out")
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selectedTheme: com.phamnhantucode.aicareercoach.data.preferences.ThemeMode,
    onThemeSelected: (com.phamnhantucode.aicareercoach.data.preferences.ThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Theme Mode")
        Box {
            Text(
                text = when (selectedTheme) {
                    com.phamnhantucode.aicareercoach.data.preferences.ThemeMode.LIGHT -> "Light"
                    com.phamnhantucode.aicareercoach.data.preferences.ThemeMode.DARK -> "Dark"
                    com.phamnhantucode.aicareercoach.data.preferences.ThemeMode.SYSTEM -> "System"
                },
                modifier = Modifier.clickable { expanded = true }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Light") },
                    onClick = {
                        onThemeSelected(com.phamnhantucode.aicareercoach.data.preferences.ThemeMode.LIGHT)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Dark") },
                    onClick = {
                        onThemeSelected(com.phamnhantucode.aicareercoach.data.preferences.ThemeMode.DARK)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("System") },
                    onClick = {
                        onThemeSelected(com.phamnhantucode.aicareercoach.data.preferences.ThemeMode.SYSTEM)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageSelector() {
    var expanded by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("English") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Language")
        Box {
            Text(
                text = selectedLanguage,
                modifier = Modifier.clickable { expanded = true }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("English") },
                    onClick = {
                        selectedLanguage = "English"
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Spanish") },
                    onClick = {
                        selectedLanguage = "Spanish"
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FontSelector() {
    var expanded by remember { mutableStateOf(false) }
    var selectedFont by remember { mutableStateOf("Default") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Font")
        Box {
            Text(
                text = selectedFont,
                modifier = Modifier.clickable { expanded = true }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Default") },
                    onClick = {
                        selectedFont = "Default"
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Serif") },
                    onClick = {
                        selectedFont = "Serif"
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountSettingsScreenPreview() {
    AppTheme {
        AccountSettingsContent(
            uiState = AccountSettingsUiState(
                isInitialized = true,
                isLoading = false,
                isSignedIn = true,
                fullName = "Ada Lovelace",
                primaryEmail = "ada.lovelace@example.com",
                profileImageUrl = null,
                connectedAccounts = listOf(
                    ConnectedAccountUiState(
                        providerName = "Google",
                        emailAddress = "ada@gmail.com"
                    ),
                    ConnectedAccountUiState(
                        providerName = "GitHub",
                        emailAddress = "ada@github.com"
                    )
                ),
                themeMode = com.phamnhantucode.aicareercoach.data.preferences.ThemeMode.SYSTEM
            ),
            onBack = {},
            onSignOut = {},
            onThemeModeChange = {}
        )
    }
}
