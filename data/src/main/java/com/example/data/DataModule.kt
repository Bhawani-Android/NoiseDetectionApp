package com.example.data

import com.example.data.repository.AudioRepositoryImpl
import com.example.domain.repository.AudioRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds
    @Singleton
    fun bindAudioRepository(
        impl: AudioRepositoryImpl
    ): AudioRepository
}