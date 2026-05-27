package dev.vikingsen.absclientapp.core.model

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val narrator: String,
    val description: String,
    val duration: Double,
    val coverPath: String?,
    val isDownloaded: Boolean,
    val audioFiles: List<AudioFile>,
    val chapters: List<Chapter>
)
