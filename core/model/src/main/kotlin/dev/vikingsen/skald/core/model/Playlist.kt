package dev.vikingsen.skald.core.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String?,
    val duration: Double,
    val itemCount: Int,
    val items: List<PlaylistItem>,
    val lastUpdated: Long
)

data class PlaylistItem(
    val id: String,
    val playlistId: String,
    val libraryItemId: String,
    val sequence: Int,
    val title: String,
    val duration: Double,
    val coverPath: String? = null
)

enum class PlaylistsSortOption {
    NAME_ASC,
    NAME_DESC,
    TRACKS_COUNT_DESC,
    DURATION_DESC
}
