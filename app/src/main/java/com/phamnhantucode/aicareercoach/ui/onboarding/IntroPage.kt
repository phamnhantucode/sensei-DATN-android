package com.phamnhantucode.aicareercoach.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

@Composable
fun IntroPage(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GridBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            item {
                HeroSection(
                    headline = "Your AI Career Coach for Professional Success",
                    subheading = "Advance your career with personalized guidance, interview prep, and AI-powered tools for job success.",
                    onGetStarted = onGetStarted,
                    onSignIn = onSignIn
                )
            }

            item {
                FeatureSection(features = featureItems)
            }

            item {
                StatsSection(stats = statsItems)
            }

            item {
                HowItWorksSection(steps = howItWorksItems)
            }

            item {
                TestimonialSection(testimonials = testimonialItems)
            }

            item {
                FaqSection(faqs = faqItems)
            }

            item {
                CallToActionSection(onGetStarted = onGetStarted)
            }
        }
    }
}

@Composable
private fun GridBackground() {
    val verticalColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    val horizontalColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    val backgroundColor = MaterialTheme.colorScheme.background
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 80.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = verticalColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += gridSize
        }

        var y = 0f
        while (y < size.height) {
            drawLine(
                color = horizontalColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += gridSize
        }

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    backgroundColor.copy(alpha = 0.9f)
                ),
                center = center,
                radius = size.maxDimension
            )
        )
    }
}

@Composable
private fun HeroSection(
    headline: String,
    subheading: String,
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = gradientHeadline(headline),
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = subheading,
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                ),
                                tileMode = TileMode.Clamp
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Interactive Dashboard Preview",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onGetStarted) {
                Text(text = "Get Started")
            }
            OutlinedButton(onClick = onSignIn) {
                Text(text = "Sign In")
            }
        }
    }
}

@Composable
private fun FeatureSection(features: List<FeatureItem>) {
    SectionContainer(
        title = "Powerful Features for Your Career Growth",
        subtitle = "Tools designed to accelerate every part of your job search."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            features.forEach { item ->
                FeatureCard(item)
            }
        }
    }
}

