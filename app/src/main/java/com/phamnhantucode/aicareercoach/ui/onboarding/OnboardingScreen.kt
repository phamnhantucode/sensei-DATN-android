package com.phamnhantucode.aicareercoach.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.data.industry.IndustryInsightsRepository
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService
import com.phamnhantucode.aicareercoach.ui.theme.AppTheme
import com.phamnhantucode.aicareercoach.utils.IndustryFormatUtils
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var isDarkMode by remember { mutableStateOf(true) }
    val industries = remember { IndustriesData.industries }
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
                 OnboardingContent(
                     isSubmitting = isSubmitting,
                     submitError = submitError,
                     loadingMessage = loadingMessage,
                     onComplete = { formData ->
                         scope.launch {
                             isSubmitting = true
                             submitError = null
                             loadingMessage = null
                             try {
                                 val selectedIndustry = industries.find { it.id == formData.industryId }
                                 val user = com.clerk.api.Clerk.user
                                 if (selectedIndustry == null) {
                                     submitError = "Please select an industry."
                                 } else if (user == null) {
                                     submitError = "User session unavailable. Please sign in again."
                                 } else if (formData.subIndustry.isBlank()) {
                                     submitError = "Please select a specialization."
                                 } else {
                                     // Format industry data for API (industryId---sub-industry-kebab)
                                     val formattedIndustry = IndustryFormatUtils.formatIndustryForApi(
                                         industryId = selectedIndustry.id,
                                         subIndustry = formData.subIndustry
                                     )

                                     // Step 1: Get auth token
                                     loadingMessage = "Setting up your account..."
                                     val authToken = com.phamnhantucode.aicareercoach.data.neon.NeonAuth.fetchNeonAuthToken()
                                         ?: throw IllegalStateException("Unable to fetch authentication token.")

                                     // Step 2: Ensure IndustryInsight exists BEFORE creating user
                                     loadingMessage = "Generating industry insights..."
                                     val authorizationHeader = "Bearer $authToken"
                                     val repository = IndustryInsightsRepository()
                                     repository.ensureIndustryInsightExists(
                                         industry = formattedIndustry,
                                         authorizationHeader = authorizationHeader,
                                     )

                                     // Step 3: Create/update user (ensure exists)
                                     loadingMessage = "Creating your profile..."
                                     val email = user.emailAddresses.firstOrNull()?.emailAddress ?: throw IllegalStateException("User email not found")
                                     NeonUserService.syncUser(user.id, email)

                                     // Step 4: Update additional user profile fields
                                     loadingMessage = "Saving your details..."
                                     val skills = formData.skills.split(',')
                                         .map { it.trim() }
                                         .filter { it.isNotEmpty() }
                                     val experienceYears = formData.experienceYears.trim().toIntOrNull()
                                     val profile = NeonUserService.UserProfileUpdate(
                                         industry = formattedIndustry,
                                         experienceYears = experienceYears,
                                         skills = skills,
                                         bio = formData.bio.takeIf { it.isNotBlank() },
                                     )

                                     NeonUserService.updateUserProfile(
                                         clerkUserId = user.id,
                                         update = profile
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
                 )
            }
        }
    }
}
