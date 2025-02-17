package com.example.domain.module

import com.example.domain.repository.AudioRepository
import com.example.domain.usecase.DeleteAudioUseCase
import com.example.domain.usecase.PlayBackUseCase
import com.example.domain.usecase.RecordAudioUseCase
import com.example.domain.usecase.ReduceNoiseUseCase
import com.example.domain.usecase.SaveRecordingUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object MyUseCasesModule {

    @Provides
    fun provideRecordAudioUseCase(
        audioRepository: AudioRepository
    ): RecordAudioUseCase {
        return RecordAudioUseCase(audioRepository)
    }

    @Provides
    fun provideReduceNoiseUseCase(
        audioRepository: AudioRepository
    ): ReduceNoiseUseCase {
        return ReduceNoiseUseCase(audioRepository)
    }

    @Provides
    fun provideSaveRecordingUseCase(
        audioRepository: AudioRepository
    ): SaveRecordingUseCase {
        return SaveRecordingUseCase(audioRepository)
    }


    @Provides
    fun providePlayBackUseCase(
        audioRepository: AudioRepository
    ): PlayBackUseCase {
        return PlayBackUseCase(audioRepository)
    }

    @Provides
    fun provideDeleteAudioUseCase(
        audioRepository: AudioRepository
    ): DeleteAudioUseCase {
        return DeleteAudioUseCase(audioRepository)
    }
}