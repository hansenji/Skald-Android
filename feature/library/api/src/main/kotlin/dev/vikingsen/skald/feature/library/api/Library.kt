package dev.vikingsen.skald.feature.library.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Library : NavKey

@Serializable
data class Detail(val bookId: String) : NavKey

@Serializable
data class SeriesDetail(val seriesId: String) : NavKey

@Serializable
data class AuthorDetail(val authorId: String) : NavKey

@Serializable
data class CollectionDetail(val collectionId: String) : NavKey

@Serializable
data class PlaylistDetail(val playlistId: String) : NavKey

