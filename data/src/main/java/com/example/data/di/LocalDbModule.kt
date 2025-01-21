package com.example.data.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.RecordingDao
import com.example.data.local.RecordingDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalDbModule {

    @Provides
    @Singleton
    fun provideRecordingDatabase(
        @ApplicationContext context: Context
    ): RecordingDatabase {
        return Room.databaseBuilder(
            context,
            RecordingDatabase::class.java,
            "recording_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideRecordingDao(
        db: RecordingDatabase
    ): RecordingDao = db.recordingDao()
}
