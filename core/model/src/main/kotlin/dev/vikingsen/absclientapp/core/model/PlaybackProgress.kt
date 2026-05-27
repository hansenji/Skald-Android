package dev.vikingsen.absclientapp.core.model

data class PlaybackProgress(
    val bookId: String,
    val currentTime: Double,
    val progress: Float,
    val isFinished: Boolean,
    val lastUpdated: Long
)
