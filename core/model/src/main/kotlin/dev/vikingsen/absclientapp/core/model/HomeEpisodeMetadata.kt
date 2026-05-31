package dev.vikingsen.absclientapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class HomeEpisodeMetadata(
    val id: String,
    val title: String? = null,
    val pubDate: String? = null,
    val duration: Double? = null
)
