package com.phamnhantucode.aicareercoach.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.data.industry.IndustryInsightsRepository
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService
import com.phamnhantucode.aicareercoach.ui.components.InsetAwareColumn
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme
import kotlinx.coroutines.launch

data class FormData(
    val industryId: String = "",
    val subIndustry: String = "",
    val experienceYears: String = "",
    val skills: String = "",
    val bio: String = "",
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var isDarkMode by remember { mutableStateOf(true) }
    var currentStep by remember { mutableStateOf(0) }
    var formData by remember { mutableStateOf(FormData()) }
    val industries = remember { IndustriesData.industries }
    val totalSteps = 3
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var loadingMessage by remember { mutableStateOf<String?>(null) }

    AppTheme(darkTheme = isDarkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                InsetAwareColumn(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { isDarkMode = !isDarkMode },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle theme",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Header
                    Text(
                        text = "AI Career Coach",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Welcome! Let's get started with your profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Progress Indicator
                    ProgressSection(
                        currentStep = currentStep,
                        totalSteps = totalSteps
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Main Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            when (currentStep) {
                                0 -> IndustrySelectionStep(
                                    industries = industries,
                                    formData = formData,
                                    onDataChange = { formData = it }
                                )

                                1 -> SpecializationExperienceStep(
                                    industries = industries,
                                    formData = formData,
                                    onDataChange = { formData = it }
                                )

                                2 -> ProfileDetailsStep(
                                    formData = formData,
                                    onDataChange = { formData = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            submitError?.let { errorMsg ->
                                Text(
                                    text = errorMsg,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Navigation Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (currentStep > 0) {
                                    OutlinedButton(
                                        onClick = { currentStep-- },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Back")
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (currentStep < totalSteps - 1) {
                                            currentStep++
                                        } else if (!isSubmitting) {
                                            submitError = null
                                            loadingMessage = null
                                            isSubmitting = true
                                            scope.launch {
                                                try {
                                                    val selectedIndustry = industries.find { it.id == formData.industryId }
                                                    val user = com.clerk.api.Clerk.user
                                                    if (selectedIndustry == null) {
                                                        submitError = "Please select an industry."
                                                    } else if (user == null) {
                                                        submitError = "User session unavailable. Please sign in again."
                                                    } else {
                                                        // Step 1: Get auth token
                                                        loadingMessage = "Setting up your account..."
                                                        val authToken = com.phamnhantucode.aicareercoach.data.neon.NeonAuth.fetchNeonAuthToken()
                                                            ?: throw IllegalStateException("Unable to fetch authentication token.")

                                                        // Step 2: Ensure IndustryInsight exists BEFORE creating user
                                                        loadingMessage = "Generating industry insights..."
                                                        val authorizationHeader = "Bearer $authToken"
                                                        val repository = IndustryInsightsRepository()
                                                        repository.ensureIndustryInsightExists(
                                                            industry = selectedIndustry.name,
                                                            authorizationHeader = authorizationHeader,
                                                        )

                                                        // Step 3: Create/update user with industry field
                                                        loadingMessage = "Creating your profile..."
                                                        NeonUserService.upsertUserWithIndustry(
                                                            user = user,
                                                            industry = selectedIndustry.name,
                                                            authToken = authToken
                                                        )

                                                        // Step 4: Update additional user profile fields
                                                        loadingMessage = "Saving your details..."
                                                        val skills = formData.skills.split(',')
                                                            .map { it.trim() }
                                                            .filter { it.isNotEmpty() }
                                                        val experienceYears = formData.experienceYears.trim().toIntOrNull()
                                                        val profile = NeonUserService.UserProfileUpdate(
                                                            industry = selectedIndustry.name,
                                                            experienceYears = experienceYears,
                                                            skills = skills,
                                                            bio = formData.bio.takeIf { it.isNotBlank() },
                                                        )

                                                        NeonUserService.updateUserProfile(
                                                            clerkUserId = user.id,
                                                            profile = profile,
                                                            authToken = authToken,
                                                        )
                                                        onComplete()
                                                    }
                                                } catch (e: Exception) {
                                                    submitError = e.localizedMessage ?: "Failed to save profile. Please try again."
                                                } finally {
                                                    isSubmitting = false
                                                    loadingMessage = null
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        if (isSubmitting) loadingMessage ?: "Saving..."
                                        else if (currentStep == totalSteps - 1) "Complete Profile"
                                        else "Continue"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Footer
                    Text(
                        text = "Your information is secure and will only be used to personalize your experience",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressSection(currentStep: Int, totalSteps: Int) {
    val progress = if (totalSteps == 0) 1f else {
        ((currentStep + 1).coerceAtMost(totalSteps)) / totalSteps.toFloat()
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "progress"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Step ${currentStep + 1} of $totalSteps",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
fun StepHeader(icon: @Composable () -> Unit, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp))
}

@Composable
fun IndustrySelectionStep(
    industries: List<Industry>,
    formData: FormData,
    onDataChange: (FormData) -> Unit,
) {
    val selectedIndustry = industries.find { it.id == formData.industryId }

    Column {
        StepHeader(
            icon = {
                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            title = "Industry Focus",
            subtitle = "Choose the industry that best represents your work"
        )

        IndustryDropdown(
            industries = industries,
            selectedIndustry = selectedIndustry,
            onIndustrySelected = { industry ->
                onDataChange(
                    formData.copy(
                        industryId = industry.id,
                        subIndustry = ""
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "We'll use your industry to tailor insights, salary ranges, and career resources.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SpecializationExperienceStep(
    industries: List<Industry>,
    formData: FormData,
    onDataChange: (FormData) -> Unit,
) {
    val selectedIndustry = industries.find { it.id == formData.industryId }

    Column {
        StepHeader(
            icon = {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            title = "Specialization & Experience",
            subtitle = "Help us understand your niche"
        )

        if (selectedIndustry == null) {
            AssistanceCard(
                message = "Select an industry first to see the relevant specializations."
            )
        } else {
            SpecializationDropdown(
                subIndustries = selectedIndustry.subIndustries,
                selectedValue = formData.subIndustry,
                onValueChange = { sub ->
                    onDataChange(formData.copy(subIndustry = sub))
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formData.experienceYears,
            onValueChange = { value ->
                onDataChange(formData.copy(experienceYears = value))
            },
            label = { Text("Years of Experience") },
            placeholder = { Text("Enter your total years of experience") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
fun ProfileDetailsStep(
    formData: FormData,
    onDataChange: (FormData) -> Unit,
) {
    Column {
        StepHeader(
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            title = "Skills & Bio",
            subtitle = "Share the highlights of your experience"
        )

        OutlinedTextField(
            value = formData.skills,
            onValueChange = { onDataChange(formData.copy(skills = it)) },
            label = { Text("Skills") },
            placeholder = { Text("e.g., Python, JavaScript, Project Management") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            minLines = 2,
            maxLines = 4
        )
        Text(
            text = "Separate multiple skills with commas to help us understand your strengths.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formData.bio,
            onValueChange = { onDataChange(formData.copy(bio = it)) },
            label = { Text("Professional Bio") },
            placeholder = { Text("Tell us about your background and what you're working toward...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            minLines = 5,
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(16.dp))

        AssistanceCard(
            message = "Tip: Highlight recent wins, leadership roles, or big projects so our AI can personalize better guidance."
        )
    }
}

@Composable
fun AssistanceCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "💡",
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndustryDropdown(
    industries: List<Industry>,
    selectedIndustry: Industry?,
    onIndustrySelected: (Industry) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedIndustry?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Industry *") },
            placeholder = { Text("Select an industry") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(8.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            industries.forEach { industry ->
                DropdownMenuItem(
                    text = { Text(industry.name) },
                    onClick = {
                        onIndustrySelected(industry)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecializationDropdown(
    subIndustries: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = if (selectedValue.isNotBlank()) selectedValue else "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Specialization") },
            placeholder = { Text("Select your specialization") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(8.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            subIndustries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
