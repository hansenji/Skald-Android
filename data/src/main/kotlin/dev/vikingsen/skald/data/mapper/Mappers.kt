package dev.vikingsen.skald.data.mapper

import dev.vikingsen.skald.core.database.BookEntity
import dev.vikingsen.skald.core.database.LocalAudioFile
import dev.vikingsen.skald.core.database.LocalChapter
import dev.vikingsen.skald.core.database.PlaybackProgressEntity
import dev.vikingsen.skald.core.database.BookWithProgressEntity
import dev.vikingsen.skald.core.database.LibraryEntity
import dev.vikingsen.skald.core.network.LibraryResponse
import dev.vikingsen.skald.core.network.LoggedUserResponse
import dev.vikingsen.skald.core.model.AudioFile
import dev.vikingsen.skald.core.model.Book
import dev.vikingsen.skald.core.model.Chapter
import dev.vikingsen.skald.core.model.DownloadStatus
import dev.vikingsen.skald.core.model.Library
import dev.vikingsen.skald.core.model.LoggedUser
import dev.vikingsen.skald.core.model.PlaybackProgress
import dev.vikingsen.skald.core.model.BookWithProgress
import dev.vikingsen.skald.core.database.SeriesEntity
import dev.vikingsen.skald.core.model.Series
import dev.vikingsen.skald.core.database.AuthorEntity
import dev.vikingsen.skald.core.model.Author
import dev.vikingsen.skald.core.network.NetworkAuthorResponse
import dev.vikingsen.skald.core.network.AuthorDetailsResponse
import dev.vikingsen.skald.core.database.CollectionEntity
import dev.vikingsen.skald.core.database.CollectionBookCrossRef
import dev.vikingsen.skald.core.model.BookCollection
import dev.vikingsen.skald.core.network.NetworkCollectionResponse
import dev.vikingsen.skald.core.database.PlaylistEntity
import dev.vikingsen.skald.core.database.PlaylistItemEntity
import dev.vikingsen.skald.core.model.Playlist
import dev.vikingsen.skald.core.model.PlaylistItem
import dev.vikingsen.skald.core.network.NetworkPlaylistResponse
import dev.vikingsen.skald.core.network.NetworkPlaylistItemResponse

fun LibraryResponse.toDomain(): Library = Library(
    id = id,
    name = name,
    type = type
)

fun LibraryEntity.toDomain(): Library = Library(
    id = id,
    name = name,
    type = type
)

fun Library.toEntity(): LibraryEntity = LibraryEntity(
    id = id,
    name = name,
    type = type
)

fun LoggedUserResponse.toDomain(): LoggedUser = LoggedUser(
    token = user.token,
    username = user.username
)

fun LocalAudioFile.toDomain(): AudioFile = AudioFile(
    index = index,
    ino = ino,
    duration = duration,
    mimeType = mimeType,
    filename = filename,
    size = size,
    localPath = localPath,
    downloadStatus = runCatching { DownloadStatus.valueOf(downloadStatus) }.getOrDefault(DownloadStatus.NOT_DOWNLOADED)
)

fun AudioFile.toEntity(): LocalAudioFile = LocalAudioFile(
    index = index,
    ino = ino,
    duration = duration,
    mimeType = mimeType,
    filename = filename,
    size = size,
    localPath = localPath,
    downloadStatus = downloadStatus.name
)

fun LocalChapter.toDomain(): Chapter = Chapter(
    start = start,
    end = end,
    title = title
)

fun Chapter.toEntity(): LocalChapter = LocalChapter(
    start = start,
    end = end,
    title = title
)

fun BookEntity.toDomain(): Book = Book(
    id = id,
    libraryId = libraryId,
    title = title,
    author = author,
    narrator = narrator,
    description = description,
    duration = duration,
    coverPath = coverPath,
    isDownloaded = isDownloaded,
    audioFiles = audioFiles.map { it.toDomain() },
    chapters = chapters.map { it.toDomain() },
    etag = etag,
    lastDetailFetchTimestamp = lastDetailFetchTimestamp,
    seriesId = seriesId,
    seriesSequence = seriesSequence
)

