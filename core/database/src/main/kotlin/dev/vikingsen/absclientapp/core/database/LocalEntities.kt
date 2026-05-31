package dev.vikingsen.absclientapp.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
data class LocalAudioFile(
    val index: Int,
    val ino: String,
    val duration: Double,
    val mimeType: String,
    val filename: String,
    val size: Long,
    val localPath: String? = null,
    val downloadStatus: String = "NOT_DOWNLOADED" // NOT_DOWNLOADED, DOWNLOADING, COMPLETED
)

@Serializable
data class LocalChapter(
    val start: Double,
    val end: Double,
    val title: String
)

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val libraryId: String = "",
    val title: String,
    val author: String,
    val narrator: String,
    val description: String,
    val duration: Double,
    val coverPath: String?,
    val isDownloaded: Boolean = false,
    val audioFiles: List<LocalAudioFile>,
    val chapters: List<LocalChapter>,
    val etag: String? = null,
    val lastDetailFetchTimestamp: Long = 0L
)

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val bookId: String,
    val currentTime: Double,
    val progress: Float,
    val isFinished: Boolean,
    val lastUpdated: Long
)

data class BookWithProgressEntity(
    @Embedded val book: BookEntity,
    @Embedded(prefix = "progress_") val progress: PlaybackProgressEntity?
)

@Entity(tableName = "libraries")
data class LibraryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String?
)

@Entity(tableName = "home_shelves")
data class HomeShelfEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val label: String,
    val total: Int,
    val type: String,          // Discriminator: "book", "series", "authors", "episode", "podcast"
    val verticalSortOrder: Int // Order in which the shelf is rendered vertically
)

@Entity(
    tableName = "home_shelf_items",
    foreignKeys = [
        ForeignKey(
            entity = HomeShelfEntity::class,
            parentColumns = ["id"],
            childColumns = ["shelfId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HomeShelfItemEntity(
    @PrimaryKey val compositeId: String, // Constructed as "${shelfId}_${entityId}"
    val shelfId: String,
    val entityId: String,                // ID of Book, Series, Author, or Podcast
    val title: String?,                  // Cached title for instant display
    val subtitle: String?,               // Cached subtitle/author/info text
    val imageUrl: String?,               // Cover image URL or relative path
    val horizontalIndex: Int,            // Order of the item in the horizontal row
    val additionalData: String?          // Serialized JSON for specialized elements (e.g. podcast episode meta, sequence data)
)

data class HomeShelfWithItems(
    @Embedded val shelf: HomeShelfEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "shelfId"
    )
    val items: List<HomeShelfItemEntity>
)


