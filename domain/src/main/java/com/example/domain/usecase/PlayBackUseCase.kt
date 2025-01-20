package com.example.domain.usecase

import com.example.domain.entity.AudioEntity
import com.example.domain.repository.AudioRepository
import javax.inject.Inject
import javax.inject.Named

class PlayBackUseCase @Inject constructor(private val audioRepository: AudioRepository) {
    operator fun invoke(audio: AudioEntity, play: Boolean) = audioRepository.playAudio(audio, play)
}

class GetPlaybackPositionUseCase @Inject constructor(
    private val audioRepository: AudioRepository
) {
    operator fun invoke(audio: AudioEntity): Int {
        return audioRepository.getCurrentPosition(audio)
    }
}

class GetDurationUseCase @Inject constructor(
    private val audioRepository: AudioRepository
) {
    operator fun invoke(audio: AudioEntity): Int {
        return audioRepository.getDuration(audio)
    }
}