fun Book.toEntity(): BookEntity = BookEntity(
    id = id,
    libraryId = libraryId,
    title = title,
    author = author,
    narrator = narrator,
    description = description,
    duration = duration,
    coverPath = coverPath,
    isDownloaded = isDownloaded,
    audioFiles = audioFiles.map { it.toEntity() },
    chapters = chapters.map { it.toEntity() },
    etag = etag,
    lastDetailFetchTimestamp = lastDetailFetchTimestamp,
    seriesId = seriesId,
    seriesSequence = seriesSequence
)

fun SeriesEntity.toDomain(): Series = Series(
    id = id,
    libraryId = libraryId,
    name = name,
    description = description,
    bookCount = bookCount,
    etag = etag
)

fun Series.toEntity(): SeriesEntity = SeriesEntity(
    id = id,
    libraryId = libraryId,
    name = name,
    description = description,
    bookCount = bookCount,
    etag = etag
)

fun PlaybackProgressEntity.toDomain(): PlaybackProgress = PlaybackProgress(
    bookId = bookId,
    currentTime = currentTime,
    progress = progress,
    isFinished = isFinished,
    lastUpdated = lastUpdated
)

fun BookWithProgressEntity.toDomain(): BookWithProgress = BookWithProgress(
    book = book.toDomain(),
    progress = progress?.toDomain()
)

fun dev.vikingsen.skald.core.database.HomeShelfWithItems.toDomain(): dev.vikingsen.skald.core.model.HomeShelf = dev.vikingsen.skald.core.model.HomeShelf(
    id = shelf.id,
    libraryId = shelf.libraryId,
    label = shelf.label,
    total = shelf.total,
    type = shelf.type,
    items = items.sortedBy { it.horizontalIndex }.map { it.toDomain() }
)

fun dev.vikingsen.skald.core.database.HomeShelfItemEntity.toDomain(): dev.vikingsen.skald.core.model.HomeShelfItem = dev.vikingsen.skald.core.model.HomeShelfItem(
    entityId = entityId,
    title = title,
    subtitle = subtitle,
    imageUrl = imageUrl,
    additionalData = additionalData
)

fun AuthorEntity.toDomain(): Author = Author(
    id = id,
    libraryId = libraryId,
    name = name,
    description = description,
    imagePath = imagePath,
    bookCount = bookCount,
    etag = etag
)

fun Author.toEntity(): AuthorEntity = AuthorEntity(
    id = id,
    libraryId = libraryId,
    name = name,
    description = description,
    imagePath = imagePath,
    bookCount = bookCount,
    etag = etag
)

fun CollectionEntity.toDomain(bookIds: List<String>, bookCovers: List<String>): BookCollection = BookCollection(
    id = id,
    libraryId = libraryId,
    name = name,
    description = description,
    bookIds = bookIds,
    bookCovers = bookCovers,
    lastUpdated = lastUpdated
)

fun NetworkCollectionResponse.toEntity(libId: String, timestamp: Long): CollectionEntity = CollectionEntity(
    id = id,
    libraryId = if (libraryId.isNotEmpty()) libraryId else libId,
    name = name,
    description = description,
    lastUpdated = lastUpdate ?: timestamp
)

fun PlaylistEntity.toDomain(items: List<PlaylistItem>): Playlist = Playlist(
    id = id,
    name = name,
    description = description,
    duration = duration,
    itemCount = itemCount,
    items = items,
    lastUpdated = lastUpdated
)

fun PlaylistItemEntity.toDomain(coverPath: String?): PlaylistItem = PlaylistItem(
    id = id,
    playlistId = playlistId,
    libraryItemId = libraryItemId,
    sequence = sequence,
    title = title,
    duration = duration,
    coverPath = coverPath
)

fun NetworkPlaylistResponse.toEntity(timestamp: Long): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name,
    description = description,
    duration = totalDuration,
    itemCount = items.size,
    lastUpdated = lastUpdate ?: timestamp
)

fun NetworkPlaylistItemResponse.toEntity(playlistId: String, idx: Int): PlaylistItemEntity = PlaylistItemEntity(
    id = id ?: "${playlistId}_${libraryItemId}_${idx}",
    playlistId = playlistId,
    libraryItemId = libraryItemId,
    sequence = sequence ?: idx,
    title = episode?.title ?: libraryItem?.media?.metadata?.title ?: "Unknown Title",
    duration = episode?.duration ?: libraryItem?.media?.duration ?: 0.0
)

