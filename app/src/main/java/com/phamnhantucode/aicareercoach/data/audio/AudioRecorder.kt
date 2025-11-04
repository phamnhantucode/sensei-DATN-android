package com.phamnhantucode.aicareercoach.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

/**
 * AudioRecorder handles audio recording for live interviews using push-to-talk functionality.
 * Supports Android API 31+ with MediaRecorder and fallback for older APIs.
 */
class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    companion object {
        private const val TAG = "AudioRecorder"
        private const val AUDIO_FILE_PREFIX = "live_interview_audio_"
        private const val AUDIO_FILE_EXTENSION = ".m4a"
    }

    /**
     * Starts recording audio. Creates a new audio file in cache directory.
     * @return The output file where audio is being recorded, or null if failed to start
     */
    fun startRecording(): File? {
        if (_recordingState.value == RecordingState.RECORDING) {
            Log.w(TAG, "Already recording, ignoring start request")
            return currentOutputFile
        }

        try {
            // Create output file in cache directory
            val cacheDir = context.cacheDir
            val outputFile = File(cacheDir, "$AUDIO_FILE_PREFIX${System.currentTimeMillis()}$AUDIO_FILE_EXTENSION")

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)

                try {
                    prepare()
                    start()
                    currentOutputFile = outputFile
                    _recordingState.value = RecordingState.RECORDING
                    Log.d(TAG, "Recording started: ${outputFile.absolutePath}")
                    return outputFile
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to start recording", e)
                    release()
                    _recordingState.value = RecordingState.ERROR
                    return null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaRecorder", e)
            _recordingState.value = RecordingState.ERROR
            return null
        }
    }

    /**
     * Stops recording and returns the recorded audio file.
     * @return The file containing the recorded audio, or null if no recording was in progress
     */
    fun stopRecording(): File? {
        if (_recordingState.value != RecordingState.RECORDING) {
            Log.w(TAG, "Not recording, ignoring stop request")
            return null
        }

        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _recordingState.value = RecordingState.STOPPED

            val recordedFile = currentOutputFile
            Log.d(TAG, "Recording stopped: ${recordedFile?.absolutePath}")

            // Verify file exists and has content
            if (recordedFile?.exists() == true && recordedFile.length() > 0) {
                recordedFile
            } else {
                Log.e(TAG, "Recorded file is empty or doesn't exist")
                _recordingState.value = RecordingState.ERROR
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            _recordingState.value = RecordingState.ERROR
            null
        }
    }

    /**
     * Cancels the current recording and deletes the file.
     */
    fun cancelRecording() {
        try {
            if (_recordingState.value == RecordingState.RECORDING) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
            }

            // Delete the file
            currentOutputFile?.delete()
            currentOutputFile = null

            _recordingState.value = RecordingState.IDLE
            Log.d(TAG, "Recording cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling recording", e)
            _recordingState.value = RecordingState.ERROR
        }
    }

    /**
     * Releases resources. Call this when done with the recorder.
     */
    fun release() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
            _recordingState.value = RecordingState.IDLE
            Log.d(TAG, "AudioRecorder released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecorder", e)
        }
    }

    /**
     * Cleans up old audio files from cache directory to free up space.
     * @param olderThanMillis Delete files older than this duration in milliseconds (default: 1 hour)
     */
    fun cleanupOldFiles(olderThanMillis: Long = 3600000) {
        try {
            val cacheDir = context.cacheDir
            val currentTime = System.currentTimeMillis()

            cacheDir.listFiles { file ->
                file.name.startsWith(AUDIO_FILE_PREFIX) &&
                file.name.endsWith(AUDIO_FILE_EXTENSION)
            }?.forEach { file ->
                if (currentTime - file.lastModified() > olderThanMillis) {
                    val deleted = file.delete()
                    Log.d(TAG, "Cleaned up old file: ${file.name}, deleted: $deleted")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old files", e)
        }
    }

    /**
     * Resets the recorder to idle state without releasing resources.
     */
    fun reset() {
        if (_recordingState.value == RecordingState.RECORDING) {
            stopRecording()
        }
        currentOutputFile = null
        _recordingState.value = RecordingState.IDLE
    }
}

/**
 * Represents the current state of the audio recorder.
 */
enum class RecordingState {
    IDLE,       // Not recording, ready to start
    RECORDING,  // Currently recording audio
    STOPPED,    // Recording finished, file available
    ERROR       // An error occurred during recording
}
