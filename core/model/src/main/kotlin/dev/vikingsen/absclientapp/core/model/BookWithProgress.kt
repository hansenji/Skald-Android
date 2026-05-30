package dev.vikingsen.absclientapp.core.model

data class BookWithProgress(
    val book: Book,
    val progress: PlaybackProgress?
)
