package dev.vikingsen.skald.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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
    val username: String,
    val id: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

@Serializable
data class RefreshResponse(
    val user: RefreshUserDetails
)

@Serializable
data class RefreshUserDetails(
    val accessToken: String,
    val refreshToken: String
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
    val results: List<LibraryItem>,
    val total: Int = 0
)

@Serializable
data class LibraryItem(
    val id: String,
    val media: LibraryItemMedia,
    val recentEpisode: NetworkPodcastEpisode? = null
)

@Serializable
data class NetworkPodcastEpisode(
    val id: String,
    val index: Int? = null,
    val episode: String? = null,
    val episodeType: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val description: String? = null,
    val pubDate: String? = null,
    val publishedAt: Long? = null,
    val duration: Double? = null,
    val size: Long? = null
)

@Serializable
sealed interface NetworkLibraryShelf {
    val id: String
    val label: String
    val total: Int
    val type: String
}

@Serializable
@SerialName("book")
data class NetworkBookShelf(
    override val id: String,
    override val label: String,
    override val total: Int,
    override val type: String,
    val entities: List<LibraryItem>? = null
) : NetworkLibraryShelf

@Serializable
@SerialName("podcast")
data class NetworkPodcastShelf(
    override val id: String,
    override val label: String,
    override val total: Int,
    override val type: String,
    val entities: List<LibraryItem>? = null
) : NetworkLibraryShelf

@Serializable
@SerialName("episode")
data class NetworkEpisodeShelf(
    override val id: String,
    override val label: String,
    override val total: Int,
    override val type: String,
    val entities: List<LibraryItem>? = null
) : NetworkLibraryShelf

@Serializable
@SerialName("series")
data class NetworkSeriesShelf(
    override val id: String,
    override val label: String,
    override val total: Int,
    override val type: String,
    val entities: List<NetworkSeriesItem>? = null
) : NetworkLibraryShelf

@Serializable
@SerialName("authors")
data class NetworkAuthorShelf(
    override val id: String,
    override val label: String,
    override val total: Int,
    override val type: String,
    val entities: List<NetworkAuthorItem>? = null
) : NetworkLibraryShelf

@Serializable
data class NetworkSeriesItem(
    val id: String,
    val name: String,
    val books: List<LibraryItem>? = null
)

@Serializable
data class NetworkAuthorItem(
    val id: String,
    val name: String,
    val coverPath: String? = null
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
    val seriesName: String? = null,
    val authors: List<BookAuthorResponse>? = null,
    val narrators: List<String>? = null
)

@Serializable
data class BookAuthorResponse(
    val id: String? = null,
    val name: String? = null
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
    val authors: List<BookAuthorResponse>? = null,
    val narrators: List<String>? = null,
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
    val clientName: String = "Skald Android",
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
