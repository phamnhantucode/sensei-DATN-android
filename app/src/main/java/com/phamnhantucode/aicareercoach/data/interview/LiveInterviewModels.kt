package com.phamnhantucode.aicareercoach.data.interview

/**
 * Domain models for live interview feature
 */

/**
 * Represents a live mock interview session
 */
data class LiveMockInterviewSession(
    val id: String = "",
    val userId: String,
    val interviewType: InterviewType = InterviewType.GENERAL,
    val status: InterviewStatus = InterviewStatus.NOT_STARTED,
    val targetQuestionCount: Int = 10,
    val currentQuestionIndex: Int = 0,
    val questions: List<LiveQuestion> = emptyList(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val overallScore: Float? = null
)

/**
 * Represents a single question in a live interview
 */
data class LiveQuestion(
    val id: String = "",
    val liveMockInterviewId: String = "",
    val questionText: String,
    val category: String,
    val correctAnswer: String = "",
    val userAnswer: String? = null,
    val audioTranscript: String? = null,
    val feedback: String? = null,
    val rating: Int? = null,
    val duration: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Interview types
 */
enum class InterviewType {
    TECHNICAL,
    BEHAVIORAL,
    GENERAL
}

/**
 * Interview status
 */
enum class InterviewStatus {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    ABANDONED
}

/**
 * Result of starting a new live interview
 */
data class StartInterviewResult(
    val sessionId: String,
    val firstQuestion: LiveQuestion
)

/**
 * Result of starting a new batch interview with all questions pre-generated
 */
data class BatchStartInterviewResult(
    val sessionId: String,
    val questions: List<LiveQuestion>
)

/**
 * Result of processing an answer
 */
data class AnswerResult(
    val transcription: String,
    val feedback: String,
    val rating: Int,
    val nextQuestion: LiveQuestion?
)

/**
 * Complete interview summary
 */
data class InterviewSummary(
    val sessionId: String,
    val overallScore: Float,
    val questionsAnswered: Int,
    val totalDuration: Long,
    val categoryBreakdown: Map<String, CategoryPerformance>,
    val strengths: List<String>,
    val areasForImprovement: List<String>,
    val questions: List<LiveQuestion>
)

/**
 * Performance breakdown by category
 */
data class CategoryPerformance(
    val category: String,
    val averageRating: Float,
    val questionsCount: Int
)

/**
 * Request to start a live interview
 */
data class StartLiveInterviewRequest(
    val userId: String,
    val interviewType: InterviewType,
    val questionCount: Int,
    val industry: String? = null,
    val experienceLevel: Int? = null,
    val skills: List<String> = emptyList()
)

/**
 * Network models for API communication
 */
data class LiveMockInterviewDto(
    val id: String? = null,
    val userId: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class LiveInterviewQuestionDto(
    val id: String? = null,
    val liveMockInterviewId: String,
    val question: String,
    val correctAnswer: String,
    val feedback: String? = null,
    val rating: Int? = null,
    val userAnswer: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Request to save a completed interview to the database
 */
data class SaveInterviewRequest(
    val interview: LiveMockInterviewDto,
    val questions: List<LiveInterviewQuestionDto>
)
