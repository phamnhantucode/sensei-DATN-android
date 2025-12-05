package com.phamnhantucode.aicareercoach.data.audio

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * VoskSpeechRecognizer provides offline speech-to-text using Vosk library.
 * This is more accurate for real speech transcription compared to Gemini's audio processing.
 * 
 * The model is downloaded on first use (~40MB) and cached locally.
 */
class VoskSpeechRecognizer(private val context: Context) {

    private var model: Model? = null
    private var isModelLoaded = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "VoskSpeechRecognizer"
        private const val MODEL_NAME = "vosk-model-small-en-us-0.15"
        private const val MODEL_URL = "https://alphacephei.com/vosk/models/$MODEL_NAME.zip"
        private const val SAMPLE_RATE = 16000f
    }

    /**
     * Initialize the Vosk model. Downloads if not present.
     */
    suspend fun initModel(): Boolean = withContext(Dispatchers.IO) {
        if (isModelLoaded && model != null) {
            return@withContext true
        }

        try {
            val modelDir = File(context.filesDir, MODEL_NAME)
            
            if (!modelDir.exists() || !File(modelDir, "am/final.mdl").exists()) {
                Log.d(TAG, "Vosk model not found, downloading...")
                val downloaded = downloadAndExtractModel(modelDir)
                if (!downloaded) {
                    Log.e(TAG, "Failed to download Vosk model")
                    return@withContext false
                }
            }

            Log.d(TAG, "Loading Vosk model from: ${modelDir.absolutePath}")
            model = Model(modelDir.absolutePath)
            isModelLoaded = true
            Log.d(TAG, "Vosk model loaded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Vosk model", e)
            isModelLoaded = false
            false
        }
    }

    /**
     * Download and extract the Vosk model.
     */
    private fun downloadAndExtractModel(destDir: File): Boolean {
        return try {
            val zipFile = File(context.cacheDir, "$MODEL_NAME.zip")
            
            // Download model
            Log.d(TAG, "Downloading model from: $MODEL_URL")
            val request = Request.Builder().url(MODEL_URL).build()
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to download model: ${response.code}")
                return false
            }

            response.body?.byteStream()?.use { input ->
                FileOutputStream(zipFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Model downloaded: ${zipFile.length()} bytes")

            // Extract zip
            Log.d(TAG, "Extracting model...")
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    // Remove the top-level directory from path
                    val entryName = entry.name.removePrefix("$MODEL_NAME/")
                    if (entryName.isNotEmpty()) {
                        val outFile = File(destDir, entryName)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Cleanup zip file
            zipFile.delete()
            Log.d(TAG, "Model extraction completed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download/extract model", e)
            false
        }
    }

    /**
     * Transcribe audio file to text using Vosk.
     * @param audioFile The audio file (WAV format, 16kHz mono PCM)
     * @return Transcribed text or error
     */
    suspend fun transcribeAudio(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isModelLoaded || model == null) {
                val loaded = initModel()
                if (!loaded) {
                    return@withContext Result.failure(Exception("Speech recognition model not available. Please check internet connection."))
                }
            }

            // Validate file exists and has minimum content (WAV header is 44 bytes)
            if (!audioFile.exists() || audioFile.length() <= 44) {
                Log.e(TAG, "Audio file is empty or invalid: exists=${audioFile.exists()}, size=${audioFile.length()}")
                return@withContext Result.failure(Exception("Audio file is empty or invalid"))
            }

            // Validate WAV file structure
            if (!isValidWavFile(audioFile)) {
                Log.e(TAG, "Audio file is not a valid WAV file or is corrupted")
                return@withContext Result.failure(Exception("Audio file is corrupted. Please try recording again."))
            }

            Log.d(TAG, "Transcribing WAV file: ${audioFile.absolutePath}, size: ${audioFile.length()} bytes")

            // Process WAV file directly (no conversion needed)
            val recognizer = Recognizer(model, SAMPLE_RATE)
            val inputStream = FileInputStream(audioFile)
            val buffer = ByteArray(4096)
            var bytesRead: Int

            val transcriptionBuilder = StringBuilder()

            // Skip WAV header (44 bytes)
            inputStream.skip(44)

            // Process audio data in chunks
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    val result = recognizer.result
                    val text = parseVoskResult(result)
                    if (text.isNotBlank()) {
                        transcriptionBuilder.append(text).append(" ")
                    }
                }
            }

            // Get final result
            val finalResult = recognizer.finalResult
            val finalText = parseVoskResult(finalResult)
            if (finalText.isNotBlank()) {
                transcriptionBuilder.append(finalText)
            }

            inputStream.close()
            recognizer.close()

            val transcription = transcriptionBuilder.toString().trim()

            if (transcription.isBlank()) {
                Log.w(TAG, "No speech detected in audio file")
                return@withContext Result.failure(Exception("No speech detected. Please speak clearly and try again."))
            }

            Log.d(TAG, "Transcription successful: $transcription")
            Result.success(transcription)

        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing audio", e)
            Result.failure(Exception("Transcription failed: ${e.message}"))
        }
    }

    /**
     * Validates that a file is a properly formed WAV file.
     */
    private fun isValidWavFile(file: File): Boolean {
        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(44)
                val bytesRead = stream.read(header)

                if (bytesRead < 44) {
                    Log.e(TAG, "File too small to be valid WAV (< 44 bytes)")
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

                // Verify sample rate (should be 16000 for Vosk)
                val sampleRate = java.nio.ByteBuffer.wrap(header, 24, 4)
                    .order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    .int

                if (sampleRate != 16000) {
                    Log.w(TAG, "WAV sample rate is $sampleRate Hz, expected 16000 Hz (Vosk may still process it)")
                }

                Log.d(TAG, "WAV file validation passed: sample rate=$sampleRate Hz")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate WAV file", e)
            false
        }
    }

    /**
     * Convert M4A/AAC audio to WAV format for Vosk processing.
     * @deprecated No longer needed - AudioRecorder now records directly to WAV format.
     * Kept for backwards compatibility with old M4A files if needed.
     */
    @Deprecated("No longer needed - recording directly to WAV format")
    private fun convertToWav(inputFile: File): File? {
        return try {
            val outputFile = File(context.cacheDir, "vosk_input_${System.currentTimeMillis()}.wav")
            
            val extractor = android.media.MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1) {
                Log.e(TAG, "No audio track found in file")
                extractor.release()
                return null
            }

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: return null
            val sampleRate = format.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT)

            val decoder = android.media.MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            val pcmData = mutableListOf<Byte>()
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            var isEOS = false

            while (!isEOS) {
                val inputBufferIndex = decoder.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                    val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer?.get(chunk)
                    outputBuffer?.clear()
                    pcmData.addAll(chunk.toList())
                    decoder.releaseOutputBuffer(outputBufferIndex, false)
                }
            }

            decoder.stop()
            decoder.release()
            extractor.release()

            // Resample to 16kHz mono for Vosk
            val pcmBytes = pcmData.toByteArray()
            val resampledPcm = resampleTo16kMono(pcmBytes, sampleRate, channelCount)

            // Write WAV file
            writeWavFile(outputFile, resampledPcm, 16000, 1)

            Log.d(TAG, "Converted audio to WAV: ${outputFile.absolutePath}, size: ${outputFile.length()}")
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert audio to WAV", e)
            null
        }
    }

    private fun resampleTo16kMono(pcmData: ByteArray, originalSampleRate: Int, channelCount: Int): ByteArray {
        val shortBuffer = java.nio.ByteBuffer.wrap(pcmData)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        val samples = ShortArray(shortBuffer.remaining())
        shortBuffer.get(samples)

        // Convert to mono if stereo
        val monoSamples = if (channelCount == 2) {
            ShortArray(samples.size / 2) { i ->
                ((samples[i * 2].toInt() + samples[i * 2 + 1].toInt()) / 2).toShort()
            }
        } else {
            samples
        }

        // Resample to 16kHz
        val resampleRatio = 16000.0 / originalSampleRate
        val newLength = (monoSamples.size * resampleRatio).toInt()
        val resampled = ShortArray(newLength) { i ->
            val srcIndex = (i / resampleRatio).toInt().coerceIn(0, monoSamples.size - 1)
            monoSamples[srcIndex]
        }

        val byteBuffer = java.nio.ByteBuffer.allocate(resampled.size * 2)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        resampled.forEach { byteBuffer.putShort(it) }
        return byteBuffer.array()
    }

    private fun writeWavFile(file: File, pcmData: ByteArray, sampleRate: Int, channels: Int) {
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size
        val fileSize = 36 + dataSize

        file.outputStream().use { out ->
            out.write("RIFF".toByteArray())
            out.write(intToBytes(fileSize))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intToBytes(16))
            out.write(shortToBytes(1))
            out.write(shortToBytes(channels.toShort()))
            out.write(intToBytes(sampleRate))
            out.write(intToBytes(byteRate))
            out.write(shortToBytes(blockAlign.toShort()))
            out.write(shortToBytes(bitsPerSample.toShort()))
            out.write("data".toByteArray())
            out.write(intToBytes(dataSize))
            out.write(pcmData)
        }
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToBytes(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }

    private fun parseVoskResult(jsonResult: String): String {
        return try {
            val json = JSONObject(jsonResult)
            json.optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }

    fun release() {
        model?.close()
        model = null
        isModelLoaded = false
    }
}
