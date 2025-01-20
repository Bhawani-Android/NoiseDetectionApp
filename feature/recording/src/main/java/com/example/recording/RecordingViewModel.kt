package com.example.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.CommonUtils
import com.example.domain.usecase.DeleteAudioUseCase
import com.example.domain.usecase.GetDurationUseCase
import com.example.domain.usecase.GetPlaybackPositionUseCase
import com.example.domain.usecase.PlayBackUseCase
import com.example.domain.usecase.RecordAudioUseCase
import com.example.domain.usecase.ReduceNoiseUseCase
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
    private val deleteAudioUseCase: DeleteAudioUseCase
): ViewModel(){
    private val _uiState =  MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState

    private var maxFileSizeBytes = 5 * 1024 * 1024 // 5MB
    private var maxDurationMillis = 60_000L // 1 minute

    private var startTime: Long = 0
    private var playbackJob: Job? = null


    fun startRecording(thresholdDb: Double)  {
        if(_uiState.value.isRecording) return
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
                // If the user hits the 1 min limit, or the file is over 5MB (we can't easily check file size here unless we have path)
                // For simplicity, we only show a time-based check:
                if (elapsed >= maxDurationMillis) {
                    stopRecording()
                }
            }
        }
    }

     fun stopRecording() {
        if(!_uiState.value.isRecording) return
        viewModelScope.launch {
            val audio = recordAudioUseCase.stop()
            _uiState.update { old ->
                old.copy(isRecording = false, currentDb = 0.0, hasNoiseWarning = false, recordedAudio = audio)
            }
        }
    }
    fun reduceNoise() {
        val audio = _uiState.value.recordedAudio ?: return
        viewModelScope.launch {
            val newAudio = reduceNoiseUseCase(audio)
            _uiState.update { old ->
                old.copy(recordedAudio = newAudio)
            }
        }
    }

    /**
     * Toggle playback with a boolean. If true => play, false => pause.
     */
    fun playAudio(playAudio: Boolean) {
        val audio = _uiState.value.recordedAudio ?: return
        viewModelScope.launch {
            playBackUseCase(audio, playAudio)

            if (playAudio) {
                // Start polling playback position
                startPollingPlaybackPosition()
                _uiState.update { it.copy(isPlaying = true) }
            } else {
                // Cancel polling
                stopPollingPlaybackPosition()
                _uiState.update { it.copy(isPlaying = false) }
            }
        }
    }

    /**
     * Launch a background job that periodically updates currentPlaybackPositionMs
     * in the UI state while isPlaying == true.
     */
    private fun startPollingPlaybackPosition() {
        // Cancel any previous job
        playbackJob?.cancel()

        // Start a new job
        playbackJob = viewModelScope.launch(Dispatchers.IO) {
            val audio = _uiState.value.recordedAudio ?: return@launch

            // Retrieve total duration from the repository
            val totalMs = getDurationUseCase(audio)

            while (_uiState.value.isPlaying) {
                val currentMs = getPlaybackPositionUseCase(audio)

                // Update UI state
                _uiState.update { old ->
                    old.copy(
                        currentPositionMs = currentMs.toLong(),
                        totalDurationMs = totalMs.toLong()
                    )
                }

                // Sleep a bit
                delay(500) // poll every 0.5s
            }
        }
    }

    /**
     * Stop the playback polling job.
     */
    private fun stopPollingPlaybackPosition() {
        playbackJob?.cancel()
        playbackJob = null
    }


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

}