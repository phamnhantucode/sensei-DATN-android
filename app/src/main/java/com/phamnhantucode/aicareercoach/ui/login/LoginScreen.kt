package com.phamnhantucode.aicareercoach.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.phamnhantucode.aicareercoach.data.interview.InterviewPrepRepository
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme

private enum class AuthMode {
    SignIn,
    SignUp
}

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: LoginViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val interviewPrepRepository = InterviewPrepRepository(context = context.applicationContext)
                return LoginViewModel(interviewPrepRepository = interviewPrepRepository) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.navigationTarget) {
        when (uiState.navigationTarget) {
            LoginNavigationTarget.Onboarding -> {
                onNavigateToOnboarding()
                viewModel.consumeNavigationTarget()
            }
            LoginNavigationTarget.Industry -> {
                onSignedIn()
                viewModel.consumeNavigationTarget()
            }
            null -> Unit
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isCheckingAutoLogin) {
                // Show loading indicator for auto-login
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Signing you in...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                LoginCard(
                    onBack = onBack,
                    uiState = uiState,
                    onSignIn = viewModel::signIn,
                    onSignUp = viewModel::signUp,
                    onSignInWithGoogle = viewModel::signInWithGoogle,
                    onVerify = viewModel::verifyCode,
                    onClearError = viewModel::clearError,
                    onResetVerification = viewModel::resetVerification
                )
            }
        }
    }
}

@Composable
private fun LoginCard(
    onBack: () -> Unit,
    uiState: LoginUiState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onVerify: (String) -> Unit,
    onClearError: () -> Unit,
    onResetVerification: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var authMode by remember { mutableStateOf(AuthMode.SignIn) }

    LaunchedEffect(uiState.requiresVerification) {
        if (uiState.requiresVerification) {
            authMode = AuthMode.SignUp
            verificationCode = ""
        }
    }

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = when {
                        uiState.requiresVerification -> "Verify your email"
                        authMode == AuthMode.SignUp -> "Create your account"
                        else -> "Welcome back"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = when {
                        uiState.requiresVerification ->
                            "Enter the code we sent to ${uiState.verificationEmail}."
                        authMode == AuthMode.SignUp ->
                            "Start your AI-powered career journey in just a moment."
                        else ->
                            "Sign in to continue your AI-powered career journey."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                AnimatedVisibility(
                    visible = !uiState.isInitialized,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "Connecting to Clerk…",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }

            if (uiState.requiresVerification) {
                VerificationSection(
                    verificationCode = verificationCode,
                    onVerificationCodeChange = {
                        verificationCode = it
                        if (uiState.errorMessage != null) onClearError()
                    },
                    onUseDifferentEmail = {
                        onResetVerification()
                        onClearError()
                        verificationCode = ""
                        authMode = AuthMode.SignUp
                    },
                    onBack = onBack,
                    isProcessing = uiState.isProcessing
                )
            } else {
                CredentialsSection(
                    email = email,
                    onEmailChange = {
                        email = it
                        if (uiState.errorMessage != null) onClearError()
                    },
                    password = password,
                    onPasswordChange = {
                        password = it
                        if (uiState.errorMessage != null) onClearError()
                    },
                    onBack = onBack,
                    isProcessing = uiState.isProcessing
                )
            }

            val primaryButtonLabel = when {
                uiState.requiresVerification -> "Verify code"
                authMode == AuthMode.SignUp -> "Create account"
                else -> "Sign in"
            }

            val primaryEnabled = when {
                uiState.requiresVerification -> verificationCode.isNotBlank()
                else -> email.isNotBlank() && password.isNotBlank()
            } && uiState.isInitialized && !uiState.isProcessing

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    when {
                        uiState.requiresVerification -> onVerify(verificationCode)
                        authMode == AuthMode.SignIn -> onSignIn(email, password)
                        else -> onSignUp(email, password)
                    }
                },
                enabled = primaryEnabled
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = primaryButtonLabel)
                }
            }

            if (!uiState.requiresVerification) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        authMode = when (authMode) {
                            AuthMode.SignIn -> AuthMode.SignUp
                            AuthMode.SignUp -> AuthMode.SignIn
                        }
                        onClearError()
                    }
                ) {
                    Text(
                        text = when (authMode) {
                            AuthMode.SignIn -> "Create an account"
                            AuthMode.SignUp -> "Already have an account? Sign in"
                        }
                    )
                }

                DividerWithLabel(label = "or")

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSignInWithGoogle,
                    enabled = uiState.isInitialized && !uiState.isProcessing,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AlternateEmail,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Continue with Google")
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DividerWithLabel(label = "securely powered by Clerk")
                Text(
                    text = "By continuing you agree to our Terms of Service and Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CredentialsSection(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    isProcessing: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LabeledField(
            value = email,
            onValueChange = onEmailChange,
            label = "Work email",
            placeholder = "you@company.com",
            icon = Icons.Outlined.AlternateEmail,
            enabled = !isProcessing
        )
        LabeledField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            placeholder = "••••••••",
            icon = Icons.Outlined.Lock,
            isPassword = true,
            enabled = !isProcessing
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, enabled = !isProcessing) {
                Text(text = "← Back")
            }
            TextButton(
                onClick = { /* TODO: integrate forgot password */ },
                enabled = !isProcessing
            ) {
                Text(text = "Forgot password?")
            }
        }
    }
}

@Composable
private fun VerificationSection(
    verificationCode: String,
    onVerificationCodeChange: (String) -> Unit,
    onUseDifferentEmail: () -> Unit,
    onBack: () -> Unit,
    isProcessing: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = verificationCode,
            onValueChange = onVerificationCodeChange,
            label = { Text("Verification code") },
            placeholder = { Text("123456") },
            singleLine = true,
            enabled = !isProcessing
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, enabled = !isProcessing) {
                Text(text = "← Back")
            }
            TextButton(onClick = onUseDifferentEmail, enabled = !isProcessing) {
                Text(text = "Use a different email")
            }
        }
    }
}

@Composable
private fun DividerWithLabel(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider(modifier = Modifier.weight(1f))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Divider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LabeledField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    enabled: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder) },
            leadingIcon = {
                Icon(imageVector = icon, contentDescription = null)
            },
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            singleLine = true,
            enabled = enabled
        )
    }
}