@Composable
private fun FeatureCard(item: FeatureItem) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(icon = item.icon)
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun StatsSection(stats: List<StatItem>) {
    SectionContainer(title = "Trusted by Ambitious Professionals") {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            stats.forEach { stat ->
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stat.value,
                            style = MaterialTheme.typography.displaySmall.copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = stat.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorksSection(steps: List<HowItWorksItem>) {
    SectionContainer(
        title = "How It Works",
        subtitle = "Four simple steps to accelerate your career growth."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            steps.forEachIndexed { index, item ->
                ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RowHeader(
                            index = index + 1,
                            title = item.title,
                            icon = item.icon
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowHeader(index: Int, title: String, icon: ImageVector) {
    val badgeGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(brush = badgeGradient, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text(
            text = "$index. $title",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun TestimonialSection(testimonials: List<TestimonialItem>) {
    SectionContainer(
        title = "What Our Users Say",
        subtitle = "Real stories from professionals advancing their careers with AI support."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            testimonials.forEach { testimonial ->
                ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SubcomposeAsyncImage(
                            model = testimonial.imageUrl,
                            contentDescription = "${testimonial.author} profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        ) {
                            when (painter.state) {
                                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                                else -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = testimonial.initials,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                        Text(
                            text = "\"${testimonial.quote}\"",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = testimonial.author,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = testimonial.role,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = testimonial.company,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FaqSection(faqs: List<FaqItem>) {
    SectionContainer(
        title = "Frequently Asked Questions",
        subtitle = "Find answers to common questions about our platform."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            faqs.forEach { faq ->
                ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = faq.question, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = faq.answer,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallToActionSection(onGetStarted: () -> Unit) {
    SectionContainer(
        title = "Ready to Accelerate Your Career?",
        subtitle = "Join thousands of professionals who are advancing with AI-powered guidance.",
        accent = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onGetStarted) {
                Text(text = "Start Your Journey Today")
            }
        }
    }
}

@Composable
private fun SectionContainer(
    title: String,
    subtitle: String? = null,
    accent: Boolean = false,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(28.dp),
        colors = if (accent) {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            )
        }
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (accent) {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = 32.dp, horizontal = 24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = if (accent) {
                            MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            MaterialTheme.typography.headlineMedium
                        },
                        textAlign = TextAlign.Center
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (accent) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                content()
            }
        }
    }
}

private fun gradientHeadline(text: String) = buildAnnotatedString {
    val gradientColors = listOf(
        Color(0xFF9CA3AF),
        Color(0xFF6B7280),
        Color(0xFF1F2937),
        Color(0xFF374151)
    )
    withStyle(
        style = SpanStyle(
            brush = Brush.linearGradient(colors = gradientColors),
            fontWeight = FontWeight.Black
        )
    ) {
        append(text)
    }
}

private data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private data class StatItem(val value: String, val label: String)

private data class HowItWorksItem(
    val title: String,
    val description: String,
    val icon: ImageVector
)

private data class TestimonialItem(
    val quote: String,
    val author: String,
    val imageUrl: String,
    val role: String,
    val company: String
) {
    val initials: String
        get() = author
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
}

private data class FaqItem(val question: String, val answer: String)

private val featureItems = listOf(
    FeatureItem(
        icon = Icons.Outlined.AutoAwesome,
        title = "AI-Powered Career Guidance",
        description = "Get personalized career advice and insights powered by advanced AI technology."
    ),
    FeatureItem(
        icon = Icons.Outlined.WorkOutline,
        title = "Interview Preparation",
        description = "Practice with role-specific questions and get instant feedback to improve your performance."
    ),
    FeatureItem(
        icon = Icons.Outlined.Insights,
        title = "Industry Insights",
        description = "Stay ahead with real-time industry trends, salary data, and market analysis."
    ),
    FeatureItem(
        icon = Icons.Outlined.Description,
        title = "Smart Resume Creation",
        description = "Generate ATS-optimized resumes with AI assistance."
    )
)

private val statsItems = listOf(
    StatItem("50+", "Industries Covered"),
    StatItem("1000+", "Interview Questions"),
    StatItem("95%", "Success Rate"),
    StatItem("24/7", "AI Support")
)

private val howItWorksItems = listOf(
    HowItWorksItem(
        title = "Professional Onboarding",
        description = "Share your industry and expertise for personalized guidance.",
        icon = Icons.Outlined.PersonAdd
    ),
    HowItWorksItem(
        title = "Craft Your Documents",
        description = "Create ATS-optimized resumes and compelling cover letters.",
        icon = Icons.Outlined.Edit
    ),
    HowItWorksItem(
        title = "Prepare for Interviews",
        description = "Practice with AI-powered mock interviews tailored to your role.",
        icon = Icons.Outlined.QuestionAnswer
    ),
    HowItWorksItem(
        title = "Track Your Progress",
        description = "Monitor improvements with detailed performance analytics.",
        icon = Icons.Outlined.Leaderboard
    )
)

private val testimonialItems = listOf(
    TestimonialItem(
        quote = "The AI-powered interview prep was a game-changer. Landed my dream job at a top tech company!",
        author = "Sarah Chen",
        imageUrl = "https://randomuser.me/api/portraits/women/75.jpg",
        role = "Software Engineer",
        company = "Tech Giant Co."
    ),
    TestimonialItem(
        quote = "The industry insights helped me pivot my career successfully. The salary data was spot-on!",
        author = "Michael Rodriguez",
        imageUrl = "https://randomuser.me/api/portraits/men/75.jpg",
        role = "Product Manager",
        company = "StartUp Inc."
    ),
    TestimonialItem(
        quote = "My resume's ATS score improved significantly. Got more interviews in two weeks than in six months!",
        author = "Priya Patel",
        imageUrl = "https://randomuser.me/api/portraits/women/74.jpg",
        role = "Marketing Director",
        company = "Global Corp"
    )
)

private val faqItems = listOf(
    FaqItem(
        question = "What makes Sensai unique as a career development tool?",
        answer = "Sensai combines AI-powered career tools with industry-specific insights to help you advance your career. Our platform offers three main features: an intelligent resume builder, a cover letter generator, and an adaptive interview preparation system. Each tool is tailored to your industry and skills, providing personalized guidance for your professional journey."
    ),
    FaqItem(
        question = "How does Sensai create tailored content?",
        answer = "Sensai learns about your industry, experience, and skills during onboarding. It then uses this information to generate customized resumes, cover letters, and interview questions. The content is specifically aligned with your professional background and industry standards, making it highly relevant and effective."
    ),
    FaqItem(
        question = "How accurate and up-to-date are Sensai's industry insights?",
        answer = "We update our industry insights weekly using advanced AI analysis of current market trends. This includes salary data, in-demand skills, and industry growth patterns. Our system constantly evolves to ensure you have the most relevant information for your career decisions."
    ),
    FaqItem(
        question = "Is my data secure with Sensai?",
        answer = "Absolutely. We prioritize the security of your professional information. All data is encrypted and securely stored using industry-standard practices. We use Clerk for authentication and never share your personal information with third parties."
    ),
    FaqItem(
        question = "How can I track my interview preparation progress?",
        answer = "Sensai tracks your performance across multiple practice interviews, providing detailed analytics and improvement suggestions. You can view your progress over time, identify areas for improvement, and receive AI-generated tips to enhance your interview skills based on your responses."
    ),
    FaqItem(
        question = "Can I edit the AI-generated content?",
        answer = "Yes! While Sensai generates high-quality initial content, you have full control to edit and customize all generated resumes, cover letters, and other content. Our markdown editor makes it easy to refine the content to perfectly match your needs."
    )
)

@Composable
@Preview(showBackground = true)
private fun OnboardingPreview() {
//    OnboardingScreen()
}
