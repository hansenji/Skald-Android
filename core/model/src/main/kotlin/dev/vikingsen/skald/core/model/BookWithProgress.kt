package dev.vikingsen.skald.core.model

data class BookWithProgress(
    val book: Book,
    val progress: PlaybackProgress?
)
