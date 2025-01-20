package com.example.data.repository

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import com.example.domain.entity.AudioEntity
import com.example.domain.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10

/**
 * Implementation of [AudioRepository] that uses [MediaRecorder] for recording
 * audio in MPEG-4/AAC format, and [MediaPlayer] for playback.
 */
@Singleton
class AudioRepositoryImpl @Inject constructor(
    // You could inject Context or other dependencies if needed
) : AudioRepository {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var outputFile: File? = null
    private var isRecording = false

    /**
     * Start recording audio, emitting decibel (dB) values in real-time.
     *
     * @param thresholdDb A noise threshold for potential UI warnings.
     * @return A [Flow] of Double, each representing the current noise level in dB.
     */
    override fun startRecording(thresholdDb: Double): Flow<Double> = flow {
        // Create a temporary output file. In a real app, you may want a specific folder.
        val tmpFile = File.createTempFile("record_", ".m4a")
        outputFile = tmpFile

        // Initialize and prepare the MediaRecorder
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(tmpFile.absolutePath)
            prepare()  // May throw IOException
            start()    // Begin recording
        }
        isRecording = true

        Log.d("AudioRepository", "startRecording: ${tmpFile.absolutePath}")

        /**
         * Real-time loop to measure amplitude and emit approximate dB values every 300ms.
         * In an actual app, you might measure more/less frequently, or handle this differently.
         */
        while (isRecording) {
            val amplitude = mediaRecorder?.maxAmplitude ?: 0
            // Very approximate decibel calculation: 20 * log10(amplitude / reference)
            // We use amplitude directly here; you might calibrate with a known reference (e.g. 2700).
            val db = if (amplitude > 0) 20 * log10(amplitude.toDouble()) else 0.0

            emit(db)

            // Optional: If you need to auto-stop after a certain threshold or length:
            // e.g., checkFileSizeOrDuration(tmpFile)
            // If we detect it's over 5MB or over 1 minute, we can stopRecording()

            // Sleep briefly to avoid busy loop
            kotlinx.coroutines.delay(300)
        }
    }

    /**
     * Stop the ongoing recording and release resources.
     *
     * @return An [AudioEntity] with metadata (file path, duration), or null if no recording.
     */
    override suspend fun stopRecording(): AudioEntity? = withContext(Dispatchers.IO) {
        if (!isRecording) return@withContext null

        // Mark recording as stopped
        isRecording = false

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null

        outputFile?.let { file ->
            val durationMs = estimateDuration(file)
            val audioEntity = AudioEntity(
                filePath = file.absolutePath,
                durationMillis = durationMs
            )
            Log.d("AudioRepository", "stopRecording: $audioEntity")
            return@withContext audioEntity
        }
        return@withContext null
    }

    /**
     * A naive example of "noise reduction," which simply copies the file
     * to a new name with "_cleaned" appended. No actual DSP is performed here.
     *
     * In a real implementation, you'd process raw audio data or integrate
     * a library like RNNoise, FFT-based filtering, or other post-processing.
     */
    override suspend fun reduceNoise(audio: AudioEntity): AudioEntity {
        // For demonstration, we just copy the file to something like "record_cleaned.m4a".
        val cleanedFile = File(audio.filePath.replace(".m4a", "_cleaned.m4a"))
        File(audio.filePath).copyTo(cleanedFile, overwrite = true)

        // Return a copy of the entity pointing to the new file, marking isNoisy=false
        return audio.copy(filePath = cleanedFile.absolutePath, isNoisy = false)
    }

    /**
     * Play a given audio file using [MediaPlayer].
     * If already playing, we stop the previous MediaPlayer instance.
     */
    override fun playAudio(audio: AudioEntity, play: Boolean) {
        if (play) {
            // Start or resume playback
            mediaPlayer?.stop()
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audio.filePath)
                prepare()
                start()
            }
        } else {
            // Pause if currently playing
            mediaPlayer?.pause()
        }
    }

    override fun getCurrentPosition(audio: AudioEntity): Int {
        // Return current playback position in ms
        return mediaPlayer?.currentPosition ?: 0
    }

    override fun getDuration(audio: AudioEntity): Int {
        // Return total duration in ms
        return mediaPlayer?.duration ?: audio.durationMillis.toInt()
    }


    /**
     * Delete the recorded audio file from storage.
     * Returns true if deletion succeeded.
     */
    override suspend fun deleteAudio(audio: AudioEntity): Boolean = withContext(Dispatchers.IO) {
        val file = File(audio.filePath)
        return@withContext if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Optionally retrieve the last recorded audio file. This is a placeholder
     * approach that just references our 'outputFile' if set.
     *
     * In a real-world scenario, you might store references in a database or shared prefs.
     */
    override suspend fun getLastRecordedAudio(): AudioEntity? = withContext(Dispatchers.IO) {
        outputFile?.let {
            AudioEntity(filePath = it.absolutePath, durationMillis = estimateDuration(it))
        }
    }

    /**
     * A helper function to estimate duration of the recording by opening
     * the file with a [MediaPlayer]. You could also estimate via file size or
     * raw data analysis if needed.
     */
    private fun estimateDuration(file: File): Long {
        val mp = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare() // Might throw IOException
        }
        val duration = mp.duration.toLong()
        mp.release()
        return duration
    }

    /**
     * (Optional) Check if the file is too large or if recording is too long,
     * and auto-stop if needed. Example usage:
     *
     *   if (checkFileSizeOrDuration(file)) { stopRecording() }
     *
     * Not fully implemented here, but you could measure file.size or
     * keep track of elapsed time to enforce a 5MB or 1-minute limit.
     */
    private fun checkFileSizeOrDuration(file: File) {
        val maxSizeBytes = 5 * 1024 * 1024 // 5MB
        val maxDurationMillis = 60_000L    // 1 minute

        val fileSize = file.length()
        // If fileSize > maxSizeBytes, handle...
        // If we track startTime, we can see if (System.currentTimeMillis() - startTime) > maxDurationMillis
    }
}
