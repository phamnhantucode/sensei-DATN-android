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

// Live interview ViewModel
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
    val audioAmplitude: StateFlow<Float> = audioRecorder.maxAmplitude

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _interviewDuration = MutableStateFlow(0L)
    val interviewDuration: StateFlow<Long> = _interviewDuration.asStateFlow()

    private val _questionStartTime = MutableStateFlow(0L)

    // New state for batch interview
    private val _allQuestions = MutableStateFlow<List<LiveQuestion>>(emptyList())
    val allQuestions: StateFlow<List<LiveQuestion>> = _allQuestions.asStateFlow()
    
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()
    
    private val _useBatchMode = MutableStateFlow(true) // Default to new batch mode
    val useBatchMode: StateFlow<Boolean> = _useBatchMode.asStateFlow()

    private val _showCreditDialog = MutableStateFlow(false)
    val showCreditDialog: StateFlow<Boolean> = _showCreditDialog.asStateFlow()

    private val _interviewHistory = MutableStateFlow<List<LiveMockInterviewSession>>(emptyList())
    val interviewHistory: StateFlow<List<LiveMockInterviewSession>> = _interviewHistory.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    init {
        loadInterviewHistory()
    }

    fun loadInterviewHistory() {
        val user = com.clerk.api.Clerk.user
        if (user != null) {
            viewModelScope.launch {
                _isHistoryLoading.value = true
                val result = repository.getInterviewHistory(user.id)
                if (result.isSuccess) {
                    _interviewHistory.value = result.getOrNull() ?: emptyList()
                } else {
                    Log.e(TAG, "Failed to load interview history", result.exceptionOrNull())
                }
                _isHistoryLoading.value = false
            }
        }
    }

    fun viewInterviewResults(session: LiveMockInterviewSession) {
        viewModelScope.launch {
            _uiState.value = LiveInterviewUiState.LoadingNextQuestion // Reusing loading state visuals or create a specific one
            
            val questionsResult = repository.getQuestionsForSession(session.id)
            if (questionsResult.isSuccess) {
                val questions = questionsResult.getOrNull() ?: emptyList()
                
                // Calculate summary
                val summaryResult = repository.completeInterview(session.id, questions)
                if (summaryResult.isSuccess) {
                    _uiState.value = LiveInterviewUiState.Completed(summaryResult.getOrNull()!!)
                } else {
                    _errorMessage.value = "Failed to load interview summary"
                    _uiState.value = LiveInterviewUiState.Setup
                }
            } else {
                _errorMessage.value = "Failed to load interview questions"
                _uiState.value = LiveInterviewUiState.Setup
            }
        }
    }
    
    // Store the last interview config for retry
    private var lastInterviewConfig: InterviewConfig? = null

    fun dismissCreditDialog() {
        _showCreditDialog.value = false
    }

    fun openPurchaseScreen() {
        // TODO: Navigation to purchase screen
        _showCreditDialog.value = false
    }

    private var timerJob: Job? = null
    private var currentAudioFile: File? = null
    private val answeredQuestions = mutableListOf<LiveQuestion>()
    private val userAnswers = mutableMapOf<Int, String>() // Index -> transcription
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

    // Starts batch interview
    fun startBatchInterview(config: InterviewConfig) {
        Log.d(TAG, "startBatchInterview() called with config: userId=${config.userId}, type=${config.interviewType}, count=${config.questionCount}")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Setting UI state to Starting")
                _uiState.value = LiveInterviewUiState.Starting
                _useBatchMode.value = true

                // Check if we are retrying a previous session
                var existingQuestions: List<LiveQuestion>? = null
                if (config.previousSessionId != null) {
                    val historyResult = repository.getQuestionsForSession(config.previousSessionId)
                    if (historyResult.isSuccess) {
                        existingQuestions = historyResult.getOrNull()
                        Log.d(TAG, "Retrieved ${existingQuestions?.size} existing questions for retry")
                    } else {
                        Log.w(TAG, "Failed to retrieve existing questions for retry", historyResult.exceptionOrNull())
                    }
                }

                val request = StartLiveInterviewRequest(
                    userId = config.userId,
                    interviewType = config.interviewType,
                    questionCount = config.questionCount,
                    industry = config.industry,
                    experienceLevel = config.experienceLevel,
                    skills = config.skills,
                    jobTitle = config.jobTitle,
                    jobDescription = config.jobDescription,
                    resumeContent = config.resumeContent,
                    existingQuestions = existingQuestions,
                    sessionId = config.previousSessionId
                )
                Log.d(TAG, "Created StartLiveInterviewRequest: $request")

                Log.d(TAG, "Calling repository.startBatchInterview()")
                val result = repository.startBatchInterview(request)
                Log.d(TAG, "Repository call completed. Success: ${result.isSuccess}")

                if (result.isSuccess) {
                    val startResult = result.getOrNull()!!
                    Log.d(TAG, "Batch start result received: sessionId=${startResult.sessionId}, questionCount=${startResult.questions.size}")

                    val session = LiveMockInterviewSession(
                        id = startResult.sessionId,
                        userId = config.userId,
                        interviewType = config.interviewType,
                        status = InterviewStatus.IN_PROGRESS,
                        targetQuestionCount = config.questionCount,
                        currentQuestionIndex = 0,
                        startedAt = System.currentTimeMillis(),
                        questions = startResult.questions,
                        jobTitle = config.jobTitle
                    )

                    _interviewSession.value = session
                    _allQuestions.value = startResult.questions
                    _currentQuestionIndex.value = 0
                    _currentQuestion.value = startResult.questions.firstOrNull()
                    _currentFeedback.value = null
                    _questionStartTime.value = System.currentTimeMillis()
                    
                    Log.d(TAG, "Setting UI state to AnswerCollection")
                    _uiState.value = LiveInterviewUiState.AnswerCollection
                    startTimer()

                    Log.d(TAG, "Batch interview started successfully: ${startResult.sessionId}")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to start batch interview"
                    Log.e(TAG, "Failed to start batch interview: $errorMsg", result.exceptionOrNull())
                    _errorMessage.value = errorMsg
                    Log.d(TAG, "Setting UI state back to Setup due to failure")
                    _uiState.value = LiveInterviewUiState.Setup
                }

            } catch (e: Exception) {
                if (e is com.phamnhantucode.aicareercoach.data.neon.NeonUserService.InsufficientCreditException) {
                    _showCreditDialog.value = true
                    _uiState.value = LiveInterviewUiState.Setup
                    return@launch
                }
                Log.e(TAG, "Exception in startBatchInterview()", e)
                _errorMessage.value = e.message ?: "An error occurred"
                Log.d(TAG, "Setting UI state back to Setup due to exception")
                _uiState.value = LiveInterviewUiState.Setup
            }
        }
    }

    // Starts live interview
    fun startInterview(config: InterviewConfig) {
        Log.d(TAG, "startInterview() called with config: userId=${config.userId}, type=${config.interviewType}, count=${config.questionCount}, batchMode=${config.useBatchMode}")
        
        // Save config for retry
        lastInterviewConfig = config
        
        if (config.useBatchMode) {
            startBatchInterview(config)
        } else {
            startImmediateFeedbackInterview(config)
        }
    }
    
    // Restarts the current session with the exact same questions
    fun restartSession() {
        val session = _interviewSession.value
        // Get questions from either allQuestions (batch) or answeredQuestions (immediate)
        val questionsToReuse = if (_allQuestions.value.isNotEmpty()) {
            _allQuestions.value
        } else {
            answeredQuestions.map { it.copy(userAnswer = null, feedback = null, rating = null) }
        }

        if (session != null && questionsToReuse.isNotEmpty()) {
            Log.d(TAG, "Restarting session with ${questionsToReuse.size} existing questions")
            
            // 1. Save config and generic reset
            stopTimer()
            _interviewDuration.value = 0
            _currentFeedback.value = null
            userAnswers.clear()
            answeredQuestions.clear()

            // 2. Prepare clean existing questions
            val cleanQuestions = questionsToReuse.map { 
                it.copy(userAnswer = null, feedback = null, rating = null) 
            }
            
            // 3. Set generic Live data
            _allQuestions.value = cleanQuestions
            _currentQuestionIndex.value = 0
            _currentQuestion.value = cleanQuestions.first()
            _questionStartTime.value = System.currentTimeMillis()

            // 4. Create new session object based on previous one but reset
            _interviewSession.value = session.copy(
                status = InterviewStatus.IN_PROGRESS,
                currentQuestionIndex = 0,
                startedAt = System.currentTimeMillis(),
                questions = cleanQuestions
            )

            // 5. Navigate
            if (_useBatchMode.value) {
                _uiState.value = LiveInterviewUiState.AnswerCollection
            } else {
                _uiState.value = LiveInterviewUiState.ActiveQuestion
            }
            
            startTimer()
        } else {
            Log.w(TAG, "Cannot restart session: missing session or questions. Falling back to retryInterview.")
            retryInterview()
        }
    }

    // Retries the last interview with same settings (New Questions)
    fun retryInterview() {
        val config = lastInterviewConfig
        if (config != null) {
            Log.d(TAG, "Retrying interview with saved config (New Questions)")
            resetInterviewState()
            startInterview(config)
        } else {
            Log.w(TAG, "No saved config for retry, going back to setup")
            _uiState.value = LiveInterviewUiState.Setup
        }
    }
    
    // Checks if retry is available
    fun canRetry(): Boolean = lastInterviewConfig != null
    
    // Checks if restart (reuse questions) is available
    fun canRestart(): Boolean = _allQuestions.value.isNotEmpty() || answeredQuestions.isNotEmpty()
    
    private fun resetInterviewState() {
        stopTimer()
        _interviewDuration.value = 0
        _interviewSession.value = null
        _currentQuestion.value = null
        _currentFeedback.value = null
        _allQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        answeredQuestions.clear()
        userAnswers.clear()
    }

    // Starts immediate feedback interview
    private fun startImmediateFeedbackInterview(config: InterviewConfig) {
        Log.d(TAG, "startImmediateFeedbackInterview() called with config: userId=${config.userId}, type=${config.interviewType}, count=${config.questionCount}")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Setting UI state to Starting")
                _uiState.value = LiveInterviewUiState.Starting
                _useBatchMode.value = false

                // Check if we are retrying a previous session
                var existingQuestions: List<LiveQuestion>? = null
                if (config.previousSessionId != null) {
                    val historyResult = repository.getQuestionsForSession(config.previousSessionId)
                    if (historyResult.isSuccess) {
                        existingQuestions = historyResult.getOrNull()
                        Log.d(TAG, "Retrieved ${existingQuestions?.size} existing questions for retry")
                    } else {
                        Log.w(TAG, "Failed to retrieve existing questions for retry", historyResult.exceptionOrNull())
                    }
                }

                val request = StartLiveInterviewRequest(
                    userId = config.userId,
                    interviewType = config.interviewType,
                    questionCount = config.questionCount,
                    industry = config.industry,
                    experienceLevel = config.experienceLevel,
                    skills = config.skills,
                    jobTitle = config.jobTitle,
                    jobDescription = config.jobDescription,
                    resumeContent = config.resumeContent,
                    existingQuestions = existingQuestions,
                    sessionId = config.previousSessionId
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
                        startedAt = System.currentTimeMillis(),
                        jobTitle = config.jobTitle
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
                if (e is com.phamnhantucode.aicareercoach.data.neon.NeonUserService.InsufficientCreditException) {
                    _showCreditDialog.value = true
                    _uiState.value = LiveInterviewUiState.Setup
                    return@launch
                }
                Log.e(TAG, "Exception in startImmediateFeedbackInterview()", e)
                _errorMessage.value = e.message ?: "An error occurred"
                Log.d(TAG, "Setting UI state back to Setup due to exception")
                _uiState.value = LiveInterviewUiState.Setup
            }
        }
    }

    // Starts audio recording
    fun startRecording() {
        recordingStartTime = System.currentTimeMillis()
        currentAudioFile = audioRecorder.startRecording()
        if (currentAudioFile == null) {
            _errorMessage.value = "Failed to start recording. Please check microphone permissions."
        }
    }

    // Stops recording and processes answer
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

                if (_useBatchMode.value) {
                    // Batch mode: just transcribe and save answer
                    processBatchAnswer(audioFile, question, session)
                } else {
                    // Original mode: immediate feedback
                    processImmediateFeedback(audioFile, question, session)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing answer", e)
                _errorMessage.value = e.message ?: "An error occurred while processing your answer"
                _uiState.value = if (_useBatchMode.value) LiveInterviewUiState.AnswerCollection else LiveInterviewUiState.ActiveQuestion
            }
        }
    }

    private suspend fun processBatchAnswer(audioFile: File, question: LiveQuestion, session: LiveMockInterviewSession) {
        // Pre-validate audio file
        if (!audioFile.exists() || audioFile.length() <= 44) {
            Log.e(TAG, "Audio file validation failed: exists=${audioFile.exists()}, size=${audioFile.length()}")
            audioFile.delete()
            _errorMessage.value = "Audio recording failed. Please try again."
            _uiState.value = LiveInterviewUiState.AnswerCollection
            return
        }

        Log.d(TAG, "Processing audio file for batch mode: ${audioFile.absolutePath}, size=${audioFile.length()} bytes")

        // Transcribe audio using Vosk
        val voskRecognizer = com.phamnhantucode.aicareercoach.data.audio.VoskSpeechRecognizer(getApplication())
        val transcriptionResult = voskRecognizer.transcribeAudio(audioFile)

        if (transcriptionResult.isFailure) {
            val error = transcriptionResult.exceptionOrNull()
            val userMessage = when {
                error?.message?.contains("corrupted") == true || error?.message?.contains("invalid") == true ->
                    "Audio file was corrupted. Please try recording again."
                error?.message?.contains("No speech detected") == true ->
                    "No speech detected. Please speak clearly into the microphone."
                error?.message?.contains("model not available") == true ->
                    "Speech recognition is initializing. Please wait and try again."
                error?.message?.contains("empty") == true ->
                    "Recording too short. Please speak for at least 2 seconds."
                else ->
                    "Transcription failed: ${error?.message ?: "Unknown error"}"
            }

            Log.e(TAG, "Transcription failed: ${error?.message}", error)
            audioFile.delete()
            _errorMessage.value = userMessage
            _uiState.value = LiveInterviewUiState.AnswerCollection
            return
        }

        val transcription = transcriptionResult.getOrNull()!!
        Log.d(TAG, "Audio transcribed for batch mode: $transcription")

        // Save user answer
        userAnswers[_currentQuestionIndex.value] = transcription

        // Save to database in background
        // repository.saveUserAnswer(question.id, session.id, transcription)

        // Clean up audio file
        audioFile.delete()

        // Move to review state instead of auto-advance
        _uiState.value = LiveInterviewUiState.ReviewingAnswer(transcription)
    }

    // Confirms answer and moves to next question
    fun confirmAnswer(finalAnswer: String) {
        val question = _currentQuestion.value ?: return
        val session = _interviewSession.value ?: return

        viewModelScope.launch {
            // Save user answer
            userAnswers[_currentQuestionIndex.value] = finalAnswer

            // Save to database in background
            repository.saveUserAnswer(question.id, session.id, finalAnswer)

            // Move to next question or finish
            val nextIndex = _currentQuestionIndex.value + 1
            val allQuestionsList = _allQuestions.value

            if (nextIndex >= allQuestionsList.size) {
                // All questions answered - start batch feedback processing
                processBatchFeedback(session.id, allQuestionsList)
            } else {
                // Move to next question
                _currentQuestionIndex.value = nextIndex
                _currentQuestion.value = allQuestionsList[nextIndex]
                _questionStartTime.value = System.currentTimeMillis()

                // Update session
                _interviewSession.value = session.copy(currentQuestionIndex = nextIndex)

                // Reset audio recorder for next question
                audioRecorder.reset()

                _uiState.value = LiveInterviewUiState.AnswerCollection
            }
        }
    }

    // Retakes recording
    fun retakeRecording() {
        _uiState.value = LiveInterviewUiState.AnswerCollection
    }

    private suspend fun processImmediateFeedback(audioFile: File, question: LiveQuestion, session: LiveMockInterviewSession) {
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
    }

    private suspend fun processBatchFeedback(sessionId: String, questions: List<LiveQuestion>) {
        _uiState.value = LiveInterviewUiState.GeneratingBatchFeedback

        try {
            // Prepare questions and answers for batch processing
            val questionsAndAnswers = questions.mapIndexed { index, question ->
                val userAnswer = userAnswers[index] ?: ""
                Triple(question, userAnswer, question.category)
            }

            // Process all answers at once
            val result = repository.processBatchAnswers(sessionId, questionsAndAnswers)

            if (result.isSuccess) {
                val updatedQuestions = result.getOrNull()!!
                answeredQuestions.clear()
                answeredQuestions.addAll(updatedQuestions)
                
                _uiState.value = LiveInterviewUiState.BatchFeedback(updatedQuestions)
                Log.d(TAG, "Batch feedback processing completed for ${updatedQuestions.size} questions")
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to process batch feedback"
                _uiState.value = LiveInterviewUiState.AnswerCollection
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in batch feedback processing", e)
            _errorMessage.value = e.message ?: "An error occurred while generating feedback"
            _uiState.value = LiveInterviewUiState.AnswerCollection
        }
    }

    // Continues to next question
    fun continueToNextQuestion() {
        val session = _interviewSession.value ?: return
        val nextIndex = session.currentQuestionIndex + 1

        if (nextIndex >= session.targetQuestionCount) {
             // If manual "Next" is clicked on last question, it should probably complete or show confirmation
             // For now, let's treat it as complete if we are at the end
             if (answeredQuestions.size >= session.targetQuestionCount) {
                 completeInterview()
             } else {
                 // Or loop back / stay? Let's just complete for now or maybe just do nothing if not all answered?
                 // If "Next" is "Skip", we might want to allow finishing even with skips?
                 // Let's assume Next on last question = Complete attempt
                 completeInterview()
             }
        } else {
            // Load next question
            loadQuestionAtIndex(nextIndex)
        }
    }

    // Go to previous question
    fun goToPreviousQuestion() {
        val session = _interviewSession.value ?: return
        val prevIndex = session.currentQuestionIndex - 1

        if (prevIndex >= 0) {
            loadQuestionAtIndex(prevIndex)
        }
    }

    private fun loadQuestionAtIndex(index: Int) {
         val session = _interviewSession.value ?: return
         
         viewModelScope.launch {
            try {
                // If moving away from current question, we might want to save draft or cancel recording?
                // audioRecorder.cancelRecording() // Ensure recording is stopped

                _uiState.value = LiveInterviewUiState.LoadingNextQuestion

                // Get question at index
                var targetQuestion: LiveQuestion? = null
                
                // 1. Check if we have pre-loaded questions (Restart/Reused mode/Batch)
                if (index < _allQuestions.value.size) {
                     targetQuestion = _allQuestions.value[index]
                }
                
                // 2. If not found in list, try feedback (Standard Immediate mode - harder for random access)
                // For Immediate mode, we usually only have history. 
                // But this feature is primarily for Batch/Live where we have the list.
                
                if (targetQuestion != null) {
                    _interviewSession.value = session.copy(currentQuestionIndex = index)
                    _currentQuestion.value = targetQuestion
                    _currentFeedback.value = null
                    _questionStartTime.value = System.currentTimeMillis()
                    
                    // Update index state
                    _currentQuestionIndex.value = index
                    
                    // Reset audio recorder state
                    audioRecorder.reset()
                    
                    // Check if we already have an answer for this question
                    val existingAnswer = userAnswers[index]
                    if (existingAnswer != null) {
                         _uiState.value = LiveInterviewUiState.ReviewingAnswer(existingAnswer)
                    } else {
                         _uiState.value = LiveInterviewUiState.AnswerCollection
                    }

                } else {
                    // Critical failure if we can't find question
                    _errorMessage.value = "Failed to load question"
                    _uiState.value = LiveInterviewUiState.AnswerCollection
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading question", e)
                _errorMessage.value = e.message ?: "An error occurred"
                _uiState.value = LiveInterviewUiState.AnswerCollection
            }
        }
    }

    // Pauses interview
    fun pauseInterview() {
        stopTimer()
        val session = _interviewSession.value ?: return
        _interviewSession.value = session.copy(status = InterviewStatus.PAUSED)
        _uiState.value = LiveInterviewUiState.Paused
    }

    // Resumes interview
    fun resumeInterview() {
        startTimer()
        val session = _interviewSession.value ?: return
        _interviewSession.value = session.copy(status = InterviewStatus.IN_PROGRESS)
        _uiState.value = LiveInterviewUiState.ActiveQuestion
    }

    // Completes interview
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

    // Toggles interview mode
    fun toggleInterviewMode(useBatch: Boolean) {
        _useBatchMode.value = useBatch
        Log.d(TAG, "Interview mode switched to: ${if (useBatch) "Batch" else "Immediate"}")
    }

    // Abandons interview
    fun abandonInterview() {
        stopTimer()
        viewModelScope.launch {
            audioRecorder.cancelRecording()
        }
        _uiState.value = LiveInterviewUiState.Setup
        _interviewSession.value = null
        _currentQuestion.value = null
        _currentFeedback.value = null
        _allQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        answeredQuestions.clear()
        userAnswers.clear()
    }

    // Clears error
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

// Live interview UI state
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
    
    // New states for batch processing
    object AnswerCollection : LiveInterviewUiState()  // Collecting answers without feedback
    data class ReviewingAnswer(val transcription: String) : LiveInterviewUiState() // Review answer before submitting
    object GeneratingBatchFeedback : LiveInterviewUiState()  // Processing all answers at once
    data class BatchFeedback(val questions: List<LiveQuestion>) : LiveInterviewUiState()  // Show all feedback
}

// Interview configuration
data class InterviewConfig(
    val userId: String,
    val interviewType: InterviewType = InterviewType.GENERAL,
    val questionCount: Int = 5,
    val industry: String? = null,
    val experienceLevel: Int? = null,
    val skills: List<String> = emptyList(),
    val useBatchMode: Boolean = true,
    val jobTitle: String = "",
    val jobDescription: String = "",
    val resumeId: String? = null,
    val resumeContent: String? = null,
    val previousSessionId: String? = null
)

// Question feedback
data class QuestionFeedback(
    val transcription: String,
    val feedback: String,
    val rating: Int,
    val nextQuestion: LiveQuestion? = null
)
