package com.example.domain.usecase

import com.example.domain.entity.AudioEntity
import com.example.domain.repository.AudioRepository
import javax.inject.Inject

class DeleteAudioUseCase @Inject constructor(private val audioRepository: AudioRepository) {
    suspend operator fun invoke(audio: AudioEntity): Boolean = audioRepository.deleteAudio(audio)
}