package com.phamnhantucode.aicareercoach.ui.interviewprep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.round

class InterviewPrepViewModel : ViewModel() {

    // Sample quiz questions
    private val quizQuestions = listOf(
        InterviewQuestion(
            id = 1,
            type = QuestionType.MULTIPLE_CHOICE,
            category = QuestionCategory.TECHNICAL,
            question = "What is the time complexity of binary search?",
            options = listOf("O(n)", "O(log n)", "O(n²)", "O(1)"),
            correctAnswer = 1,
            explanation = "Binary search divides the search space in half with each iteration, resulting in O(log n) time complexity."
        ),
        InterviewQuestion(
            id = 2,
            type = QuestionType.MULTIPLE_CHOICE,
            category = QuestionCategory.TECHNICAL,
            question = "Which data structure uses LIFO principle?",
            options = listOf("Queue", "Stack", "Array", "Tree"),
            correctAnswer = 1,
            explanation = "Stack follows Last-In-First-Out (LIFO) principle where the last element added is the first one to be removed."
        ),
        InterviewQuestion(
            id = 3,
            type = QuestionType.MULTIPLE_CHOICE,
            category = QuestionCategory.BEHAVIORAL,
            question = "What is the STAR method used for?",
            options = listOf("Coding patterns", "Interview answers", "Data structures", "Testing"),
            correctAnswer = 1,
            explanation = "STAR (Situation, Task, Action, Result) is a technique for structuring behavioral interview answers."
        ),
        InterviewQuestion(
            id = 4,
            type = QuestionType.MULTIPLE_CHOICE,
            category = QuestionCategory.TECHNICAL,
            question = "What does REST stand for?",
            options = listOf(
                "Remote Event Service",
                "Representational State Transfer",
                "Resource Execution System",
                "Real-time Event Stream"
            ),
            correctAnswer = 1,
            explanation = "REST stands for Representational State Transfer, an architectural style for web services."
        ),
        InterviewQuestion(
            id = 5,
            type = QuestionType.MULTIPLE_CHOICE,
            category = QuestionCategory.TECHNICAL,
            question = "Which SQL command is used to retrieve data?",
            options = listOf("INSERT", "UPDATE", "SELECT", "DELETE"),
            correctAnswer = 2,
            explanation = "SELECT is used to query and retrieve data from database tables."
        )
    )

    // Sample interview questions (includes essay questions)
    private val interviewQuestions = listOf(
        InterviewQuestion(
            id = 1,
            type = QuestionType.MULTIPLE_CHOICE,
            category = QuestionCategory.TECHNICAL,
            question = "What is polymorphism in OOP?",
            options = listOf(
                "Multiple inheritance",
                "Ability to take multiple forms",
                "Data encapsulation",
                "Code reusability"
            ),
            correctAnswer = 1,
            explanation = "Polymorphism allows objects to take multiple forms and behave differently based on their data type or class."
        ),
        InterviewQuestion(
            id = 2,
            type = QuestionType.ESSAY,
            category = QuestionCategory.BEHAVIORAL,
            question = "Tell me about a challenging project you worked on and how you overcame obstacles.",
            placeholder = "Describe the situation, your approach, and the outcome..."
        ),
        InterviewQuestion(
            id = 3,
            type = QuestionType.MULTIPLE_CHOICE,
            category = QuestionCategory.TECHNICAL,
            question = "Which HTTP method is idempotent?",
            options = listOf("POST", "PUT", "PATCH", "All of the above"),
            correctAnswer = 1,
            explanation = "PUT is idempotent, meaning multiple identical requests have the same effect as a single request."
        ),
        InterviewQuestion(
            id = 4,
            type = QuestionType.ESSAY,
            category = QuestionCategory.SITUATIONAL,
            question = "How would you handle a disagreement with a team member about a technical decision?",
            placeholder = "Explain your approach using specific examples..."
        )
    )

    private val _quizState = MutableStateFlow<QuizState?>(null)
    val quizState: StateFlow<QuizState?> = _quizState.asStateFlow()

    private val _interviewState = MutableStateFlow<InterviewState?>(null)
    val interviewState: StateFlow<InterviewState?> = _interviewState.asStateFlow()

