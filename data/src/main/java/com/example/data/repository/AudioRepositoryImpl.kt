package com.example.data.repository

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import com.example.data.local.RecordingDao
import com.example.data.local.RecordingEntity
import com.example.data.mapper.toDomain
import com.example.domain.entity.AudioEntity
import com.example.domain.entity.Recording
import com.example.domain.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10

@Singleton
class AudioRepositoryImpl @Inject constructor(
    private val recordingDao: RecordingDao
) : AudioRepository {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var outputFile: File? = null
    private var isRecording = false

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5MB
        private const val TAG = "AudioRepository"
    }

    override fun getAllRecordingsFlow(): Flow<List<Recording>> {
        return recordingDao.getAllRecordingsFlow().map { entityList ->
            entityList.map { it.toDomain() }
        }
    }


    override fun startRecording(thresholdDb: Double): Flow<Double> = flow {
        val tmpFile = File.createTempFile("record_", ".m4a")
        outputFile = tmpFile

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(tmpFile.absolutePath)
            prepare()
            start()
        }
        isRecording = true

        Log.d(TAG, "startRecording: ${tmpFile.absolutePath}")

        // Real-time noise detection / file-size check
        while (isRecording) {
            val amplitude = mediaRecorder?.maxAmplitude ?: 0
            val db = if (amplitude > 0) 20 * log10(amplitude.toDouble()) else 0.0
            emit(db)

            // File size check
            if (tmpFile.length() > MAX_FILE_SIZE_BYTES) {
                Log.d(TAG, "File exceeded 5MB, stopping.")
                stopRecording()
                break
            }
            delay(300)
        }
    }

    override suspend fun stopRecording(): AudioEntity? = withContext(Dispatchers.IO) {
        if (!isRecording) return@withContext null
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
            val audio = AudioEntity(
                filePath = file.absolutePath,
                durationMillis = durationMs
            )
            Log.d(TAG, "stopRecording: $audio")

            // Insert into the DB
            val entity = RecordingEntity(
                filePath = file.absolutePath,
                timestamp = System.currentTimeMillis(),
                durationMillis = durationMs,
                isNoisy = false // default
            )
            val newId = recordingDao.insertRecording(entity)
            Log.d(TAG, "Inserted recording with id=$newId")

            return@withContext audio
        }
        return@withContext null
    }

    /**
     * A naive example of "noise reduction" which simply copies the file to a new name,
     * or if you had a WAV, you might do zeroing of samples or TarsosDSP.
     */
    override suspend fun reduceNoise(audio: AudioEntity): AudioEntity = withContext(Dispatchers.IO) {
        // If you want advanced noise reduction, do decode -> DSP -> re-encode
        val cleanedFile = File(audio.filePath.replace(".m4a", "_cleaned.m4a"))
        File(audio.filePath).copyTo(cleanedFile, overwrite = true)

        // Update the DB to reflect isNoisy=false
        // You’d need to query the recording by file path, then update the entity
        val existing = recordingDao.getRecordingByFilePath(audio.filePath)
        existing?.let {
            val updated = it.copy(
                filePath = cleanedFile.absolutePath,
                isNoisy = false
            )
            recordingDao.insertRecording(updated) // upsert
        }

        return@withContext audio.copy(filePath = cleanedFile.absolutePath, isNoisy = false)
    }

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
        return mediaPlayer?.currentPosition ?: 0
    }

    override fun getDuration(audio: AudioEntity): Int {
        return mediaPlayer?.duration ?: audio.durationMillis.toInt()
    }

    override suspend fun deleteAudio(audio: AudioEntity): Boolean = withContext(Dispatchers.IO) {
        val file = File(audio.filePath)
        val success = file.exists() && file.delete()

        // Also remove from DB
        val existing = recordingDao.getRecordingByFilePath(audio.filePath)
        existing?.let {
            recordingDao.deleteRecording(it.id)
        }

        success
    }

    override suspend fun getLastRecordedAudio(): AudioEntity? = withContext(Dispatchers.IO) {
        outputFile?.let {
            AudioEntity(filePath = it.absolutePath, durationMillis = estimateDuration(it))
        }
    }

    // Helper function to estimate duration (similar to your code)
    private fun estimateDuration(file: File): Long {
        val mp = MediaPlayer().apply { setDataSource(file.absolutePath) }
        mp.prepare()
        val duration = mp.duration.toLong()
        mp.release()
        return duration
    }
}
