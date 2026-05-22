package com.example.absclientapp.data.repository

import android.content.Context
import com.example.absclientapp.data.PreferencesManager
import com.example.absclientapp.data.database.AppDatabase
import com.example.absclientapp.data.database.BookEntity
import com.example.absclientapp.data.database.LocalAudioFile
import com.example.absclientapp.data.database.LocalChapter
import com.example.absclientapp.data.database.PlaybackProgressEntity
import com.example.absclientapp.data.model.BookResponse
import com.example.absclientapp.data.model.CredentialsLoginRequest
import com.example.absclientapp.data.model.LibrariesResponse
import com.example.absclientapp.data.model.LibraryItemsResponse
import com.example.absclientapp.data.model.LibraryResponse
import com.example.absclientapp.data.model.LoggedUserResponse
import com.example.absclientapp.data.model.MediaProgressResponse
import com.example.absclientapp.data.model.PlaybackSessionResponse
import com.example.absclientapp.data.model.PlaybackStartRequest
import com.example.absclientapp.data.model.ProgressSyncRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.LogLevel
import android.util.Log
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import com.example.absclientapp.data.model.DeviceInfo
import io.ktor.utils.io.readAvailable
import java.io.File
import java.io.FileOutputStream

class AudiobookshelfRepository(
    private val context: Context,
    private val db: AppDatabase,
    val preferencesManager: PreferencesManager
) {
    private val bookDao = db.bookDao()
    private val progressDao = db.playbackProgressDao()

    private val apiPlugin = createClientPlugin("AbsApiPlugin") {
        onRequest { request, _ ->
            val serverUrl = preferencesManager.getServerUrl()
            val token = preferencesManager.getToken()

            val host = request.url.host
            val isRelative = host.isEmpty() || host == "localhost" || host == "127.0.0.1"

            if (isRelative && !serverUrl.isNullOrEmpty()) {
                val base = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
                val relative = request.url.encodedPath.let { if (it.startsWith("/")) it.substring(1) else it }
                request.url.takeFrom(base + relative)
            }

            if (!token.isNullOrEmpty() && !request.headers.contains("Authorization")) {
                request.headers["Authorization"] = "Bearer $token"
            }
        }
    }

    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 30000
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("KtorClient", message)
                }
            }
            level = LogLevel.ALL
        }
        install(apiPlugin)
    }

    suspend fun login(url: String, user: String, pass: String): Result<LoggedUserResponse> = withContext(Dispatchers.IO) {
        runCatching {
            var formattedUrl = url.trim()
            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "https://$formattedUrl"
            }
            val base = if (formattedUrl.endsWith("/")) formattedUrl else "$formattedUrl/"
            val httpResponse = client.post("${base}login") {
                contentType(ContentType.Application.Json)
                setBody(CredentialsLoginRequest(user, pass))
            }
            
            if (httpResponse.status.value == 401) {
                throw Exception("Invalid username or password")
            } else if (httpResponse.status.value >= 400) {
                throw Exception("Login failed: Status ${httpResponse.status.value}")
            }
            
            val response: LoggedUserResponse = httpResponse.body()
            preferencesManager.saveConnectionDetails(formattedUrl, user, response.user.token)
            response
        }
    }

    suspend fun fetchLibraries(): Result<List<LibraryResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            val httpResponse = client.get("api/libraries")
            if (httpResponse.status.value >= 400) {
                throw Exception("Failed to fetch libraries: Status ${httpResponse.status.value}")
            }
            val response: LibrariesResponse = httpResponse.body()
            response.libraries
        }
    }

    suspend fun syncLibraryBooks(libraryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val httpResponse = client.get("api/libraries/$libraryId/items") {
                // limit to large number to get all books for caching
                url.parameters.append("limit", "100")
            }
            if (httpResponse.status.value >= 400) {
                throw Exception("Failed to sync library books: Status ${httpResponse.status.value}")
            }
            val response: LibraryItemsResponse = httpResponse.body()

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
            Result.success(Unit)
        }.getOrElse {
            Result.failure(it)
        }
    }

    fun getBooksFlow(): Flow<List<BookEntity>> = bookDao.getAllBooksFlow()

    fun getBookWithProgressFlow(bookId: String): Flow<Pair<BookEntity?, PlaybackProgressEntity?>> {
        return bookDao.getBookByIdFlow(bookId).combine(progressDao.getProgressForBookFlow(bookId)) { book, progress ->
            book to progress
        }
    }

    suspend fun fetchBookDetails(bookId: String): Result<BookEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val httpResponse = client.get("api/items/$bookId")
            if (httpResponse.status.value >= 400) {
                throw Exception("Failed to fetch book details: Status ${httpResponse.status.value}")
            }
            val response: BookResponse = httpResponse.body()
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

            Result.success(bookEntity)
        }.getOrElse {
            val local = bookDao.getBookById(bookId)
            if (local != null) Result.success(local) else Result.failure(it)
        }
    }

    private suspend fun fetchProgressFromServer(bookId: String) {
        runCatching {
            val httpResponse = client.get("api/me/progress/$bookId")
            if (httpResponse.status.value == 404) {
                return@runCatching
            } else if (httpResponse.status.value >= 400) {
                throw Exception("Failed to fetch progress: Status ${httpResponse.status.value}")
            }
            val response: MediaProgressResponse = httpResponse.body()
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

    suspend fun saveLocalProgress(bookId: String, currentTime: Double, totalDuration: Double) {
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

    suspend fun startPlaybackSession(bookId: String, deviceId: String, deviceName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = PlaybackStartRequest(
                deviceInfo = DeviceInfo(deviceId = deviceId, deviceName = deviceName)
            )
            val httpResponse = client.post("api/items/$bookId/play") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (httpResponse.status.value >= 400) {
                throw Exception("Failed to start playback session: Status ${httpResponse.status.value}")
            }
            val response: PlaybackSessionResponse = httpResponse.body()
            response.id
        }
    }

    suspend fun syncPlaybackProgress(sessionId: String, timeListened: Double, currentTime: Double): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.post("api/session/$sessionId/sync") {
                contentType(ContentType.Application.Json)
                setBody(ProgressSyncRequest(timeListened, currentTime))
            }
            Result.success(Unit)
        }.getOrElse {
            Result.failure(it)
        }
    }

    suspend fun syncStaticProgress(bookId: String, currentTime: Double, progress: Float, isFinished: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.post("api/me/progress") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonArray {
                        addJsonObject {
                            put("libraryItemId", bookId)
                            put("currentTime", currentTime)
                            put("progress", progress)
                            put("isFinished", isFinished)
                        }
                    }
                )
            }
            Result.success(Unit)
        }.getOrElse {
            Result.failure(it)
        }
    }

    fun downloadAudioFile(bookId: String, audioFile: LocalAudioFile): Flow<DownloadState> = flow {
        emit(DownloadState.Progress(0f))
        
        val folder = File(context.getExternalFilesDir(null), "downloads/$bookId")
        if (!folder.exists()) folder.mkdirs()
        
        val destinationFile = File(folder, "${audioFile.ino}.${audioFile.filename.substringAfterLast('.', "mp3")}")
        
        runCatching {
            val responseStatement: HttpStatement = client.prepareGet("api/items/$bookId/file/${audioFile.ino}/download")
            responseStatement.execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()
                val totalBytes = audioFile.size
                var bytesWritten = 0L
                
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        bytesWritten += read
                        if (totalBytes > 0) {
                            emit(DownloadState.Progress(bytesWritten.toFloat() / totalBytes))
                        }
                    }
                }
            }
            
            // Update file download path in DB
            val book = bookDao.getBookById(bookId)
            if (book != null) {
                val updatedFiles = book.audioFiles.map {
                    if (it.ino == audioFile.ino) {
                        it.copy(localPath = destinationFile.absolutePath, downloadStatus = "COMPLETED")
                    } else it
                }
                val isAllDownloaded = updatedFiles.all { it.downloadStatus == "COMPLETED" }
                bookDao.insertBook(book.copy(audioFiles = updatedFiles, isDownloaded = isAllDownloaded))
            }
            
            emit(DownloadState.Completed(destinationFile.absolutePath))
        }.getOrElse {
            emit(DownloadState.Error(it))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
    }
}

sealed interface DownloadState {
    data class Progress(val progress: Float) : DownloadState
    data class Completed(val path: String) : DownloadState
    data class Error(val error: Throwable) : DownloadState
}
