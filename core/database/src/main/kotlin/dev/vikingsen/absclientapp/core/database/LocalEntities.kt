package dev.vikingsen.absclientapp.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class LocalAudioFile(
    val index: Int,
    val ino: String,
    val duration: Double,
    val mimeType: String,
    val filename: String,
    val size: Long,
    val localPath: String? = null,
    val downloadStatus: String = "NOT_DOWNLOADED" // NOT_DOWNLOADED, DOWNLOADING, COMPLETED
)

@Serializable
data class LocalChapter(
    val start: Double,
    val end: Double,
    val title: String
)

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val narrator: String,
    val description: String,
    val duration: Double,
    val coverPath: String?,
    val isDownloaded: Boolean = false,
    val audioFiles: List<LocalAudioFile>,
    val chapters: List<LocalChapter>
)

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val bookId: String,
    val currentTime: Double,
    val progress: Float,
    val isFinished: Boolean,
    val lastUpdated: Long
)
