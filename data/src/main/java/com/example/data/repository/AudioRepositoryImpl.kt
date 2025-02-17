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

        // Start recording using MediaRecorder.
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

        // Create a new RecordingEntity and update the database.
        val entity = RecordingEntity(
            filePath = file.absolutePath,
            timestamp = System.currentTimeMillis(),
            durationMillis = durationMs,
            isNoisy = false
        )
        recordingDao.upsertRecording(entity)
        return@withContext audio
    }

    override suspend fun reduceNoise(audio: AudioEntity): AudioEntity = withContext(Dispatchers.IO) {
        val inputFile = File(audio.filePath)
        if (!inputFile.exists()) return@withContext audio

        // Decode the input file (M4A) to a WAV file.
        val decodedWavFile = decodeM4AToWav(inputFile) ?: return@withContext fallbackCopy(audio)

        // Create a temporary output file for the noise-reduced (gated) WAV.
        val tempOutput = File.createTempFile("cleaned_", ".wav", inputFile.parentFile)
        applyAmplitudeGate(decodedWavFile, tempOutput)

        // Recalculate the duration of the cleaned file.
        val newDuration = estimateDuration(tempOutput)
        if (newDuration == 0L) {
            Log.e(TAG, "reduceNoise: Gated file has zero duration!")
            return@withContext fallbackCopy(audio)
        }

        // Generate the new file name using the original file’s base name and noise reduction count.
        val newFile = getNextNoiseReducedFile(inputFile)
        if (!tempOutput.renameTo(newFile)) {
            Log.e(TAG, "reduceNoise: Failed to rename noise reduced file")
            return@withContext fallbackCopy(audio)
        }

        // Update the database entry using the absolute path.
        val existing = recordingDao.getRecordingByFilePath(audio.filePath)
        if (existing != null) {
            val updated = existing.copy(
                filePath = newFile.absolutePath,
                durationMillis = newDuration,
                isNoisy = false
            )
            recordingDao.upsertRecording(updated)
        }
        // Return the updated AudioEntity with the absolute path.
        return@withContext audio.copy(filePath = newFile.absolutePath, durationMillis = newDuration, isNoisy = false)
    }


    private suspend fun fallbackCopy(audio: AudioEntity): AudioEntity = withContext(Dispatchers.IO) {
        val originalFile = File(audio.filePath)
        // Generate a fallback file name using the same noise reduction naming logic.
        val fallbackFile = getNextNoiseReducedFile(originalFile)
        originalFile.copyTo(fallbackFile, overwrite = true)

        val existing = recordingDao.getRecordingByFilePath(audio.filePath)
        if (existing != null) {
            val updated = existing.copy(filePath = fallbackFile.absolutePath, isNoisy = false)
            recordingDao.upsertRecording(updated)
        }
        return@withContext audio.copy(filePath = fallbackFile.absolutePath, isNoisy = false)
    }

    /**
     * Generates a new file name by appending an incrementing counter in parentheses to the
     * original file’s base name (without extension). For example:
     *
     *   - If the original file is "MyRecording" then the first noise reduction will be "MyRecording(1)".
     *   - If the original file is "MyRecording(1)" then the next will be "MyRecording(2)".
     */
    private fun getNextNoiseReducedFile(originalFile: File): File {
        val parent = originalFile.parentFile
        val originalName = originalFile.nameWithoutExtension

        // Look for a trailing count pattern, for example: MyRecording(1)
        val regex = Regex("^(.*)\\((\\d+)\\)$")
        val matchResult = regex.find(originalName)
        val baseName: String
        val startCount: Int

        if (matchResult != null) {
            // Extract the base name (without the count) and the current count.
            baseName = matchResult.groupValues[1].trim()
            startCount = matchResult.groupValues[2].toIntOrNull() ?: 0
        } else {
            baseName = originalName
            startCount = 0
        }

        // Determine the new count: if a count exists, increment it; otherwise start at 1.
        var counter = if (startCount > 0) startCount + 1 else 1
        var newFile = File(parent, "$baseName($counter)")
        while (newFile.exists()) {
            counter++
            newFile = File(parent, "$baseName($counter)")
        }
        return newFile
    }

    override suspend fun renameRecording(audio: AudioEntity, newName: String): AudioEntity? = withContext(Dispatchers.IO) {
        val oldFile = File(audio.filePath)
        if (!oldFile.exists()) return@withContext null

        // Trim the user-entered name.
        val baseNewName = newName.trim()
        // Create a candidate file with the user-specified name.
        val candidateFile = File(oldFile.parentFile, baseNewName)
        // If a file with that name exists, get a new file name by appending a counter.
        val newFile = if (candidateFile.exists()) {
            getNextRenamedFile(candidateFile)
        } else {
            candidateFile
        }

        return@withContext if (oldFile.renameTo(newFile)) {
            // Update the database entry with the absolute path.
            val existing = recordingDao.getRecordingByFilePath(oldFile.absolutePath)
            if (existing != null) {
                val updated = existing.copy(filePath = newFile.absolutePath)
                recordingDao.upsertRecording(updated)
            }
            // Return the updated AudioEntity with the absolute file path.
            audio.copy(filePath = newFile.absolutePath)
        } else {
            null
        }
    }


    /**
     * If the candidate file already exists, this function appends a counter in parentheses
     * to generate a new file name. For example, if "MyRecording" exists then it returns "MyRecording(1)",
     * and if that exists then "MyRecording(2)", and so on.
     */
    private fun getNextRenamedFile(candidate: File): File {
        val parent = candidate.parentFile
        val baseName = candidate.name // using the candidate name (what the user entered)
        var counter = 1
        var newFile = File(parent, "$baseName($counter)")
        while (newFile.exists()) {
            counter++
            newFile = File(parent, "$baseName($counter)")
        }
        return newFile
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
        val (format, samples) = WavFileReader.readWav(inputWav)
        AmplitudeGate.applyAmplitudeGate(samples, 30)
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
