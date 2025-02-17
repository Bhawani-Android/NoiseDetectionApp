package com.example.recording.ui

import com.example.domain.entity.AudioEntity
import com.example.domain.entity.Recording

/**
 * Represents the UI state for recording and playback.
 *
 * Note: This data class currently includes both recording and playback fields.
 * Consider splitting it (e.g. into [RecordingState] and [AudioPlaybackState]) to
 * separate concerns more clearly.
 */
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
