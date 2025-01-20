package com.example.domain.usecase

import com.example.domain.entity.AudioEntity
import com.example.domain.repository.AudioRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RecordAudioUseCase @Inject constructor(private val audioRepository: AudioRepository) {
    operator fun invoke(thresholdDb: Double): Flow<Double> = audioRepository.startRecording(thresholdDb)
    suspend fun stop(): AudioEntity? = audioRepository.stopRecording()
}