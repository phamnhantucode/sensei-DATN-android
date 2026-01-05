package com.phamnhantucode.aicareercoach.ui.liveinterview

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.data.interview.InterviewSummary
import com.phamnhantucode.aicareercoach.data.interview.LiveQuestion
import kotlin.math.roundToInt

// Push-to-talk button
@Composable
fun PushToTalkButton(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .size(120.dp)
            .scale(if (isRecording) pulseScale else scale),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow effect when recording
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            )
        }

        // Main button
        Surface(
            modifier = Modifier
                .size(100.dp)
                .pointerInput(enabled) {
                    if (enabled) {
                        detectTapGestures(
                            onPress = {
                                onStartRecording()
                                tryAwaitRelease()
                                onStopRecording()
                            }
                        )
                    }
                },
            shape = CircleShape,
            color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (isRecording) "Recording" else "Hold to speak",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    if (enabled) {
        Text(
            text = if (isRecording) "Release to send" else "Hold to speak",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// Current question card
@Composable
fun QuestionCard(
    question: LiveQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question $questionNumber of $totalQuestions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Chip(
                    label = question.category,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = question.questionText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// AI feedback card
@Composable
fun FeedbackCard(
    transcription: String,
    expectedAnswer: String?,
    feedback: String,
    rating: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Answer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RatingDisplay(rating = rating)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            ) {
                Text(
                    text = transcription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!expectedAnswer.isNullOrBlank()) {
                Text(
                    text = "Expected Answer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = expectedAnswer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "AI Feedback",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            MarkdownText(
                text = feedback,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Markdown text renderer
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified
) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            var currentIndex = 0
            val boldPattern = Regex("\\*\\*(.+?)\\*\\*")
            val italicPattern = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
            
            // Find all bold matches first
            val boldMatches = boldPattern.findAll(text).toList()
            val italicMatches = italicPattern.findAll(text).filter { italicMatch ->
                // Exclude italic matches that overlap with bold matches
                boldMatches.none { boldMatch ->
                    italicMatch.range.first >= boldMatch.range.first && 
                    italicMatch.range.last <= boldMatch.range.last
                }
            }.toList()
            
            // Combine and sort all matches
            data class MatchInfo(val range: IntRange, val content: String, val isBold: Boolean)
            val allMatches = (boldMatches.map { MatchInfo(it.range, it.groupValues[1], true) } +
                    italicMatches.map { MatchInfo(it.range, it.groupValues[1], false) })
                .sortedBy { it.range.first }
            
            for (match in allMatches) {
                // Append text before this match
                if (currentIndex < match.range.first) {
                    append(text.substring(currentIndex, match.range.first))
                }
                
                // Append styled text
                withStyle(
                    SpanStyle(
                        fontWeight = if (match.isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (!match.isBold) FontStyle.Italic else FontStyle.Normal
                    )
                ) {
                    append(match.content)
                }
                
                currentIndex = match.range.last + 1
            }
            
            // Append remaining text
            if (currentIndex < text.length) {
                append(text.substring(currentIndex))
            }
        }
    }
    
    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        color = color
    )
}

// Star rating display
@Composable
fun RatingDisplay(
    rating: Int,
    modifier: Modifier = Modifier
) {
    val stars = (rating / 2f).roundToInt().coerceIn(1, 5)
    val color = when {
        rating >= 8 -> Color(0xFF22C55E) // Green
        rating >= 6 -> Color(0xFFFBBF24) // Yellow
        else -> Color(0xFFEF4444) // Red
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rating/10",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        repeat(stars) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Category label chip
@Composable
fun Chip(
    label: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// Question progress indicator
@Composable
fun InterviewProgressIndicator(
    currentQuestion: Int,
    totalQuestions: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$currentQuestion/$totalQuestions",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { currentQuestion.toFloat() / totalQuestions },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// Interview timer
@Composable
fun TimerDisplay(
    durationSeconds: Long,
    modifier: Modifier = Modifier
) {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Timer,
            contentDescription = "Timer",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Interview summary card
@Composable
fun InterviewSummaryCard(
    summary: InterviewSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Interview Complete!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Overall score
            Text(
                text = String.format("%.1f", summary.overallScore),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Overall Score",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Questions",
                    value = summary.questionsAnswered.toString()
                )
                StatItem(
                    label = "Duration",
                    value = formatDuration(summary.totalDuration)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    return if (minutes > 0) "${minutes}m" else "${seconds}s"
}

@Composable
fun InterviewHistoryShimmerItem(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon placeholder
                    com.phamnhantucode.aicareercoach.ui.components.ShimmerBox(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        shimmerProgress = rememberInfiniteTransition(label = "shimmerIcon").animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "progressIcon"
                        ).value
                    )
                    
                    // Text placeholder
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        com.phamnhantucode.aicareercoach.ui.components.ShimmerBox(
                            modifier = Modifier
                                .width(120.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            shimmerProgress = rememberInfiniteTransition(label = "shimmerTitle").animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "progressTitle"
                            ).value
                        )
                         com.phamnhantucode.aicareercoach.ui.components.ShimmerBox(
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            shimmerProgress = rememberInfiniteTransition(label = "shimmerDate").animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "progressDate"
                            ).value
                        )
                    }
                }
                
                // Status badge placeholder
                com.phamnhantucode.aicareercoach.ui.components.ShimmerBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    shimmerProgress = rememberInfiniteTransition(label = "shimmerStatus").animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "progressStatus"
                    ).value
                )
            }
        }
    }
}

// Shimmer loading view for Live Interview
@Composable
fun LiveInterviewLoadingView(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: Progress indicator placeholder
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.phamnhantucode.aicareercoach.ui.components.ShimmerBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    shimmerProgress = rememberInfiniteTransition(label = "shimmer").animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "progress"
                    ).value
                )
                com.phamnhantucode.aicareercoach.ui.components.ShimmerBox(
                    modifier = Modifier
                        .width(40.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    shimmerProgress = rememberInfiniteTransition(label = "shimmer2").animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "progress2"
                    ).value
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            com.phamnhantucode.aicareercoach.ui.components.ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                shimmerProgress = rememberInfiniteTransition(label = "shimmer3").animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "progress3"
                ).value
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Body: Question Card placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.phamnhantucode.aicareercoach.ui.components.ShimmerBox(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        shimmerProgress = rememberInfiniteTransition(label = "shimmer4").animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "progress4"
                        ).value
                    )
                    com.phamnhantucode.aicareercoach.ui.components.ShimmerBox(
                        modifier = Modifier
                            .width(80.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        shimmerProgress = rememberInfiniteTransition(label = "shimmer5").animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "progress5"
                        ).value
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Question text lines
                repeat(3) {
                    com.phamnhantucode.aicareercoach.ui.components.ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(if (it == 2) 0.6f else 1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        shimmerProgress = rememberInfiniteTransition(label = "shimmerText$it").animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "progressText$it"
                        ).value
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Loading message centered in card
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                   Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    ) 
                }
                
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer: Controls placeholder
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Hint Card
             com.phamnhantucode.aicareercoach.ui.components.ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp)),
                shimmerProgress = rememberInfiniteTransition(label = "shimmerFooter1").animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "progressFooter1"
                ).value
            )

            // Mic Button
            com.phamnhantucode.aicareercoach.ui.components.ShimmerBox(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                shimmerProgress = rememberInfiniteTransition(label = "shimmerMic").animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "progressMic"
                ).value
            )
        }
    }
}
