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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
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

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText

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
            LoginCard(
                onBack = onBack,
                uiState = uiState,
                onSignIn = viewModel::signIn,
                onSignUp = viewModel::signUp,
                onSignInWithGoogle = viewModel::signInWithGoogle,
                onVerify = viewModel::verifyCode,
                onClearError = viewModel::clearError,
                onResetVerification = viewModel::resetVerification,
                startForgotPassword = viewModel::startForgotPassword,
                onInitiatePasswordReset = viewModel::initiatePasswordReset,
                onCompletePasswordReset = viewModel::completePasswordReset,
                onCancelForgotPassword = viewModel::cancelForgotPassword
            )
        }
    }
}

@Composable
private fun LoginCard(
    onBack: () -> Unit,
    uiState: LoginUiState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String, String) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onVerify: (String) -> Unit,
    onClearError: () -> Unit,
    onResetVerification: () -> Unit,
    startForgotPassword: () -> Unit,
    onInitiatePasswordReset: (String) -> Unit,
    onCompletePasswordReset: (String, String) -> Unit,
    onCancelForgotPassword: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var verificationCode by rememberSaveable { mutableStateOf("") }
    var authMode by rememberSaveable { mutableStateOf(AuthMode.SignIn) } // Local UI state for Sign In vs Sign Up

    LaunchedEffect(uiState.requiresVerification) {
        if (uiState.requiresVerification && !uiState.isResettingPassword) {
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
                        uiState.isResettingPassword -> "Reset password"
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
                        uiState.isResettingPassword ->
                             if (uiState.resetPasswordStep == ResetPasswordStep.Request)
                                 "Enter your email to receive a reset code."
                             else
                                 "Enter the code and your new password."
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

            if (uiState.isResettingPassword) {
                if (uiState.resetPasswordStep == ResetPasswordStep.Request) {
                     ForgotPasswordSection(
                        email = email,
                        onEmailChange = {
                            email = it
                            if (uiState.errorMessage != null) onClearError()
                        },
                        onBack = onCancelForgotPassword,
                        isProcessing = uiState.isProcessing,
                        isCheckingAutoLogin = uiState.isCheckingAutoLogin
                     )
                } else {
                     ResetPasswordSection(
                        code = verificationCode,
                        onCodeChange = {
                             verificationCode = it
                             if (uiState.errorMessage != null) onClearError()
                        },
                        password = password,
                        onPasswordChange = {
                             password = it
                             if (uiState.errorMessage != null) onClearError()
                        },
                        onBack = onCancelForgotPassword,
                        isProcessing = uiState.isProcessing,
                        isCheckingAutoLogin = uiState.isCheckingAutoLogin
                     )
                }
            } else if (uiState.requiresVerification) {
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
                    isProcessing = uiState.isProcessing,
                    isCheckingAutoLogin = uiState.isCheckingAutoLogin
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
                    firstName = firstName,
                    onFirstNameChange = {
                        firstName = it
                        if (uiState.errorMessage != null) onClearError()
                    },
                    lastName = lastName,
                    onLastNameChange = {
                        lastName = it
                        if (uiState.errorMessage != null) onClearError()
                    },
                    isSignUp = authMode == AuthMode.SignUp,
                    onForgotPassword = startForgotPassword,
                    isProcessing = uiState.isProcessing,
                    isCheckingAutoLogin = uiState.isCheckingAutoLogin
                )
            }

            val primaryButtonLabel = when {
                uiState.isCheckingAutoLogin -> "Signing you in..."
                uiState.isResettingPassword -> if (uiState.resetPasswordStep == ResetPasswordStep.Request) "Send reset code" else "Reset password"
                uiState.requiresVerification -> "Verify code"
                authMode == AuthMode.SignUp -> "Create account"
                else -> "Sign in"
            }

            val isValidEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            val primaryEnabled = when {
                uiState.isCheckingAutoLogin -> false
                uiState.isResettingPassword -> if (uiState.resetPasswordStep == ResetPasswordStep.Request) email.isNotBlank() && isValidEmail else verificationCode.isNotBlank() && password.isNotBlank()
                uiState.requiresVerification -> verificationCode.isNotBlank()
                else -> email.isNotBlank() && isValidEmail && password.isNotBlank()
            } && uiState.isInitialized && !uiState.isProcessing

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    when {
                        uiState.isResettingPassword -> {
                            if (uiState.resetPasswordStep == ResetPasswordStep.Request)
                                onInitiatePasswordReset(email)
                            else
                                onCompletePasswordReset(verificationCode, password)
                        }
                        uiState.requiresVerification -> onVerify(verificationCode)
                        authMode == AuthMode.SignIn -> onSignIn(email, password)
                        else -> onSignUp(email, password, firstName, lastName)
                    }
                },
                enabled = primaryEnabled
            ) {
                if (uiState.isProcessing || uiState.isCheckingAutoLogin) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = primaryButtonLabel)
                }
            }

            if (!uiState.requiresVerification && !uiState.isResettingPassword) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        authMode = when (authMode) {
                            AuthMode.SignIn -> AuthMode.SignUp
                            AuthMode.SignUp -> AuthMode.SignIn
                        }
                        onClearError()
                    },
                    enabled = !uiState.isCheckingAutoLogin
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
                    enabled = uiState.isInitialized && !uiState.isProcessing && !uiState.isCheckingAutoLogin,
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
                val annotatedString = buildAnnotatedString {
                    append("By continuing you agree to our ")
                    pushStringAnnotation(tag = "TERMS", annotation = "terms")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                        append("Terms of Service")
                    }
                    pop()
                    append(" and ")
                    pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                        append("Privacy Policy")
                    }
                    pop()
                    append(".")
                }
                
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    ),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "TERMS", start = offset, end = offset).firstOrNull()?.let {
                            // TODO: Open Terms URL
                        }
                        annotatedString.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset).firstOrNull()?.let {
                            // TODO: Open Privacy URL
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
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
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    isSignUp: Boolean,
    onForgotPassword: () -> Unit,
    isProcessing: Boolean,
    isCheckingAutoLogin: Boolean
) {
    val focusManager = LocalFocusManager.current
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isSignUp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "First name (optional)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = firstName,
                        onValueChange = onFirstNameChange,
                        placeholder = { Text(text = "John") },
                        singleLine = true,
                        enabled = !isProcessing && !isCheckingAutoLogin,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Last name (optional)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = lastName,
                        onValueChange = onLastNameChange,
                        placeholder = { Text(text = "Doe") },
                        singleLine = true,
                        enabled = !isProcessing && !isCheckingAutoLogin,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )
                }
            }
        }
        
        LabeledField(
            value = email,
            onValueChange = onEmailChange,
            label = "Work email",
            placeholder = "you@company.com",
            icon = Icons.Outlined.AlternateEmail,
            enabled = !isProcessing && !isCheckingAutoLogin,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                // Default behavior is to move focus to the next field, which is what we want.
                // Explicitly defining it can sometimes interfere if not done correctly (e.g. focusManager.moveFocus).
                // Leaving empty/default allows the system to find the next focusable (Password field).
            )
        )
        LabeledField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            placeholder = "••••••••",
            icon = Icons.Outlined.Lock,
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
            enabled = !isProcessing && !isCheckingAutoLogin,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )
        if (!isSignUp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onForgotPassword,
                    enabled = !isProcessing && !isCheckingAutoLogin
                ) {
                    Text(text = "Forgot password?")
                }
            }
        }
    }
}

