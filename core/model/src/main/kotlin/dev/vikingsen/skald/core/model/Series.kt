package dev.vikingsen.skald.core.model

data class Series(
    val id: String,
    val libraryId: String,
    val name: String,
    val description: String?,
    val bookCount: Int,
    val etag: String? = null
)
