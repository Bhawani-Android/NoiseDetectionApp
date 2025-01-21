package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a recorded audio in the local DB.
 */
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val timestamp: Long,
    val durationMillis: Long,
    val isNoisy: Boolean = false
)
