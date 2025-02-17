package com.example.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.CommonUtils
import com.example.domain.entity.AudioEntity
import com.example.domain.entity.Recording
import com.example.domain.usecase.*
import com.example.recording.ui.RecordingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for recording, playback, and noise reduction.
 *
 * Note: The current [RecordingUiState] contains both recording and playback
 * information. Consider refactoring this into separate state classes (e.g.
 * [RecordingState] and [AudioPlaybackState]) to better adhere to the Single
 * Responsibility Principle.
 */
@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val recordAudioUseCase: RecordAudioUseCase,
    private val reduceNoiseUseCase: ReduceNoiseUseCase,
    private val playBackUseCase: PlayBackUseCase,
    private val getPlaybackPositionUseCase: GetPlaybackPositionUseCase,
    private val getDurationUseCase: GetDurationUseCase,
    private val deleteAudioUseCase: DeleteAudioUseCase,
    private val getAllRecordingsUseCase: GetAllRecordingsUseCase,
    private val saveRecordingUseCase: SaveRecordingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState

    // Maximum recording duration (e.g. 1 minute)
    private val maxDurationMillis = 60_000L
    private var startTime: Long = 0
    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            // Listen for updates to the list of recordings.
            getAllRecordingsUseCase().collectLatest { recordings ->
                _uiState.update { it.copy(recordings = recordings) }
            }
        }
    }

    /**
     * Starts audio recording with a given noise threshold.
     *
     * @param thresholdDb The decibel threshold at which a noise warning should be shown.
     */
    fun startRecording(thresholdDb: Double) {
        if (_uiState.value.isRecording) return

        _uiState.update {
            it.copy(
                isRecording = true,
                currentDb = 0.0,
                hasNoiseWarning = false
            )
        }
        startTime = CommonUtils.now()

        viewModelScope.launch {
            recordAudioUseCase(thresholdDb).collectLatest { db ->
                val elapsed = CommonUtils.now() - startTime
                val showWarning = db >= thresholdDb

                _uiState.update { old ->
                    old.copy(
                        currentDb = db,
                        hasNoiseWarning = showWarning
                    )
                }

                if (elapsed >= maxDurationMillis) {
                    stopRecording()
                }
            }
        }
    }

    /**
     * Stops the ongoing recording and updates the UI state with the recorded audio.
     */
    fun stopRecording() {
        if (!_uiState.value.isRecording) return

        viewModelScope.launch {
            val audio = recordAudioUseCase.stop()
            _uiState.update { old ->
                old.copy(
                    isRecording = false,
                    currentDb = 0.0,
                    hasNoiseWarning = false,
                    recordedAudio = audio
                )
            }
        }
    }

    /**
     * Reduces noise in the recorded audio.
     */
    fun reduceNoise() {
        // Optionally reduce noise in live recording.
        val audio = _uiState.value.recordedAudio ?: return
        viewModelScope.launch {
            val newAudio = reduceNoiseUseCase(audio)
            _uiState.update { old ->
                old.copy(recordedAudio = newAudio)
            }
        }
    }

    /**
     * Toggles audio playback.
     *
     * @param shouldPlay When true, starts playback; when false, pauses playback.
     */
    fun togglePlayback(shouldPlay: Boolean) {
        val audio = _uiState.value.recordedAudio ?: return
        viewModelScope.launch {
            playBackUseCase(audio, shouldPlay)

            if (shouldPlay) {
                startPollingPlaybackPosition()
                _uiState.update { it.copy(isPlaying = true) }
            } else {
                stopPollingPlaybackPosition()
                _uiState.update { it.copy(isPlaying = false) }
            }
        }
    }

    /**
     * Starts playback of a selected recording.
     *
     * Converts a [Recording] domain object to an [AudioEntity] before playback.
     */
    fun playRecording(rec: Recording, shouldPlay: Boolean = true) {
        val audioEntity = AudioEntity(
            filePath = rec.filePath,
            durationMillis = rec.durationMillis
        )
        // Update the current recorded audio.
        _uiState.update { it.copy(recordedAudio = audioEntity) }
        // Delegate to togglePlayback.
        togglePlayback(shouldPlay)
    }

    /**
     * Deletes the current recorded audio if present.
     */
    fun deleteAudio() {
        val audio = _uiState.value.recordedAudio ?: return
        viewModelScope.launch {
            val success = deleteAudioUseCase(audio)
            if (success) {
                _uiState.update { old ->
                    old.copy(recordedAudio = null, isPlaying = false)
                }
            }
        }
    }

    /**
     * Deletes a specific recording.
     */
    fun deleteRecording(rec: Recording) {
        val audioEntity = AudioEntity(
            filePath = rec.filePath,
            durationMillis = rec.durationMillis
        )
        viewModelScope.launch {
            val success = deleteAudioUseCase(audioEntity)
            if (success && _uiState.value.recordedAudio?.filePath == rec.filePath) {
                _uiState.update { old ->
                    old.copy(recordedAudio = null, isPlaying = false)
                }
            }
        }
    }

    /**
     * Periodically polls for the current playback position and updates the UI state.
     */
    private fun startPollingPlaybackPosition() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch(Dispatchers.IO) {
            val audio = _uiState.value.recordedAudio ?: return@launch
            val totalMs = getDurationUseCase(audio)

            while (_uiState.value.isPlaying) {
                val currentMs = getPlaybackPositionUseCase(audio)
                _uiState.update { old ->
                    old.copy(
                        currentPositionMs = currentMs.toLong(),
                        totalDurationMs = totalMs.toLong()
                    )
                }
                delay(500)
            }
        }
    }

    /**
     * Stops polling for the playback position.
     */
    private fun stopPollingPlaybackPosition() {
        playbackJob?.cancel()
        playbackJob = null
    }


    /**
     * Saves the recorded audio using the provided file name.
     * This delegates to the [SaveRecordingUseCase] which renames the file and updates the database.
     */
    fun saveRecording(fileName: String) {
        val audio = _uiState.value.recordedAudio ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedAudio = saveRecordingUseCase(audio, fileName)
            if (updatedAudio != null) {
                _uiState.update { old -> old.copy(recordedAudio = updatedAudio) }
            } else {
                // Optionally update the UI state with an error message.
            }
        }
    }
}
