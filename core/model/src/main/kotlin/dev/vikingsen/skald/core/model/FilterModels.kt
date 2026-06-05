package dev.vikingsen.skald.core.model

enum class ReadStatusFilter {
    ALL,
    UNREAD,
    IN_PROGRESS,
    READ
}

enum class SortOption {
    TITLE_ASC,
    TITLE_DESC,
    AUTHOR_ASC,
    AUTHOR_DESC,
    DURATION_ASC,
    DURATION_DESC,
    LAST_PLAYED
}

enum class SeriesFilter {
    ALL,
    IN_PROGRESS,
    COMPLETED
}

enum class SeriesSortOption {
    NAME_ASC,
    NAME_DESC,
    BOOKS_COUNT_DESC,
    RECENTLY_UPDATED
}

enum class AuthorsSortOption {
    NAME_ASC,
    NAME_DESC,
    BOOKS_COUNT_DESC
}
