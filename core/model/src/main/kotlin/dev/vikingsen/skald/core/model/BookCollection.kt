package dev.vikingsen.skald.core.model

data class BookCollection(
    val id: String,
    val libraryId: String,
    val name: String,
    val description: String?,
    val bookIds: List<String>,
    val bookCovers: List<String> = emptyList(),
    val lastUpdated: Long
)

enum class CollectionsSortOption {
    NAME_ASC,
    NAME_DESC,
    BOOKS_COUNT_DESC,
    LAST_MODIFIED
}