@Composable
private fun VerificationSection(
    verificationCode: String,
    onVerificationCodeChange: (String) -> Unit,
    onUseDifferentEmail: () -> Unit,
    isProcessing: Boolean,
    isCheckingAutoLogin: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = verificationCode,
            onValueChange = onVerificationCodeChange,
            label = { Text("Verification code") },
            placeholder = { Text("123456") },
            singleLine = true,
            enabled = !isProcessing && !isCheckingAutoLogin
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onUseDifferentEmail, enabled = !isProcessing && !isCheckingAutoLogin) {
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
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityToggle: () -> Unit = {},
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
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
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !isPasswordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )
    }
}
@Composable
private fun ForgotPasswordSection(
    email: String,
    onEmailChange: (String) -> Unit,
    onBack: () -> Unit,
    isProcessing: Boolean,
    isCheckingAutoLogin: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LabeledField(
            value = email,
            onValueChange = onEmailChange,
            label = "Work email",
            placeholder = "you@company.com",
            icon = Icons.Outlined.AlternateEmail,
            enabled = !isProcessing && !isCheckingAutoLogin,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            )
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack, enabled = !isProcessing && !isCheckingAutoLogin) {
                Text(text = "← Back")
            }
        }
    }
}

@Composable
private fun ResetPasswordSection(
    code: String,
    onCodeChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    isProcessing: Boolean,
    isCheckingAutoLogin: Boolean
) {
    val focusManager = LocalFocusManager.current
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = code,
            onValueChange = onCodeChange,
            label = { Text("Reset code") },
            placeholder = { Text("123456") },
            singleLine = true,
            enabled = !isProcessing && !isCheckingAutoLogin,
             keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
        LabeledField(
            value = password,
            onValueChange = onPasswordChange,
            label = "New Password",
            placeholder = "••••••••",
            icon = Icons.Outlined.Lock,
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
            enabled = !isProcessing && !isCheckingAutoLogin,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )
         Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack, enabled = !isProcessing && !isCheckingAutoLogin) {
                Text(text = "← Back")
            }
        }
    }
}
