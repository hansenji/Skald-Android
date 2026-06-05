package dev.vikingsen.skald.domain.repository

import dev.vikingsen.skald.core.model.Book
import dev.vikingsen.skald.core.model.Library
import dev.vikingsen.skald.core.model.LoggedUser
import dev.vikingsen.skald.core.model.PlaybackProgress
import dev.vikingsen.skald.core.model.AudioFile
import dev.vikingsen.skald.core.model.DownloadStatusState
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData
import dev.vikingsen.skald.core.model.BookWithProgress
import dev.vikingsen.skald.core.model.ReadStatusFilter
import dev.vikingsen.skald.core.model.SortOption
import dev.vikingsen.skald.core.model.HomeShelf
import dev.vikingsen.skald.core.model.Series

interface AudiobookshelfRepository {
    suspend fun login(url: String, user: String, pass: String): Result<LoggedUser>
    suspend fun fetchLibraries(): Result<List<Library>>
    suspend fun syncLibraryBooks(libraryId: String, forceRefresh: Boolean = false): Result<Unit>
    suspend fun syncLibrarySeries(libraryId: String, forceRefresh: Boolean = false): Result<Unit>
    fun getHomeShelvesFlow(libraryId: String): Flow<List<HomeShelf>>
    suspend fun syncHomeShelves(libraryId: String, forceRefresh: Boolean = false): Result<Unit>
    fun getBooksFlow(): Flow<List<Book>>
    fun getSeriesFlow(libraryId: String): Flow<List<Series>>
    suspend fun getSeriesById(seriesId: String): Series?
    fun getBooksForSeriesFlow(seriesId: String): Flow<List<BookWithProgress>>
    fun getBooksWithProgressForLibraryFlow(libraryId: String): Flow<List<BookWithProgress>>
    fun getAllProgressFlow(): Flow<List<PlaybackProgress>>
    fun getBookWithProgressFlow(bookId: String): Flow<Pair<Book?, PlaybackProgress?>>
    suspend fun fetchBookDetails(bookId: String, forceRefresh: Boolean = false): Result<Book>
    suspend fun enqueueBookDownloads(bookId: String): Result<Unit>
    fun getBookDownloadFlow(bookId: String): Flow<DownloadStatusState>
    suspend fun saveLocalProgress(bookId: String, currentTime: Double, totalDuration: Double)
    suspend fun startPlaybackSession(bookId: String, deviceId: String, deviceName: String): Result<String>
    suspend fun syncPlaybackProgress(sessionId: String, timeListened: Double, currentTime: Double): Result<Unit>
    suspend fun syncStaticProgress(bookId: String, currentTime: Double, progress: Float, isFinished: Boolean): Result<Unit>
    suspend fun syncGlobalProgress(forceRefresh: Boolean = false): Result<Unit>
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
    suspend fun scanAndRelinkDownloads(): Result<Unit>
    suspend fun getOrphanedDownloadsSize(): Long
    suspend fun deleteOrphanedDownloads(): Result<Unit>
}

