package com.example.domain.entity

data class Recording(
    val id: Long,
    val filePath: String,
    val durationMillis: Long,
    val timestamp: Long,
    val isNoisy: Boolean
)
