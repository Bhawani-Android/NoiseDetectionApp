package com.example.domain.usecase

import com.example.domain.entity.AudioEntity
import com.example.domain.repository.AudioRepository
import javax.inject.Inject

/**
 * Use case to save (i.e. rename) the recorded audio with the provided file name.
 */
class SaveRecordingUseCase @Inject constructor(
    private val audioRepository: AudioRepository
) {
    suspend operator fun invoke(audio: AudioEntity, newName: String): AudioEntity? {
        return audioRepository.renameRecording(audio, newName)
    }
}
