package com.example.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.CommonUtils
import com.example.domain.entity.AudioEntity
import com.example.domain.entity.Recording
import com.example.domain.usecase.*
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

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val recordAudioUseCase: RecordAudioUseCase,
    private val reduceNoiseUseCase: ReduceNoiseUseCase,
    private val playBackUseCase: PlayBackUseCase,
    private val getPlaybackPositionUseCase: GetPlaybackPositionUseCase,
    private val getDurationUseCase: GetDurationUseCase,
    private val deleteAudioUseCase: DeleteAudioUseCase,
    private val getAllRecordingsUseCase: GetAllRecordingsUseCase
) : ViewModel() {

    val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState

    private val maxDurationMillis = 60_000L // 1 minute
    private var startTime: Long = 0
    private var playbackJob: Job? = null

    init {
        // Observe the list of all recordings from the DB
        viewModelScope.launch {
            getAllRecordingsUseCase().collectLatest { recordings ->
                _uiState.update { it.copy(recordings = recordings) }
            }
        }
    }

    /** Start recording flow, with real-time dB emission */
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

                // If we exceed time limit => automatically stop
                if (elapsed >= maxDurationMillis) {
                    stopRecording()
                }
            }
        }
    }

    /** Stop recording and store the last AudioEntity in uiState. */
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

    /** Apply a naive noise reduction on the 'recordedAudio' (if present). */
    fun reduceNoise() {
        val audio = _uiState.value.recordedAudio ?: return
        viewModelScope.launch {
            val newAudio = reduceNoiseUseCase(audio)
            _uiState.update { old ->
                old.copy(recordedAudio = newAudio)
            }
        }
    }

    /** Toggle playback (play/pause) for the current 'recordedAudio' in state. */
    fun playAudio(playAudio: Boolean) {
        val audio = _uiState.value.recordedAudio ?: return
        viewModelScope.launch {
            playBackUseCase(audio, playAudio)

            if (playAudio) {
                startPollingPlaybackPosition()
                _uiState.update { it.copy(isPlaying = true) }
            } else {
                stopPollingPlaybackPosition()
                _uiState.update { it.copy(isPlaying = false) }
            }
        }
    }

    /**
     * For the future: directly play a domain-level 'Recording' from the list.
     * We'll convert it to an AudioEntity, set it as the active recordedAudio, then call playAudio(true).
     */
    fun playRecording(rec: Recording, playAudio: Boolean = true) {
        // Convert domain 'Recording' to 'AudioEntity'
        val audioEntity = AudioEntity(
            filePath = rec.filePath,
            durationMillis = rec.durationMillis
        )
        // Set the recordedAudio as this item
        _uiState.update { it.copy(recordedAudio = audioEntity) }

        // Now just call our existing 'playAudio(true)'
        playAudio(playAudio)
    }

    /**
     * Delete the *currently active* 'recordedAudio' from the UI state.
     * Typically called by the 'Delete' button in the PlaybackSection.
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
     * Delete a domain-level 'Recording' from the list. This does not rely on 'recordedAudio'.
     * We'll convert the 'Recording' to an 'AudioEntity' and call [deleteAudioUseCase].
     */
    fun deleteRecording(rec: Recording) {
        val audioEntity = AudioEntity(
            filePath = rec.filePath,
            durationMillis = rec.durationMillis
        )
        viewModelScope.launch {
            val success = deleteAudioUseCase(audioEntity)
            if (success) {
                // If the currently active 'recordedAudio' matches this, clear it
                if (_uiState.value.recordedAudio?.filePath == rec.filePath) {
                    _uiState.update { old ->
                        old.copy(recordedAudio = null, isPlaying = false)
                    }
                }
            }
        }
    }

    /** Polls the playback position while playing. */
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

    private fun stopPollingPlaybackPosition() {
        playbackJob?.cancel()
        playbackJob = null
    }
}