    private val _userProgress = MutableStateFlow(UserProgress(
        totalQuizzes = 3,
        averageScore = 81.6,
        questionsAnswered = 15,
        streak = 5,
        recentScores = listOf(75, 80, 90),
        completedQuizStates = listOf(
            QuizState(quizQuestions.take(5), 5, mapOf(0 to 1, 1 to 1, 2 to 1, 3 to 1, 4 to 2), true, LocalDateTime.now().minusDays(1), 80),
            QuizState(quizQuestions.take(5), 5, mapOf(0 to 1, 1 to 1, 2 to 1, 3 to 1, 4 to 1), true, LocalDateTime.now().minusDays(2), 75),
            QuizState(quizQuestions.take(5), 5, mapOf(0 to 1, 1 to 1, 2 to 1, 3 to 1, 4 to 1), true, LocalDateTime.now().minusDays(3), 90)
        )
    ))
    val userProgress: StateFlow<UserProgress> = _userProgress.asStateFlow()

    private var timerJob: Job? = null

    fun startQuiz() {
        _quizState.value = QuizState(
            questions = quizQuestions,
            currentQuestionIndex = 0,
            answers = emptyMap(),
            isComplete = false,
            timeStarted = LocalDateTime.now()
        )
    }

    fun startInterview() {
        _interviewState.value = InterviewState(
            questions = interviewQuestions,
            currentQuestionIndex = 0,
            answers = emptyMap(),
            timeLeft = 1800,
            isComplete = false
        )
        startTimer()
    }

    fun answerQuizQuestion(answer: Any) {
        _quizState.value?.let { state ->
            _quizState.value = state.copy(
                answers = state.answers + (state.currentQuestionIndex to answer)
            )
        }
    }

    fun answerInterviewQuestion(answer: Any) {
        _interviewState.value?.let { state ->
            _interviewState.value = state.copy(
                answers = state.answers + (state.currentQuestionIndex to answer)
            )
        }
    }

    fun nextQuizQuestion() {
        _quizState.value?.let { state ->
            if (state.currentQuestionIndex < state.questions.size - 1) {
                _quizState.value = state.copy(
                    currentQuestionIndex = state.currentQuestionIndex + 1
                )
            } else {
                completeQuiz()
            }
        }
    }

    fun nextInterviewQuestion() {
        _interviewState.value?.let { state ->
            if (state.currentQuestionIndex < state.questions.size - 1) {
                _interviewState.value = state.copy(
                    currentQuestionIndex = state.currentQuestionIndex + 1
                )
            } else {
                completeInterview()
            }
        }
    }

    private fun completeQuiz() {
        _quizState.value?.let { state ->
            val score = calculateScore(state.questions, state.answers)
            val completedState = state.copy(isComplete = true, finalScore = score)

            _userProgress.value = _userProgress.value.copy(
                totalQuizzes = _userProgress.value.totalQuizzes + 1,
                completedQuizStates = listOf(completedState) + _userProgress.value.completedQuizStates,
                recentScores = (_userProgress.value.recentScores + score).takeLast(8)
            )

            _quizState.value = completedState
        }
    }

    private fun completeInterview() {
        timerJob?.cancel()
        _interviewState.value?.let { state ->
            val score = calculateScore(state.questions, state.answers)
            _interviewState.value = state.copy(
                isComplete = true,
                finalScore = score
            )
        }
    }

    private fun calculateScore(questions: List<InterviewQuestion>, answers: Map<Int, Any>): Int {
        var correct = 0
        var total = 0

        questions.forEachIndexed { index, question ->
            if (question.type == QuestionType.MULTIPLE_CHOICE) {
                total++
                if (answers[index] == question.correctAnswer) {
                    correct++
                }
            }
        }

        return if (total > 0) round((correct.toDouble() / total) * 100).toInt() else 0
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _interviewState.value?.let { state ->
                    if (state.timeLeft > 0) {
                        _interviewState.value = state.copy(timeLeft = state.timeLeft - 1)
                    } else {
                        completeInterview()
                        break
                    }
                }
            }
        }
    }

    fun resetQuiz() {
        _quizState.value = null
    }

    fun resetInterview() {
        timerJob?.cancel()
        _interviewState.value = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
