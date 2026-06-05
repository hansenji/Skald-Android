package dev.vikingsen.skald.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "authors")
data class AuthorEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val name: String,
    val description: String?,
    val imagePath: String?,
    val bookCount: Int,
    val etag: String? = null
)

@Entity(
    tableName = "author_books",
    primaryKeys = ["authorId", "bookId"]
)
data class AuthorBookCrossRef(
    val authorId: String,
    val bookId: String
)
