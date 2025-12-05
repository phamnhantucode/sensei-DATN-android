package com.phamnhantucode.aicareercoach.ui.liveinterview

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phamnhantucode.aicareercoach.data.audio.AudioRecorder
import com.phamnhantucode.aicareercoach.data.audio.RecordingState
import com.phamnhantucode.aicareercoach.data.interview.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for managing live interview state and orchestrating audio recording,
 * transcription, and AI feedback.
 */
class LiveInterviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LiveInterviewRepository(context = application)
    private val audioRecorder = AudioRecorder(context = application)

    private val _uiState = MutableStateFlow<LiveInterviewUiState>(LiveInterviewUiState.Setup)
    val uiState: StateFlow<LiveInterviewUiState> = _uiState.asStateFlow()

    private val _interviewSession = MutableStateFlow<LiveMockInterviewSession?>(null)
    val interviewSession: StateFlow<LiveMockInterviewSession?> = _interviewSession.asStateFlow()

    private val _currentQuestion = MutableStateFlow<LiveQuestion?>(null)
    val currentQuestion: StateFlow<LiveQuestion?> = _currentQuestion.asStateFlow()

    private val _currentFeedback = MutableStateFlow<QuestionFeedback?>(null)
    val currentFeedback: StateFlow<QuestionFeedback?> = _currentFeedback.asStateFlow()

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = audioRecorder.recordingState

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _interviewDuration = MutableStateFlow(0L)
    val interviewDuration: StateFlow<Long> = _interviewDuration.asStateFlow()

    private val _questionStartTime = MutableStateFlow(0L)

    private var timerJob: Job? = null
    private var currentAudioFile: File? = null
    private val answeredQuestions = mutableListOf<LiveQuestion>()
    private var recordingStartTime = 0L

    companion object {
        private const val TAG = "LiveInterviewViewModel"
        private const val MIN_RECORDING_DURATION_MS = 1500L // 1.5 seconds minimum
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
        timerJob?.cancel()
    }

    /**
     * Starts a new live interview with the specified configuration.
     */
    fun startInterview(config: InterviewConfig) {
        Log.d(TAG, "startInterview() called with config: userId=${config.userId}, type=${config.interviewType}, count=${config.questionCount}")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Setting UI state to Starting")
                _uiState.value = LiveInterviewUiState.Starting

                val request = StartLiveInterviewRequest(
                    userId = config.userId,
                    interviewType = config.interviewType,
                    questionCount = config.questionCount,
                    industry = config.industry,
                    experienceLevel = config.experienceLevel,
                    skills = config.skills
                )
                Log.d(TAG, "Created StartLiveInterviewRequest: $request")

                Log.d(TAG, "Calling repository.startLiveInterview()")
                val result = repository.startLiveInterview(request)
                Log.d(TAG, "Repository call completed. Success: ${result.isSuccess}")

                if (result.isSuccess) {
                    val startResult = result.getOrNull()!!
                    Log.d(TAG, "Start result received: sessionId=${startResult.sessionId}, hasFirstQuestion=${startResult.firstQuestion != null}")

                    val session = LiveMockInterviewSession(
                        id = startResult.sessionId,
                        userId = config.userId,
                        interviewType = config.interviewType,
                        status = InterviewStatus.IN_PROGRESS,
                        targetQuestionCount = config.questionCount,
                        currentQuestionIndex = 0,
                        startedAt = System.currentTimeMillis()
                    )

                    _interviewSession.value = session
                    _currentQuestion.value = startResult.firstQuestion
                    _currentFeedback.value = null
                    _questionStartTime.value = System.currentTimeMillis()
                    Log.d(TAG, "Setting UI state to ActiveQuestion")
                    _uiState.value = LiveInterviewUiState.ActiveQuestion
                    startTimer()

                    Log.d(TAG, "Interview started successfully: ${startResult.sessionId}")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to start interview"
                    Log.e(TAG, "Failed to start interview: $errorMsg", result.exceptionOrNull())
                    _errorMessage.value = errorMsg
                    Log.d(TAG, "Setting UI state back to Setup due to failure")
                    _uiState.value = LiveInterviewUiState.Setup
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception in startInterview()", e)
                _errorMessage.value = e.message ?: "An error occurred"
                Log.d(TAG, "Setting UI state back to Setup due to exception")
                _uiState.value = LiveInterviewUiState.Setup
            }
        }
    }

    /**
     * Starts recording audio (push-to-talk pressed).
     */
    fun startRecording() {
        recordingStartTime = System.currentTimeMillis()
        currentAudioFile = audioRecorder.startRecording()
        if (currentAudioFile == null) {
            _errorMessage.value = "Failed to start recording. Please check microphone permissions."
        }
    }

    /**
     * Stops recording and processes the answer (push-to-talk released).
     */
    fun stopRecordingAndProcess() {
        viewModelScope.launch {
            // Check minimum recording duration
            val recordingDuration = System.currentTimeMillis() - recordingStartTime
            if (recordingDuration < MIN_RECORDING_DURATION_MS) {
                _errorMessage.value = "Recording too short. Please hold the button and speak for at least 2 seconds."
                audioRecorder.cancelRecording()
                return@launch
            }

            val question = _currentQuestion.value
            val session = _interviewSession.value

            if (question == null || session == null) {
                _errorMessage.value = "No active question"
                audioRecorder.cancelRecording()
                return@launch
            }

            try {
                // Stop recording (suspend function)
                val audioFile = audioRecorder.stopRecording()
                if (audioFile == null) {
                    _errorMessage.value = "Failed to stop recording"
                    return@launch
                }

                _uiState.value = LiveInterviewUiState.Processing

                val questionDuration = System.currentTimeMillis() - _questionStartTime.value
                val updatedQuestion = question.copy(duration = questionDuration)

                val isLastQuestion = session.currentQuestionIndex >= session.targetQuestionCount - 1
                val previousQuestions = answeredQuestions.map { it.questionText }

                val result = repository.processAnswer(
                    audioFile = audioFile,
                    question = updatedQuestion,
                    sessionId = session.id,
                    isLastQuestion = isLastQuestion,
                    previousQuestions = previousQuestions
                )

                if (result.isSuccess) {
                    val answerResult = result.getOrNull()!!

                    // Update answered questions list
                    answeredQuestions.add(updatedQuestion.copy(
                        userAnswer = answerResult.transcription,
                        feedback = answerResult.feedback,
                        rating = answerResult.rating
                    ))

                    // Show feedback
                    _currentFeedback.value = QuestionFeedback(
                        transcription = answerResult.transcription,
                        feedback = answerResult.feedback,
                        rating = answerResult.rating,
                        nextQuestion = answerResult.nextQuestion
                    )

                    _uiState.value = LiveInterviewUiState.ViewingFeedback

                    Log.d(TAG, "Answer processed: rating=${answerResult.rating}")

                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to process answer"
                    _uiState.value = LiveInterviewUiState.ActiveQuestion
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing answer", e)
                _errorMessage.value = e.message ?: "An error occurred while processing your answer"
                _uiState.value = LiveInterviewUiState.ActiveQuestion
            }
        }
    }

    /**
     * Continues to the next question after viewing feedback.
     */
    fun continueToNextQuestion() {
        val session = _interviewSession.value ?: return
        val nextIndex = session.currentQuestionIndex + 1

        if (nextIndex >= session.targetQuestionCount) {
            // Interview complete
            completeInterview()
        } else {
            // Load next question from the feedback result
            viewModelScope.launch {
                try {
                    _uiState.value = LiveInterviewUiState.LoadingNextQuestion

                    // Get next question from cache or generate new one
                    val feedback = _currentFeedback.value
                    val nextQuestion = feedback?.nextQuestion

                    if (nextQuestion != null) {
                        _interviewSession.value = session.copy(currentQuestionIndex = nextIndex)
                        _currentQuestion.value = nextQuestion
                        _currentFeedback.value = null
                        _questionStartTime.value = System.currentTimeMillis()
                        
                        // Reset audio recorder state for next question
                        audioRecorder.reset()
                        
                        _uiState.value = LiveInterviewUiState.ActiveQuestion
                    } else {
                        _errorMessage.value = "Failed to load next question"
                        _uiState.value = LiveInterviewUiState.ActiveQuestion
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error loading next question", e)
                    _errorMessage.value = e.message ?: "An error occurred"
                    _uiState.value = LiveInterviewUiState.ActiveQuestion
                }
            }
        }
    }

    /**
     * Pauses the interview.
     */
    fun pauseInterview() {
        stopTimer()
        val session = _interviewSession.value ?: return
        _interviewSession.value = session.copy(status = InterviewStatus.PAUSED)
        _uiState.value = LiveInterviewUiState.Paused
    }

    /**
     * Resumes the interview from pause.
     */
    fun resumeInterview() {
        startTimer()
        val session = _interviewSession.value ?: return
        _interviewSession.value = session.copy(status = InterviewStatus.IN_PROGRESS)
        _uiState.value = LiveInterviewUiState.ActiveQuestion
    }

    /**
     * Completes the interview and generates summary.
     */
    fun completeInterview() {
        stopTimer()
        val session = _interviewSession.value ?: return

        viewModelScope.launch {
            try {
                _uiState.value = LiveInterviewUiState.GeneratingSummary

                val result = repository.completeInterview(
                    sessionId = session.id,
                    questions = answeredQuestions
                )

                if (result.isSuccess) {
                    val summary = result.getOrNull()!!
                    _uiState.value = LiveInterviewUiState.Completed(summary)

                    Log.d(TAG, "Interview completed: score=${summary.overallScore}")
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to generate summary"
                    _uiState.value = LiveInterviewUiState.Completed(
                        InterviewSummary(
                            sessionId = session.id,
                            overallScore = 0f,
                            questionsAnswered = answeredQuestions.size,
                            totalDuration = _interviewDuration.value,
                            categoryBreakdown = emptyMap(),
                            strengths = emptyList(),
                            areasForImprovement = emptyList(),
                            questions = answeredQuestions
                        )
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error completing interview", e)
                _errorMessage.value = e.message ?: "An error occurred"
            }
        }
    }

    /**
     * Abandons the interview.
     */
    fun abandonInterview() {
        stopTimer()
        viewModelScope.launch {
            audioRecorder.cancelRecording()
        }
        _uiState.value = LiveInterviewUiState.Setup
        _interviewSession.value = null
        _currentQuestion.value = null
        _currentFeedback.value = null
        answeredQuestions.clear()
    }

    /**
     * Clears error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _interviewDuration.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }
}

/**
 * UI state for live interview screen.
 */
sealed class LiveInterviewUiState {
    object Setup : LiveInterviewUiState()
    object Starting : LiveInterviewUiState()
    object ActiveQuestion : LiveInterviewUiState()
    object Processing : LiveInterviewUiState()
    object ViewingFeedback : LiveInterviewUiState()
    object LoadingNextQuestion : LiveInterviewUiState()
    object Paused : LiveInterviewUiState()
    object GeneratingSummary : LiveInterviewUiState()
    data class Completed(val summary: InterviewSummary) : LiveInterviewUiState()
}

/**
 * Configuration for starting an interview.
 */
data class InterviewConfig(
    val userId: String,
    val interviewType: InterviewType,
    val questionCount: Int,
    val industry: String? = null,
    val experienceLevel: Int? = null,
    val skills: List<String> = emptyList()
)

/**
 * Feedback for a single question.
 */
data class QuestionFeedback(
    val transcription: String,
    val feedback: String,
    val rating: Int,
    val nextQuestion: LiveQuestion? = null
)
