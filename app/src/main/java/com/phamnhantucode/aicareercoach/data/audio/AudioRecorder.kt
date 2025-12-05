package com.phamnhantucode.aicareercoach.data.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * AudioRecorder handles audio recording for live interviews using push-to-talk functionality.
 * Uses AudioRecord API to record directly to WAV format optimized for Vosk speech recognition.
 *
 * Recording format: 16kHz mono PCM WAV (matches Vosk requirements exactly)
 */
class AudioRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var currentOutputFile: File? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val recordingMutex = Mutex()

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE = 16000 // 16kHz for Vosk
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val AUDIO_FILE_PREFIX = "live_interview_audio_"
        private const val AUDIO_FILE_EXTENSION = ".wav"
    }

    /**
     * Starts recording audio. Creates a new WAV file in cache directory.
     * @return The output file where audio is being recorded, or null if failed to start
     */
    fun startRecording(): File? {
        if (_recordingState.value == RecordingState.RECORDING) {
            Log.w(TAG, "Already recording, ignoring start request")
            return currentOutputFile
        }

        try {
            // Calculate buffer size
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            ).coerceAtLeast(4096)

            // Initialize AudioRecord
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                _recordingState.value = RecordingState.ERROR
                return null
            }

            // Create output file in cache directory
            val cacheDir = context.cacheDir
            val outputFile = File(cacheDir, "$AUDIO_FILE_PREFIX${System.currentTimeMillis()}$AUDIO_FILE_EXTENSION")
            currentOutputFile = outputFile

            // Set state BEFORE starting coroutine to avoid race condition
            _recordingState.value = RecordingState.RECORDING
            audioRecord?.startRecording()
            Log.d(TAG, "Recording started: ${outputFile.absolutePath}")

            // Start recording in background coroutine
            recordingJob = coroutineScope.launch {
                recordToWav(audioRecord!!, outputFile, bufferSize)
            }

            return outputFile

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioRecord", e)
            audioRecord?.release()
            audioRecord = null
            _recordingState.value = RecordingState.ERROR
            return null
        }
    }

    /**
     * Stops recording and returns the recorded audio file.
     * @return The file containing the recorded audio, or null if no recording was in progress
     */
    suspend fun stopRecording(): File? = withContext(Dispatchers.IO) {
        recordingMutex.withLock {
            if (_recordingState.value != RecordingState.RECORDING) {
                Log.w(TAG, "Not recording, ignoring stop request")
                return@withContext null
            }

            // Capture file reference before any state changes
            val recordedFile = currentOutputFile

            // Change state FIRST so recording loop exits
            _recordingState.value = RecordingState.STOPPED

            return@withContext try {
                // Stop recording
                audioRecord?.stop()

                // Wait for recording job to complete and finalize file
                recordingJob?.join()
                recordingJob = null

                // Release AudioRecord
                audioRecord?.release()
                audioRecord = null

                Log.d(TAG, "Recording stopped: ${recordedFile?.absolutePath}")

                // Verify file exists and has content (WAV header is 44 bytes)
                if (recordedFile?.exists() == true && recordedFile.length() > 44) {
                    // Validate WAV file structure
                    if (isValidAudioFile(recordedFile)) {
                        Log.d(TAG, "Recording valid: ${recordedFile.length()} bytes")
                        recordedFile
                    } else {
                        Log.e(TAG, "Recorded file has invalid WAV structure")
                        _recordingState.value = RecordingState.ERROR
                        null
                    }
                } else {
                    Log.e(TAG, "Recorded file is empty or doesn't exist")
                    _recordingState.value = RecordingState.ERROR
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
                audioRecord?.release()
                audioRecord = null
                recordingJob?.cancel()
                recordingJob = null
                _recordingState.value = RecordingState.ERROR
                null
            }
        }
    }

    /**
     * Records audio data and writes as WAV file with proper RIFF headers.
     */
    private suspend fun recordToWav(
        recorder: AudioRecord,
        outputFile: File,
        bufferSize: Int
    ) = withContext(Dispatchers.IO) {
        var outputStream: FileOutputStream? = null

        try {
            outputStream = FileOutputStream(outputFile)
            val buffer = ByteArray(bufferSize)
            var totalBytesWritten = 0

            // Write placeholder WAV header (will update after recording completes)
            writeWavHeader(outputStream, 0)

            // Record audio data in chunks
            while (_recordingState.value == RecordingState.RECORDING) {
                val bytesRead = recorder.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesWritten += bytesRead
                } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "AudioRecord invalid operation")
                    break
                } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "AudioRecord bad value")
                    break
                }
            }

            outputStream.close()
            outputStream = null

            // Update WAV header with actual data size
            updateWavHeader(outputFile, totalBytesWritten)

            Log.d(TAG, "WAV file written: $totalBytesWritten bytes of audio data")

        } catch (e: Exception) {
            Log.e(TAG, "Error writing WAV file", e)
        } finally {
            outputStream?.close()
        }
    }

    /**
     * Writes WAV file header (RIFF format).
     */
    private fun writeWavHeader(out: FileOutputStream, dataSize: Int) {
        val channels = 1 // Mono
        val byteRate = SAMPLE_RATE * channels * 2 // 16-bit = 2 bytes per sample
        val blockAlign = channels * 2

        // RIFF header
        out.write("RIFF".toByteArray(Charsets.US_ASCII))
        out.write(intToLittleEndianBytes(36 + dataSize)) // File size - 8
        out.write("WAVE".toByteArray(Charsets.US_ASCII))

        // fmt subchunk
        out.write("fmt ".toByteArray(Charsets.US_ASCII))
        out.write(intToLittleEndianBytes(16)) // Subchunk1Size (16 for PCM)
        out.write(shortToLittleEndianBytes(1)) // AudioFormat (1 = PCM)
        out.write(shortToLittleEndianBytes(channels.toShort())) // NumChannels
        out.write(intToLittleEndianBytes(SAMPLE_RATE)) // SampleRate
        out.write(intToLittleEndianBytes(byteRate)) // ByteRate
        out.write(shortToLittleEndianBytes(blockAlign.toShort())) // BlockAlign
        out.write(shortToLittleEndianBytes(16)) // BitsPerSample

        // data subchunk
        out.write("data".toByteArray(Charsets.US_ASCII))
        out.write(intToLittleEndianBytes(dataSize)) // Subchunk2Size
    }

    /**
     * Updates WAV header with actual data size after recording completes.
     */
    private fun updateWavHeader(file: File, dataSize: Int) {
        try {
            val raf = RandomAccessFile(file, "rw")

            // Update file size in RIFF header (offset 4)
            raf.seek(4)
            raf.write(intToLittleEndianBytes(36 + dataSize))

            // Update data chunk size (offset 40)
            raf.seek(40)
            raf.write(intToLittleEndianBytes(dataSize))

            raf.close()

            Log.d(TAG, "WAV header updated: dataSize=$dataSize, totalSize=${36 + dataSize + 8}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update WAV header", e)
        }
    }

    /**
     * Validates that an audio file is properly formed WAV file.
     */
    private fun isValidAudioFile(file: File): Boolean {
        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(12)
                val bytesRead = stream.read(header)

                if (bytesRead < 12) {
                    Log.e(TAG, "File too small for valid WAV header")
                    return false
                }

                // Check RIFF header
                val riffMagic = String(header, 0, 4, Charsets.US_ASCII)
                val waveMagic = String(header, 8, 4, Charsets.US_ASCII)

                if (riffMagic != "RIFF") {
                    Log.e(TAG, "Invalid RIFF header: $riffMagic")
                    return false
                }

                if (waveMagic != "WAVE") {
                    Log.e(TAG, "Invalid WAVE header: $waveMagic")
                    return false
                }

                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate audio file", e)
            false
        }
    }

    /**
     * Converts an integer to 4-byte little-endian byte array.
     */
    private fun intToLittleEndianBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    /**
     * Converts a short to 2-byte little-endian byte array.
     */
    private fun shortToLittleEndianBytes(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }

    /**
     * Cancels the current recording and deletes the file.
     */
    suspend fun cancelRecording() = withContext(Dispatchers.IO) {
        recordingMutex.withLock {
            try {
                if (_recordingState.value == RecordingState.RECORDING) {
                    audioRecord?.stop()
                    recordingJob?.cancel()
                    recordingJob = null

                    audioRecord?.release()
                    audioRecord = null
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
    }

    /**
     * Releases resources. Call this when done with the recorder.
     */
    fun release() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingJob?.cancel()
            recordingJob = null
            coroutineScope.cancel()
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
    suspend fun reset() {
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
