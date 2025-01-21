package com.example.data.mapper

import com.example.data.local.RecordingEntity
import com.example.domain.entity.Recording

fun RecordingEntity.toDomain(): Recording {
    return Recording(
        id = this.id,
        filePath = this.filePath,
        durationMillis = this.durationMillis,
        timestamp = this.timestamp,
        isNoisy = this.isNoisy
    )
}
