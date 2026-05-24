package com.example.absclientapp.data.mapper

import com.example.absclientapp.data.database.BookEntity
import com.example.absclientapp.data.database.LocalAudioFile
import com.example.absclientapp.data.database.LocalChapter
import com.example.absclientapp.data.database.PlaybackProgressEntity
import com.example.absclientapp.data.model.LibraryResponse
import com.example.absclientapp.data.model.LoggedUserResponse
import com.example.absclientapp.domain.model.AudioFile
import com.example.absclientapp.domain.model.Book
import com.example.absclientapp.domain.model.Chapter
import com.example.absclientapp.domain.model.DownloadStatus
import com.example.absclientapp.domain.model.Library
import com.example.absclientapp.domain.model.LoggedUser
import com.example.absclientapp.domain.model.PlaybackProgress

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
    title = title,
    author = author,
    narrator = narrator,
    description = description,
    duration = duration,
    coverPath = coverPath,
    isDownloaded = isDownloaded,
    audioFiles = audioFiles.map { it.toDomain() },
    chapters = chapters.map { it.toDomain() }
)

fun Book.toEntity(): BookEntity = BookEntity(
    id = id,
    title = title,
    author = author,
    narrator = narrator,
    description = description,
    duration = duration,
    coverPath = coverPath,
    isDownloaded = isDownloaded,
    audioFiles = audioFiles.map { it.toEntity() },
    chapters = chapters.map { it.toEntity() }
)

fun PlaybackProgressEntity.toDomain(): PlaybackProgress = PlaybackProgress(
    bookId = bookId,
    currentTime = currentTime,
    progress = progress,
    isFinished = isFinished,
    lastUpdated = lastUpdated
)
