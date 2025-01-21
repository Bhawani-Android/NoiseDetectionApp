package com.example.domain.repository

import com.example.domain.entity.AudioEntity
import com.example.domain.entity.Recording
import kotlinx.coroutines.flow.Flow

interface AudioRepository {

    /**
     * Start recording audio, emit noise levels in real-time through a Flow of decibel values.
     * Also provide callbacks or additional flows to notify about threshold exceed.
     */
    fun startRecording(thresholdDb: Double): Flow<Double>

    /**
     * Stop recording, returns the recorded audio file metadata.
     */
    suspend fun stopRecording(): AudioEntity?

    /**
     * Apply noise reduction on the recorded file and return updated entity.
     */
    suspend fun reduceNoise(audio: AudioEntity): AudioEntity

    /**
     * Play recorded audio;
     */
     fun playAudio(audio: AudioEntity, play: Boolean)
     fun getCurrentPosition(audio: AudioEntity): Int
     fun getDuration(audio: AudioEntity): Int

    /**
     * Delete recorded audio from local storage.
     */
    suspend fun deleteAudio(audio: AudioEntity):Boolean

    /**
     * Check if there's an existing recording in storage or memory.
     */
    suspend fun getLastRecordedAudio(): AudioEntity?
    fun getAllRecordingsFlow(): Flow<List<Recording>>

}