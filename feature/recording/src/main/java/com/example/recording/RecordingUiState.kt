package com.example.recording

import com.example.domain.entity.AudioEntity
import com.example.domain.entity.Recording

data class RecordingUiState(
    val isRecording: Boolean = false,
    val currentDb: Double = 0.0,
    val hasNoiseWarning: Boolean = false,
    val recordedAudio: AudioEntity? = null,
    val isPlaying: Boolean = false,
    val errorMessage: String? = null,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val recordings: List<Recording> = emptyList()
)