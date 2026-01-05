package com.phamnhantucode.aicareercoach.data.interview

// Domain models for live interview

// Live mock interview session
data class LiveMockInterviewSession(
    val id: String = "",
    val userId: String,
    val interviewType: InterviewType = InterviewType.GENERAL,
    val status: InterviewStatus = InterviewStatus.NOT_STARTED,
    val targetQuestionCount: Int = 5,
    val currentQuestionIndex: Int = 0,
    val questions: List<LiveQuestion> = emptyList(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val overallScore: Float? = null,
    val jobTitle: String = "",
    val jobDescription: String = ""
)

// Single question in live interview
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

// Interview types
enum class InterviewType {
    TECHNICAL,
    BEHAVIORAL,
    GENERAL
}

// Interview status
enum class InterviewStatus {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    ABANDONED
}

// Start interview result
data class StartInterviewResult(
    val sessionId: String,
    val firstQuestion: LiveQuestion
)

// Batch start result
data class BatchStartInterviewResult(
    val sessionId: String,
    val questions: List<LiveQuestion>
)

// Answer processing result
data class AnswerResult(
    val transcription: String,
    val feedback: String,
    val rating: Int,
    val nextQuestion: LiveQuestion?
)

// Interview summary
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

// Category performance
data class CategoryPerformance(
    val category: String,
    val averageRating: Float,
    val questionsCount: Int
)

// Start request

data class StartLiveInterviewRequest(
    val userId: String,
    val interviewType: InterviewType,
    val questionCount: Int,
    val industry: String? = null,
    val experienceLevel: Int? = null,
    val skills: List<String> = emptyList(),
    val jobTitle: String = "",
    val jobDescription: String = "",
    val resumeContent: String? = null,
    val resumeId: String? = null,
    val existingQuestions: List<LiveQuestion>? = null,
    val sessionId: String? = null
) {
    companion object {
        fun createEmpty() = StartLiveInterviewRequest(
            userId = "", // userId is required, assuming an empty string for empty state
            interviewType = InterviewType.BEHAVIORAL,
            questionCount = 0, // questionCount is required, assuming 0 for empty state
            jobTitle = "",
            jobDescription = "",
            industry = null,
            experienceLevel = null,
            skills = emptyList(), // skills is required, assuming emptyList for empty state
            resumeContent = null,
            resumeId = null,
            existingQuestions = null,
            sessionId = null
        )
    }
}

// Network models
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

// Save interview request
data class SaveInterviewRequest(
    val interview: LiveMockInterviewDto,
    val questions: List<LiveInterviewQuestionDto>
)
