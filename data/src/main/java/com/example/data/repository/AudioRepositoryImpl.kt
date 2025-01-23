package com.example.data.repository

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.data.local.RecordingDao
import com.example.data.local.RecordingEntity
import com.example.data.mapper.toDomain
import com.example.data.utils.wavutils.AmplitudeGate
import com.example.data.utils.wavutils.WavFileReader
import com.example.data.utils.wavutils.WavFileWriter
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
        return recordingDao.getAllRecordingsFlow().map { entities ->
            entities.map { it.toDomain() }
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

        while (isRecording) {
            val amplitude = mediaRecorder?.maxAmplitude ?: 0
            val db = if (amplitude > 0) 20 * log10(amplitude.toDouble()) else 0.0
            emit(db)

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

        val file = outputFile ?: return@withContext null
        val durationMs = estimateDuration(file)
        val audio = AudioEntity(filePath = file.absolutePath, durationMillis = durationMs)
        Log.d(TAG, "stopRecording: $audio")

        val entity = RecordingEntity(
            filePath = file.absolutePath,
            timestamp = System.currentTimeMillis(),
            durationMillis = durationMs,
            isNoisy = false
        )

        recordingDao.insertRecording(entity)

        return@withContext audio
    }

    override suspend fun reduceNoise(audio: AudioEntity): AudioEntity = withContext(Dispatchers.IO) {
        val inputM4A = File(audio.filePath)
        if (!inputM4A.exists()) return@withContext audio

        // Decode to WAV
        val decodedWavFile = decodeM4AToWav(inputM4A) ?: return@withContext fallbackCopy(audio)

        // Apply noise gating
        val gateOutput = File.createTempFile("cleaned_", ".wav", inputM4A.parentFile)
        applyAmplitudeGate(decodedWavFile, gateOutput)

        // Recalculate duration
        val newDuration = estimateDuration(gateOutput)

        if (newDuration == 0L) {
            Log.e(TAG, "reduceNoise: Gated file has zero duration!")
            return@withContext fallbackCopy(audio)
        }

        // Update database
        val existing = recordingDao.getRecordingByFilePath(audio.filePath)
        if (existing != null) {
            val updated = existing.copy(filePath = gateOutput.absolutePath, durationMillis = newDuration, isNoisy = false)
            recordingDao.insertRecording(updated)
        }

        return@withContext audio.copy(filePath = gateOutput.absolutePath, durationMillis = newDuration, isNoisy = false)
    }


    private suspend fun fallbackCopy(audio: AudioEntity): AudioEntity  = withContext(Dispatchers.IO){
        val fallbackFile = File(audio.filePath.replace(".m4a", "_naive.m4a"))
        File(audio.filePath).copyTo(fallbackFile, overwrite = true)

        val existing = recordingDao.getRecordingByFilePath(audio.filePath)
        if (existing != null) {
            val updated = existing.copy(filePath = fallbackFile.absolutePath, isNoisy = false)
            recordingDao.insertRecording(updated)
        }
        return@withContext audio.copy(filePath = fallbackFile.absolutePath, isNoisy = false)
    }

    private fun decodeM4AToWav(input: File): File? {
        val outputWav = File.createTempFile("decoded_", ".wav", input.parentFile)
        val cmd = "-y -i ${input.absolutePath} -acodec pcm_s16le -ac 1 -ar 44100 ${outputWav.absolutePath}"
        val session = FFmpegKit.execute(cmd)
        val returnCode = session.returnCode
        return if (ReturnCode.isSuccess(returnCode)) {
            Log.d(TAG, "decodeM4AToWav success => ${outputWav.absolutePath}")
            outputWav
        } else {
            Log.e(TAG, "decodeM4AToWav fail => code=$returnCode logs=${session.allLogsAsString}")
            null
        }
    }

    private fun applyAmplitudeGate(inputWav: File, outputWav: File) {
        // read wav => short array
        val (format, samples) = WavFileReader.readWav(inputWav)
        // apply gating
        // e.g. threshold=200 => below that = 0
        AmplitudeGate.applyAmplitudeGate(samples, 30)

        // write out
        WavFileWriter.writeWav(outputWav, format, samples)
    }


    override fun playAudio(audio: AudioEntity, play: Boolean) {
        if (play) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audio.filePath)
                prepare()
                start()
            }
        } else {
            mediaPlayer?.pause()
        }
    }

    override fun getCurrentPosition(audio: AudioEntity): Int {
        // Option A: try/catch
        val mp = mediaPlayer ?: return 0
        return try {
            mp.currentPosition
        } catch (e: IllegalStateException) {
            Log.e(TAG, "getCurrentPosition: MediaPlayer invalid state => $e")
            0
        }
    }


    override fun getDuration(audio: AudioEntity): Int {
        return mediaPlayer?.duration ?: audio.durationMillis.toInt()
    }

    override suspend fun deleteAudio(audio: AudioEntity): Boolean = withContext(Dispatchers.IO) {
        val file = File(audio.filePath)
        val success = file.exists() && file.delete()
        val existing = recordingDao.getRecordingByFilePath(audio.filePath)
        if (existing != null) {
            recordingDao.deleteRecording(existing.id)
        }
        success
    }

    override suspend fun getLastRecordedAudio(): AudioEntity? = withContext(Dispatchers.IO) {
        outputFile?.let {
            AudioEntity(it.absolutePath, estimateDuration(it))
        }
    }

    private fun estimateDuration(file: File): Long {
        val mp = MediaPlayer().apply { setDataSource(file.absolutePath) }
        mp.prepare()
        val duration = mp.duration.toLong()
        mp.release()
        return duration
    }
}
