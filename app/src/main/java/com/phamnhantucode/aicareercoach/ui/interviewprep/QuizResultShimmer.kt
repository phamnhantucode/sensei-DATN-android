package com.phamnhantucode.aicareercoach.ui.interviewprep

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.ui.components.InsetAwareColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultShimmer(
    onBackToHome: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "shimmer"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        start = Offset(translateAnim - 500f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(24.dp)
                        .background(shimmerBrush, RoundedCornerShape(4.dp))
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackToHome) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        InsetAwareColumn(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Score Card Shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(shimmerBrush, RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Improvement Tip Shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(shimmerBrush, RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Review Answers Header
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(28.dp)
                    .background(shimmerBrush, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Questions Shimmer
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(shimmerBrush, RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
