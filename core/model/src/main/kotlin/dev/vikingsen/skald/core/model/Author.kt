package dev.vikingsen.skald.core.model

data class Author(
    val id: String,
    val libraryId: String,
    val name: String,
    val description: String?,
    val imagePath: String?,
    val bookCount: Int,
    val etag: String? = null
)
