package com.phamnhantucode.aicareercoach.ui.interviewprep

import java.time.LocalDateTime

/**
 * Represents a single interview question
 */
data class InterviewQuestion(
    val id: Int,
    val type: QuestionType,
    val category: QuestionCategory,
    val question: String,
    val options: List<String> = emptyList(),
    val correctAnswer: Int = -1,
    val explanation: String = "",
    val placeholder: String = ""
)

enum class QuestionType {
    MULTIPLE_CHOICE,
    ESSAY
}

enum class QuestionCategory {
    TECHNICAL,
    BEHAVIORAL,
    SITUATIONAL
}

/**
 * Represents quiz state during active session
 */
data class QuizState(
    val questions: List<InterviewQuestion>,
    val currentQuestionIndex: Int = 0,
    val answers: Map<Int, Any> = emptyMap(),
    val isComplete: Boolean = false,
    val timeStarted: LocalDateTime = LocalDateTime.now(),
    val finalScore: Int = 0
)

/**
 * Represents interview state during active session
 */
data class InterviewState(
    val questions: List<InterviewQuestion>,
    val currentQuestionIndex: Int = 0,
    val answers: Map<Int, Any> = emptyMap(),
    val timeLeft: Int = 1800, // 30 minutes in seconds
    val isComplete: Boolean = false,
    val finalScore: Int = 0
)

/**
 * User's overall progress and statistics
 */
data class UserProgress(
    val totalQuizzes: Int = 0,
    val averageScore: Double = 0.0,
    val questionsAnswered: Int = 0,
    val streak: Int = 0,
    val recentScores: List<Int> = emptyList(),
    val completedQuizStates: List<QuizState> = emptyList()
)

/**
 * Interview tip with category
 */
data class InterviewTip(
    val category: String,
    val icon: String,
    val tips: List<String>,
    val color: String
)

data class InterviewCoachingNotes(
    val summary: String = "",
    val improvementAreas: List<String> = emptyList(),
    val recommendedPracticeFrequency: String = ""
)

/**
 * Loading state with progress and description
 */
data class LoadingState(
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val description: String = ""
)
