package dev.vikingsen.absclientapp.data.mapper

import dev.vikingsen.absclientapp.core.database.BookEntity
import dev.vikingsen.absclientapp.core.database.LocalAudioFile
import dev.vikingsen.absclientapp.core.database.LocalChapter
import dev.vikingsen.absclientapp.core.database.PlaybackProgressEntity
import dev.vikingsen.absclientapp.core.database.BookWithProgressEntity
import dev.vikingsen.absclientapp.core.network.LibraryResponse
import dev.vikingsen.absclientapp.core.network.LoggedUserResponse
import dev.vikingsen.absclientapp.core.model.AudioFile
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.model.Chapter
import dev.vikingsen.absclientapp.core.model.DownloadStatus
import dev.vikingsen.absclientapp.core.model.Library
import dev.vikingsen.absclientapp.core.model.LoggedUser
import dev.vikingsen.absclientapp.core.model.PlaybackProgress
import dev.vikingsen.absclientapp.core.model.BookWithProgress

fun LibraryResponse.toDomain(): Library = Library(
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
    lastDetailFetchTimestamp = lastDetailFetchTimestamp
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
    lastDetailFetchTimestamp = lastDetailFetchTimestamp
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
