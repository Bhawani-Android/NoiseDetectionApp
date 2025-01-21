package com.example.domain.usecase

import com.example.domain.entity.Recording
import com.example.domain.repository.AudioRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class GetAllRecordingsUseCase @Inject constructor(
    private val audioRepository: AudioRepository
) {
    operator fun invoke(): Flow<List<Recording>> {
        return audioRepository.getAllRecordingsFlow()
    }
}
