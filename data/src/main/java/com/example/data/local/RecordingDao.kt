package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Query("SELECT * FROM recordings ORDER BY timestamp DESC")
    fun getAllRecordingsFlow(): Flow<List<RecordingEntity>>

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecording(id: Long)

    @Query("SELECT * FROM recordings WHERE filePath = :path LIMIT 1")
    suspend fun getRecordingByFilePath(path: String): RecordingEntity?
}
