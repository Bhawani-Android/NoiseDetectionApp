package com.example.domain.usecase

import com.example.domain.entity.AudioEntity
import com.example.domain.repository.AudioRepository
import javax.inject.Inject

class ReduceNoiseUseCase @Inject constructor(private val audioRepository: AudioRepository) {
    suspend operator fun invoke(audio : AudioEntity): AudioEntity = audioRepository.reduceNoise(audio)

}