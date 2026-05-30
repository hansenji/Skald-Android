package dev.vikingsen.absclientapp.domain.repository

import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.model.Library
import dev.vikingsen.absclientapp.core.model.LoggedUser
import dev.vikingsen.absclientapp.core.model.PlaybackProgress
import dev.vikingsen.absclientapp.core.model.AudioFile
import dev.vikingsen.absclientapp.core.model.DownloadStatusState
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData
import dev.vikingsen.absclientapp.core.model.BookWithProgress
import dev.vikingsen.absclientapp.core.model.ReadStatusFilter
import dev.vikingsen.absclientapp.core.model.SortOption

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
    fun getBooksPaged(
        libraryId: String,
        query: String,
        filter: ReadStatusFilter,
        downloadedOnly: Boolean,
        sortBy: SortOption
    ): Flow<PagingData<BookWithProgress>>
    suspend fun getCachedLibraries(): List<Library>
}
