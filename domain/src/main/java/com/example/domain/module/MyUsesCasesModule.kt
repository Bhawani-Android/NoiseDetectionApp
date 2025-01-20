package com.example.domain.module

import com.example.domain.repository.AudioRepository
import com.example.domain.usecase.DeleteAudioUseCase
import com.example.domain.usecase.PlayBackUseCase
import com.example.domain.usecase.RecordAudioUseCase
import com.example.domain.usecase.ReduceNoiseUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

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

//    @Provides
//    fun providePauseAudioUseCase(
//        audioRepository: AudioRepository
//    ): PauseAudioUseCase {
//        return PauseAudioUseCase(audioRepository)
//    }


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