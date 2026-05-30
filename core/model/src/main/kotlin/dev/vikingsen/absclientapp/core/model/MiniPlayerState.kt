package dev.vikingsen.absclientapp.core.model

data class MiniPlayerState(
    val bookId: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val authorizationHeader: String?,
    val isPlaying: Boolean,
    val progress: Float,
    val isLoading: Boolean = false
)
