package dev.vikingsen.absclientapp.core.network

import android.util.Log
import dev.vikingsen.absclientapp.core.preferences.PreferencesManager
import dev.vikingsen.absclientapp.core.network.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import java.io.File
import java.io.FileOutputStream

interface AudiobookshelfRemoteDataSource {
    suspend fun login(url: String, user: String, pass: String): Pair<LoggedUserResponse, String>
    suspend fun fetchLibraries(): LibrariesResponse
    suspend fun fetchLibraryItems(libraryId: String, limit: Int): LibraryItemsResponse
    suspend fun fetchBookDetails(bookId: String): BookResponse
    suspend fun fetchProgressFromServer(bookId: String): MediaProgressResponse?
    suspend fun startPlaybackSession(bookId: String, deviceId: String, deviceName: String): PlaybackSessionResponse
    suspend fun syncPlaybackProgress(sessionId: String, timeListened: Double, currentTime: Double)
    suspend fun syncStaticProgress(bookId: String, currentTime: Double, progress: Float, isFinished: Boolean)
    fun downloadFile(bookId: String, ino: String, destinationFile: File, totalBytes: Long): Flow<Float>
}

class AudiobookshelfRemoteDataSourceImpl(
    private val preferencesManager: PreferencesManager,
    engine: io.ktor.client.engine.HttpClientEngine? = null
) : AudiobookshelfRemoteDataSource {

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

    private val client = engine?.let {
        HttpClient(it) {
            setupClient()
        }
    } ?: HttpClient {
        setupClient()
    }

    private fun io.ktor.client.HttpClientConfig<*>.setupClient() {
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

    override suspend fun login(url: String, user: String, pass: String): Pair<LoggedUserResponse, String> = withContext(Dispatchers.IO) {
        var formattedUrl = url.trim().removeSuffix("/")
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://$formattedUrl"
        }
        val base = "$formattedUrl/"
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
        Pair(response, formattedUrl)
    }

    override suspend fun fetchLibraries(): LibrariesResponse = withContext(Dispatchers.IO) {
        val httpResponse = client.get("api/libraries")
        if (httpResponse.status.value >= 400) {
            throw Exception("Failed to fetch libraries: Status ${httpResponse.status.value}")
        }
        httpResponse.body()
    }

    override suspend fun fetchLibraryItems(libraryId: String, limit: Int): LibraryItemsResponse = withContext(Dispatchers.IO) {
        val httpResponse = client.get("api/libraries/$libraryId/items") {
            url.parameters.append("limit", limit.toString())
        }
        if (httpResponse.status.value >= 400) {
            throw Exception("Failed to sync library books: Status ${httpResponse.status.value}")
        }
        httpResponse.body()
    }

    override suspend fun fetchBookDetails(bookId: String): BookResponse = withContext(Dispatchers.IO) {
        val httpResponse = client.get("api/items/$bookId")
        if (httpResponse.status.value >= 400) {
            throw Exception("Failed to fetch book details: Status ${httpResponse.status.value}")
        }
        httpResponse.body()
    }

    override suspend fun fetchProgressFromServer(bookId: String): MediaProgressResponse? = withContext(Dispatchers.IO) {
        val httpResponse = client.get("api/me/progress/$bookId")
        if (httpResponse.status.value == 404) {
            return@withContext null
        } else if (httpResponse.status.value >= 400) {
            throw Exception("Failed to fetch progress: Status ${httpResponse.status.value}")
        }
        httpResponse.body()
    }

    override suspend fun startPlaybackSession(bookId: String, deviceId: String, deviceName: String): PlaybackSessionResponse = withContext(Dispatchers.IO) {
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
        httpResponse.body()
    }

    override suspend fun syncPlaybackProgress(sessionId: String, timeListened: Double, currentTime: Double): Unit = withContext(Dispatchers.IO) {
        val httpResponse = client.post("api/session/$sessionId/sync") {
            contentType(ContentType.Application.Json)
            setBody(ProgressSyncRequest(timeListened, currentTime))
        }
        if (httpResponse.status.value >= 400) {
            throw Exception("Failed to sync playback progress: Status ${httpResponse.status.value}")
        }
    }

    override suspend fun syncStaticProgress(bookId: String, currentTime: Double, progress: Float, isFinished: Boolean): Unit = withContext(Dispatchers.IO) {
        val httpResponse = client.post("api/me/progress") {
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
        if (httpResponse.status.value >= 400) {
            throw Exception("Failed to sync static progress: Status ${httpResponse.status.value}")
        }
    }

    override fun downloadFile(bookId: String, ino: String, destinationFile: File, totalBytes: Long): Flow<Float> = flow {
        val responseStatement: HttpStatement = client.prepareGet("api/items/$bookId/file/$ino/download")
        responseStatement.execute { httpResponse ->
            if (httpResponse.status.value >= 400) {
                throw Exception("Failed to download file: Status ${httpResponse.status.value}")
            }
            val channel: ByteReadChannel = httpResponse.body()
            var bytesWritten = 0L
            FileOutputStream(destinationFile).use { output ->
                val buffer = ByteArray(8192)
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    bytesWritten += read
                    if (totalBytes > 0) {
                        emit(bytesWritten.toFloat() / totalBytes)
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
