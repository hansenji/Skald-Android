package com.example.absclientapp.domain.repository

import com.example.absclientapp.domain.model.Book
import com.example.absclientapp.domain.model.Library
import com.example.absclientapp.domain.model.LoggedUser
import com.example.absclientapp.domain.model.PlaybackProgress
import com.example.absclientapp.domain.model.AudioFile
import com.example.absclientapp.domain.model.DownloadStatusState
import kotlinx.coroutines.flow.Flow

interface AudiobookshelfRepository {
    suspend fun login(url: String, user: String, pass: String): Result<LoggedUser>
    suspend fun fetchLibraries(): Result<List<Library>>
    suspend fun syncLibraryBooks(libraryId: String): Result<Unit>
    fun getBooksFlow(): Flow<List<Book>>
    fun getAllProgressFlow(): Flow<List<PlaybackProgress>>
    fun getBookWithProgressFlow(bookId: String): Flow<Pair<Book?, PlaybackProgress?>>
    suspend fun fetchBookDetails(bookId: String): Result<Book>
    suspend fun enqueueBookDownloads(bookId: String): Result<Unit>
    fun getBookDownloadFlow(bookId: String): Flow<DownloadStatusState>
    suspend fun saveLocalProgress(bookId: String, currentTime: Double, totalDuration: Double)
    suspend fun startPlaybackSession(bookId: String, deviceId: String, deviceName: String): Result<String>
    suspend fun syncPlaybackProgress(sessionId: String, timeListened: Double, currentTime: Double): Result<Unit>
    suspend fun syncStaticProgress(bookId: String, currentTime: Double, progress: Float, isFinished: Boolean): Result<Unit>
    suspend fun deleteLocalBookFiles(bookId: String): Result<Unit>
    suspend fun clearLocalData()
}
