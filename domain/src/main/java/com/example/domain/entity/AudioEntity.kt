package com.example.domain.entity

data class AudioEntity(
    val filePath: String,
    val durationMillis: Long,
    val isNoisy: Boolean = false
)
