package com.example.absclientapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CredentialsLoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoggedUserResponse(
    val user: UserDetails
)

@Serializable
data class UserDetails(
    val token: String,
    val username: String
)

@Serializable
data class LibraryResponse(
    val id: String,
    val name: String,
    val type: String? = null
)

@Serializable
data class LibrariesResponse(
    val libraries: List<LibraryResponse>
)

@Serializable
data class LibraryItemsResponse(
    val results: List<LibraryItem>
)

@Serializable
data class LibraryItem(
    val id: String,
    val media: LibraryItemMedia
)

@Serializable
data class LibraryItemMedia(
    val metadata: LibraryItemMetadata,
    val numChapters: Int? = null
)

@Serializable
data class LibraryItemMetadata(
    val title: String? = null,
    val authorName: String? = null,
    val narratorName: String? = null,
    val seriesName: String? = null
)

@Serializable
data class BookResponse(
    val id: String,
    val media: BookMedia
)

@Serializable
data class BookMedia(
    val metadata: BookMetadata,
    val audioFiles: List<BookAudioFileResponse>? = null,
    val chapters: List<BookChapterResponse>? = null
)

@Serializable
data class BookMetadata(
    val title: String,
    val subtitle: String? = null,
    val authorName: String? = null,
    val narratorName: String? = null,
    val description: String? = null,
    val publishedYear: String? = null,
    val publisher: String? = null
)

@Serializable
data class BookAudioFileResponse(
    val index: Int,
    val ino: String,
    val duration: Double? = null,
    val mimeType: String,
    val metadata: AudioFileMetadata? = null
)

@Serializable
data class AudioFileMetadata(
    val filename: String,
    val ext: String,
    val size: Long
)

@Serializable
data class BookChapterResponse(
    val start: Double,
    val end: Double,
    val title: String
)

@Serializable
data class PlaybackStartRequest(
    val deviceInfo: DeviceInfo,
    val supportedMimeTypes: List<String> = listOf("audio/mpeg", "audio/mp4", "audio/aac", "audio/ogg", "application/x-mpegURL"),
    val mediaPlayer: String = "ExoPlayer",
    val forceTranscode: Boolean = false,
    val forceDirectPlay: Boolean = true
)

@Serializable
data class DeviceInfo(
    val clientName: String = "ABS Client Android",
    val deviceId: String,
    val deviceName: String
)

@Serializable
data class PlaybackSessionResponse(
    val id: String,
    val libraryItemId: String
)

@Serializable
data class ProgressSyncRequest(
    val timeListened: Double,
    val currentTime: Double
)

@Serializable
data class MediaProgressResponse(
    val currentTime: Double,
    val isFinished: Boolean,
    val progress: Float
)
