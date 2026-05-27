package dev.vikingsen.absclientapp.data.repository

import android.content.Context
import dev.vikingsen.absclientapp.core.preferences.PreferencesManager
import dev.vikingsen.absclientapp.core.database.AppDatabase
import dev.vikingsen.absclientapp.core.database.BookEntity
import dev.vikingsen.absclientapp.core.database.LocalAudioFile
import dev.vikingsen.absclientapp.core.database.LocalChapter
import dev.vikingsen.absclientapp.core.database.PlaybackProgressEntity
import dev.vikingsen.absclientapp.data.mapper.toDomain
import dev.vikingsen.absclientapp.core.network.AudiobookshelfRemoteDataSource
import dev.vikingsen.absclientapp.core.model.AudioFile
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.model.DownloadStatusState
import dev.vikingsen.absclientapp.core.model.Library
import dev.vikingsen.absclientapp.core.model.LoggedUser
import dev.vikingsen.absclientapp.core.model.PlaybackProgress
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository
import android.app.DownloadManager
import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class AudiobookshelfRepositoryImpl(
    private val context: Context,
    private val db: AppDatabase,
    private val preferencesManager: PreferencesManager,
    private val remoteDataSource: AudiobookshelfRemoteDataSource
) : AudiobookshelfRepository {
    private val bookDao = db.bookDao()
    private val progressDao = db.playbackProgressDao()

    override suspend fun login(url: String, user: String, pass: String): Result<LoggedUser> = withContext(Dispatchers.IO) {
        runCatching {
            val (response, formattedUrl) = remoteDataSource.login(url, user, pass)
            preferencesManager.saveConnectionDetails(formattedUrl, user, response.user.token)
            response.toDomain()
        }
    }

    override suspend fun fetchLibraries(): Result<List<Library>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = remoteDataSource.fetchLibraries()
            response.libraries.map { it.toDomain() }
        }
    }

    override suspend fun syncLibraryBooks(libraryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = remoteDataSource.fetchLibraryItems(libraryId, 100)
            val books = response.results.map { item ->
                val existing = bookDao.getBookById(item.id)
                BookEntity(
                    id = item.id,
                    title = item.media.metadata.title ?: "Unknown Title",
                    author = item.media.metadata.authorName ?: "Unknown Author",
                    narrator = item.media.metadata.narratorName ?: "Unknown Narrator",
                    description = "",
                    duration = 0.0,
                    coverPath = existing?.coverPath,
                    isDownloaded = existing?.isDownloaded ?: false,
                    audioFiles = existing?.audioFiles ?: emptyList(),
                    chapters = existing?.chapters ?: emptyList()
                )
            }
            bookDao.insertAll(books)
            Unit
        }
    }

    override fun getBooksFlow(): Flow<List<Book>> {
        return bookDao.getAllBooksFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getAllProgressFlow(): Flow<List<PlaybackProgress>> {
        return progressDao.getAllProgressFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getBookWithProgressFlow(bookId: String): Flow<Pair<Book?, PlaybackProgress?>> {
        return bookDao.getBookByIdFlow(bookId).combine(progressDao.getProgressForBookFlow(bookId)) { bookEntity, progressEntity ->
            bookEntity?.toDomain() to progressEntity?.toDomain()
        }
    }

    override suspend fun fetchBookDetails(bookId: String): Result<Book> = withContext(Dispatchers.IO) {
        runCatching {
            val response = remoteDataSource.fetchBookDetails(bookId)
            val existing = bookDao.getBookById(bookId)

            val bookEntity = BookEntity(
                id = response.id,
                title = response.media.metadata.title,
                author = response.media.metadata.authorName ?: "Unknown Author",
                narrator = response.media.metadata.narratorName ?: "Unknown Narrator",
                description = response.media.metadata.description ?: "",
                duration = response.media.audioFiles?.sumOf { it.duration ?: 0.0 } ?: 0.0,
                coverPath = existing?.coverPath,
                isDownloaded = existing?.isDownloaded ?: false,
                audioFiles = response.media.audioFiles?.map { file ->
                    val existingFile = existing?.audioFiles?.find { it.ino == file.ino }
                    LocalAudioFile(
                        index = file.index,
                        ino = file.ino,
                        duration = file.duration ?: 0.0,
                        mimeType = file.mimeType,
                        filename = file.metadata?.filename ?: "file_${file.index}",
                        size = file.metadata?.size ?: 0L,
                        localPath = existingFile?.localPath,
                        downloadStatus = existingFile?.downloadStatus ?: "NOT_DOWNLOADED"
                    )
                } ?: emptyList(),
                chapters = response.media.chapters?.map { chapter ->
                    LocalChapter(
                        start = chapter.start,
                        end = chapter.end,
                        title = chapter.title
                    )
                } ?: emptyList()
            )

            bookDao.insertBook(bookEntity)

            // Sync progress from server if exists
            fetchProgressFromServer(bookId)

            bookEntity.toDomain()
        }.recoverCatching { exception ->
            val local = bookDao.getBookById(bookId)
            local?.toDomain() ?: throw exception
        }
    }

    private suspend fun fetchProgressFromServer(bookId: String) {
        runCatching {
            val response = remoteDataSource.fetchProgressFromServer(bookId) ?: return@runCatching
            val currentProgress = progressDao.getProgressForBook(bookId)
            if (currentProgress == null || response.currentTime > currentProgress.currentTime) {
                progressDao.insertProgress(
                    PlaybackProgressEntity(
                        bookId = bookId,
                        currentTime = response.currentTime,
                        progress = response.progress,
                        isFinished = response.isFinished,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    override suspend fun saveLocalProgress(bookId: String, currentTime: Double, totalDuration: Double) {
        val progress = if (totalDuration > 0) (currentTime / totalDuration).toFloat() else 0f
        val entity = PlaybackProgressEntity(
            bookId = bookId,
            currentTime = currentTime,
            progress = progress,
            isFinished = progress >= 0.99f,
            lastUpdated = System.currentTimeMillis()
        )
        progressDao.insertProgress(entity)
    }

    override suspend fun startPlaybackSession(bookId: String, deviceId: String, deviceName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val response = remoteDataSource.startPlaybackSession(bookId, deviceId, deviceName)
            response.id
        }
    }

    override suspend fun syncPlaybackProgress(sessionId: String, timeListened: Double, currentTime: Double): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            remoteDataSource.syncPlaybackProgress(sessionId, timeListened, currentTime)
        }
    }

    override suspend fun syncStaticProgress(bookId: String, currentTime: Double, progress: Float, isFinished: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            remoteDataSource.syncStaticProgress(bookId, currentTime, progress, isFinished)
        }
    }

    override suspend fun enqueueBookDownloads(bookId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val book = bookDao.getBookById(bookId) ?: throw Exception("Book not found")
            val serverUrl = preferencesManager.getServerUrl() ?: throw Exception("Server URL not configured")
            val token = preferencesManager.getToken() ?: throw Exception("Auth token not configured")
            
            val filesToDownload = book.audioFiles.filter { it.downloadStatus != "COMPLETED" }
            if (filesToDownload.isEmpty()) return@runCatching

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val sanitizedBase = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"

            for (file in filesToDownload) {
                val downloadUrl = "${sanitizedBase}api/items/$bookId/file/${file.ino}/download"
                val folder = File(context.getExternalFilesDir(null), "downloads/$bookId")
                if (!folder.exists()) folder.mkdirs()
                
                val extension = file.filename.substringAfterLast('.', "mp3")
                val destinationFile = File(folder, "${file.ino}.$extension")

                val request = DownloadManager.Request(Uri.parse(downloadUrl))
                    .setTitle(file.filename)
                    .setDescription("Downloading file for ${book.title}")
                    .setDestinationUri(Uri.fromFile(destinationFile))
                    .addRequestHeader("Authorization", "Bearer $token")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

                downloadManager.enqueue(request)
                updateAudioFileDownloadStatus(bookId, file.ino, "DOWNLOADING", null)
            }
        }
    }

    override fun getBookDownloadFlow(bookId: String): Flow<DownloadStatusState> = flow {
        while (true) {
            val book = bookDao.getBookById(bookId)
            if (book == null) {
                emit(DownloadStatusState.Progress(0f))
                delay(1000)
                continue
            }

            val files = book.audioFiles
            val totalFiles = files.size
            if (totalFiles == 0) {
                emit(DownloadStatusState.Progress(0f))
                delay(1000)
                continue
            }

            val completedCount = files.count { it.downloadStatus == "COMPLETED" }
            val downloadingFiles = files.filter { it.downloadStatus == "DOWNLOADING" }

            if (completedCount == totalFiles) {
                val path = files.firstOrNull()?.localPath?.let { File(it).parent } ?: ""
                emit(DownloadStatusState.Completed(path))
                break
            }

            if (downloadingFiles.isEmpty()) {
                if (completedCount > 0) {
                    emit(DownloadStatusState.Progress(completedCount.toFloat() / totalFiles))
                } else {
                    emit(DownloadStatusState.Progress(0f))
                }
                delay(1000)
                continue
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterByStatus(
                DownloadManager.STATUS_RUNNING or
                DownloadManager.STATUS_PAUSED or
                DownloadManager.STATUS_PENDING
            )

            var activeProgressSum = 0f
            var activeCount = 0

            runCatching {
                downloadManager.query(query).use { cursor ->
                    while (cursor.moveToNext()) {
                        val uriColumn = cursor.getColumnIndex(DownloadManager.COLUMN_URI)
                        val remoteUri = cursor.getString(uriColumn) ?: continue

                        val regex = Regex(".*/api/items/([^/]+)/file/([^/]+)/download.*")
                        val match = regex.matchEntire(remoteUri) ?: continue
                        val itemBookId = match.groupValues[1]
                        val itemIno = match.groupValues[2]

                        if (itemBookId == bookId) {
                            val bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val bytesTotal = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            if (bytesTotal > 0) {
                                activeProgressSum += bytesDownloaded.toFloat() / bytesTotal
                            } else {
                                activeProgressSum += 0.1f
                            }
                            activeCount++
                        }
                    }
                }
            }

            val progressSum = completedCount.toFloat() + activeProgressSum.coerceAtMost(downloadingFiles.size.toFloat())
            val totalProgress = (progressSum / totalFiles).coerceIn(0f, 1f)
            emit(DownloadStatusState.Progress(totalProgress))
            delay(1000)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun updateAudioFileDownloadStatus(bookId: String, ino: String, status: String, localPath: String?) {
        val book = bookDao.getBookById(bookId) ?: return
        val updatedFiles = book.audioFiles.map {
            if (it.ino == ino) {
                it.copy(localPath = localPath, downloadStatus = status)
            } else {
                it
            }
        }
        val isAllDownloaded = updatedFiles.all { it.downloadStatus == "COMPLETED" }
        bookDao.insertBook(book.copy(audioFiles = updatedFiles, isDownloaded = isAllDownloaded))
    }

    override suspend fun deleteLocalBookFiles(bookId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val book = bookDao.getBookById(bookId) ?: throw Exception("Book not found")

            // 1. Cancel active downloads in DownloadManager
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query()
            val idsToRemove = mutableListOf<Long>()
            runCatching {
                downloadManager.query(query).use { cursor ->
                    val uriColumn = cursor.getColumnIndex(DownloadManager.COLUMN_URI)
                    val idColumn = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                    if (uriColumn != -1 && idColumn != -1) {
                        while (cursor.moveToNext()) {
                            val remoteUri = cursor.getString(uriColumn) ?: continue
                            val id = cursor.getLong(idColumn)

                            val regex = Regex(".*/api/items/([^/]+)/file/([^/]+)/download.*")
                            val match = regex.matchEntire(remoteUri) ?: continue
                            val itemBookId = match.groupValues[1]

                            if (itemBookId == bookId) {
                                idsToRemove.add(id)
                            }
                        }
                    }
                }
            }
            if (idsToRemove.isNotEmpty()) {
                downloadManager.remove(*idsToRemove.toLongArray())
            }

            // 2. Delete local directory recursively
            val folder = File(context.getExternalFilesDir(null), "downloads/$bookId")
            if (folder.exists()) {
                val deleted = folder.deleteRecursively()
                if (!deleted) {
                    throw Exception("Failed to delete local book files directory")
                }
            }

            // 3. Update database
            val updatedFiles = book.audioFiles.map {
                it.copy(localPath = null, downloadStatus = "NOT_DOWNLOADED")
            }
            val updatedBook = book.copy(audioFiles = updatedFiles, isDownloaded = false)
            bookDao.insertBook(updatedBook)
        }
    }

    override suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
    }
}
