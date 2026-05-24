package com.example.absclientapp.domain.model

data class PlaybackProgress(
    val bookId: String,
    val currentTime: Double,
    val progress: Float,
    val isFinished: Boolean,
    val lastUpdated: Long
)
