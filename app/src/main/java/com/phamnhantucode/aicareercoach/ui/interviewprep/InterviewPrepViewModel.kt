package com.phamnhantucode.aicareercoach.ui.interviewprep

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phamnhantucode.aicareercoach.data.interview.InterviewPrepRepository
import com.phamnhantucode.aicareercoach.data.interview.InterviewPrepRepository.AssessmentRecord
import com.phamnhantucode.aicareercoach.data.interview.InterviewPrepRepository.CoachingNotes
import com.phamnhantucode.aicareercoach.data.interview.InterviewPrepRepository.PracticeTipSpec
import com.phamnhantucode.aicareercoach.data.interview.InterviewPrepRepository.RepositoryQuestionSnapshot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InterviewPrepViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository: InterviewPrepRepository = InterviewPrepRepository(context = application)

    private val _quizState = MutableStateFlow<QuizState?>(null)
    val quizState: StateFlow<QuizState?> = _quizState.asStateFlow()

    private val _interviewState = MutableStateFlow<InterviewState?>(null)
    val interviewState: StateFlow<InterviewState?> = _interviewState.asStateFlow()

    private val _userProgress = MutableStateFlow(UserProgress())
    val userProgress: StateFlow<UserProgress> = _userProgress.asStateFlow()

    private val _practiceTips = MutableStateFlow<List<InterviewTip>>(emptyList())
    val practiceTips: StateFlow<List<InterviewTip>> = _practiceTips.asStateFlow()

    private val _coachingNotes = MutableStateFlow<InterviewCoachingNotes?>(null)
    val coachingNotes: StateFlow<InterviewCoachingNotes?> = _coachingNotes.asStateFlow()

    private val _loadingState = MutableStateFlow(LoadingState(isLoading = true, progress = 0f, description = "Initializing..."))
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var quizBlueprint: List<RepositoryQuestionSnapshot> = emptyList()
    private var interviewBlueprint: List<RepositoryQuestionSnapshot> = emptyList()
    private var latestQuizQuestions: List<InterviewQuestion> = emptyList()
    private var latestInterviewQuestions: List<InterviewQuestion> = emptyList()

    private var timerJob: Job? = null
    private var loadJob: Job? = null

    init {
        refreshContent()
        // Preload question pools in the background for better UX
        viewModelScope.launch {
            try {
                repository.preloadQuestionPools()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to preload question pools in background", e)
            }
        }
    }

    fun refreshContent(force: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _loadingState.value = LoadingState(isLoading = true, progress = 0.1f, description = "Connecting to server...")
            _errorMessage.value = null
            try {
                _loadingState.value = LoadingState(isLoading = true, progress = 0.3f, description = "Fetching interview questions...")
                val content = repository.loadInterviewPrepContent(forceRefreshAuth = force)

                _loadingState.value = LoadingState(isLoading = true, progress = 0.5f, description = "Processing quiz questions...")
                quizBlueprint = content.quizQuestions
                interviewBlueprint = content.interviewQuestions
                latestQuizQuestions = quizBlueprint.mapIndexed { index, snapshot ->
                    snapshot.toInterviewQuestion(index)
                }
                latestInterviewQuestions = interviewBlueprint.mapIndexed { index, snapshot ->
                    snapshot.toInterviewQuestion(index)
                }

                _loadingState.value = LoadingState(isLoading = true, progress = 0.7f, description = "Loading practice tips...")
                _practiceTips.value = content.practiceTips.map { it.toUiModel() }
                _coachingNotes.value = content.coachingNotes?.toUiModel()

                _loadingState.value = LoadingState(isLoading = true, progress = 0.9f, description = "Calculating your progress...")
                _userProgress.value = buildUserProgress(content.assessments)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Log.e(TAG, "Failed to load interview prep content.", error)
                _errorMessage.value = error.localizedMessage
                if (latestQuizQuestions.isEmpty()) {
                    _userProgress.value = UserProgress()
                }
            } finally {
                _loadingState.value = LoadingState(isLoading = false, progress = 1f, description = "Done!")
            }
        }
    }

    fun acknowledgeError() {
        _errorMessage.value = null
    }

    fun startQuiz() {
        if (latestQuizQuestions.isEmpty()) {
            refreshContent(force = true)
            return
        }
        _quizState.value = QuizState(
            questions = latestQuizQuestions,
            currentQuestionIndex = 0,
            answers = emptyMap(),
            isComplete = false,
            timeStarted = LocalDateTime.now()
        )
    }

    fun startInterview() {
        if (latestInterviewQuestions.isEmpty()) {
            refreshContent(force = true)
            return
        }
        _interviewState.value = InterviewState(
            questions = latestInterviewQuestions,
            currentQuestionIndex = 0,
            answers = emptyMap(),
            timeLeft = INTERVIEW_DURATION_SECONDS,
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
            _quizState.value = completedState

            val snapshots = mergeAnswers(quizBlueprint, state.answers)
            viewModelScope.launch {
                try {
                    val improvementTip = _coachingNotes.value?.improvementAreas?.joinToString()
                    repository.recordQuizAttempt(
                        quizScore = score,
                        questions = snapshots,
                        improvementTip = improvementTip
                    )

                    // Mark questions as used in the local pool
                    val questionIds = snapshots.map { it.id }
                    try {
                        repository.markQuestionsAsUsed(questionIds)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to mark questions as used", e)
                    }

                    refreshContent(force = false)
                } catch (error: Exception) {
                    Log.e(TAG, "Failed to record quiz attempt.", error)
                    _errorMessage.value = error.localizedMessage
                }
            }
        }
    }

    private fun completeInterview() {
        timerJob?.cancel()
        _interviewState.value?.let { state ->
            val score = calculateScore(state.questions, state.answers)
            val completedState = state.copy(
                isComplete = true,
                finalScore = score
            )
            _interviewState.value = completedState

            val snapshots = mergeAnswers(interviewBlueprint, state.answers)
            viewModelScope.launch {
                try {
                    val improvementTip = _coachingNotes.value?.summary
                    repository.recordInterviewAttempt(
                        quizScore = score,
                        questions = snapshots,
                        improvementTip = improvementTip
                    )

                    // Mark questions as used in the local pool
                    val questionIds = snapshots.map { it.id }
                    try {
                        repository.markQuestionsAsUsed(questionIds)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to mark questions as used", e)
                    }

                    refreshContent(force = false)
                } catch (error: Exception) {
                    Log.e(TAG, "Failed to record interview attempt.", error)
                    _errorMessage.value = error.localizedMessage
                }
            }
        }
    }

    private fun calculateScore(
        questions: List<InterviewQuestion>,
        answers: Map<Int, Any>,
    ): Int {
        var correct = 0
        var total = 0

        questions.forEachIndexed { index, question ->
            if (question.type == QuestionType.MULTIPLE_CHOICE && question.correctAnswer >= 0) {
                total++
                val answerValue = answers[index]
                if (answerValue is Int && answerValue == question.correctAnswer) {
                    correct++
                }
            }
        }

        if (total == 0) return 0
        val rawScore = (correct.toDouble() / total) * 100
        return rawScore.roundToInt()
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
                        return@launch
                    }
                } ?: return@launch
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
        loadJob?.cancel()
    }

    private fun RepositoryQuestionSnapshot.toInterviewQuestion(index: Int): InterviewQuestion {
        val resolvedType = when (type?.uppercase(Locale.US)) {
            QuestionType.ESSAY.name -> QuestionType.ESSAY
            else -> QuestionType.MULTIPLE_CHOICE
        }
        val resolvedCategory = when (category?.uppercase(Locale.US)) {
            QuestionCategory.BEHAVIORAL.name -> QuestionCategory.BEHAVIORAL
            QuestionCategory.SITUATIONAL.name -> QuestionCategory.SITUATIONAL
            else -> QuestionCategory.TECHNICAL
        }
        return InterviewQuestion(
            id = index,
            type = resolvedType,
            category = resolvedCategory,
            question = question,
            options = if (resolvedType == QuestionType.MULTIPLE_CHOICE) options else emptyList(),
            correctAnswer = correctAnswerIndex ?: -1,
            explanation = explanation ?: "",
            placeholder = placeholder ?: DEFAULT_ESSAY_PLACEHOLDER
        )
    }

    private fun PracticeTipSpec.toUiModel(): InterviewTip {
        return InterviewTip(
            category = category,
            icon = icon,
            tips = tips,
            color = color
        )
    }

    private fun CoachingNotes.toUiModel(): InterviewCoachingNotes {
        return InterviewCoachingNotes(
            summary = summary ?: "",
            improvementAreas = improvementAreas,
            recommendedPracticeFrequency = recommendedPracticeFrequency ?: ""
        )
    }

    private fun mergeAnswers(
        blueprint: List<RepositoryQuestionSnapshot>,
        answers: Map<Int, Any>,
    ): List<RepositoryQuestionSnapshot> {
        return blueprint.mapIndexed { index, snapshot ->
            val answer = answers[index]
            when (answer) {
                is Int -> snapshot.copy(selectedAnswerIndex = answer)
                is String -> snapshot.copy(essayResponse = answer)
                else -> snapshot
            }
        }
    }

    private fun buildUserProgress(assessments: List<AssessmentRecord>): UserProgress {
        if (assessments.isEmpty()) return UserProgress()

        val totalQuizzes = assessments.size
        val averageScore =
            assessments.map { it.quizScore }.average().takeIf { !it.isNaN() } ?: 0.0
        val questionsAnswered = assessments.sumOf { it.questions.size }
        val recentScores = assessments.take(8).map { it.quizScore.roundToInt() }
        val streak = computeStreak(assessments)
        val quizStates = assessments.mapIndexed { index, record ->
            record.toQuizState(index)
        }

        return UserProgress(
            totalQuizzes = totalQuizzes,
            averageScore = averageScore,
            questionsAnswered = questionsAnswered,
            streak = streak,
            recentScores = recentScores,
            completedQuizStates = quizStates
        )
    }

    private fun computeStreak(assessments: List<AssessmentRecord>): Int {
        val zone = ZoneId.systemDefault()
        val uniqueDates = assessments
            .map { it.createdAt.atZone(zone).toLocalDate() }
            .distinct()
            .sortedDescending()
        if (uniqueDates.isEmpty()) return 0

        var streak = 0
        var cursor = LocalDate.now()
        for (date in uniqueDates) {
            if (streak == 0 && (date.isEqual(cursor) || date.isEqual(cursor.minusDays(1)))) {
                streak++
                cursor = date.minusDays(1)
            } else if (date.isEqual(cursor)) {
                streak++
                cursor = cursor.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    private fun AssessmentRecord.toQuizState(index: Int): QuizState {
        val zone = ZoneId.systemDefault()
        val startedAt = createdAt.atZone(zone).toLocalDateTime()
        val questionsUi = questions.mapIndexed { questionIndex, snapshot ->
            snapshot.toInterviewQuestion(questionIndex)
        }
        val answers = buildMap<Int, Any> {
            questions.forEachIndexed { idx, snapshot ->
                snapshot.selectedAnswerIndex?.let { put(idx, it) }
                snapshot.essayResponse?.takeIf { it.isNotBlank() }?.let { put(idx, it) }
            }
        }
        return QuizState(
            questions = questionsUi,
            currentQuestionIndex = questionsUi.lastIndex.coerceAtLeast(0),
            answers = answers,
            isComplete = true,
            timeStarted = startedAt,
            finalScore = quizScore.roundToInt()
        )
    }

    companion object {
        private const val TAG = "InterviewPrepVM"
        private const val INTERVIEW_DURATION_SECONDS = 30 * 60
        private const val DEFAULT_ESSAY_PLACEHOLDER =
            "Structure your response with STAR (Situation, Task, Action, Result)."
    }
}
