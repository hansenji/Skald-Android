package dev.vikingsen.skald.core.network

import android.util.Base64
import android.util.Log
import dev.vikingsen.skald.core.preferences.PreferencesManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Base64 as JavaBase64
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class AudiobookshelfRemoteDataSourceTest {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var client: HttpClient
    private lateinit var dataSource: AudiobookshelfRemoteDataSource

    private var tokenState: String? = null
    private var refreshTokenState: String? = null
    private var serverUrlState: String? = null

    private var requestHandler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = {
        respond("", HttpStatusCode.NotFound)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun createToken(exp: Long, iat: Long): String {
        val payload = "{\"exp\":$exp,\"iat\":$iat}"
        val encodedPayload = JavaBase64.getUrlEncoder().withoutPadding().encodeToString(payload.encodeToByteArray())
        return "header.$encodedPayload.signature"
    }

    @Before
    fun setUp() {
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } answers {
            val input = firstArg<String>()
            val sanitized = input.replace('-', '+').replace('_', '/')
            JavaBase64.getDecoder().decode(sanitized)
        }
        every { Base64.encodeToString(any<ByteArray>(), any()) } answers {
            val input = firstArg<ByteArray>()
            JavaBase64.getUrlEncoder().withoutPadding().encodeToString(input)
        }

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.e(any(), any()) } returns 0

        tokenState = null
        refreshTokenState = null
        serverUrlState = null

        preferencesManager = mockk(relaxed = true)

        every { preferencesManager.getToken() } answers { tokenState }
        every { preferencesManager.getRefreshToken() } answers { refreshTokenState }
        every { preferencesManager.getServerUrl() } answers { serverUrlState }
        coEvery { preferencesManager.saveTokens(any(), any()) } answers {
            tokenState = firstArg()
            refreshTokenState = secondArg()
        }
        coEvery { preferencesManager.clearTokens() } answers {
            tokenState = null
            refreshTokenState = null
        }

        val mockEngine = MockEngine { request ->
            requestHandler(request)
        }

        client = createHttpClient(preferencesManager, mockEngine)
        dataSource = AudiobookshelfRemoteDataSourceImpl(preferencesManager, client)

        startKoin {
            modules(module {
                single { preferencesManager }
                single { client }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    @Test
    fun testLogin_Success() {
        runBlocking {
            val loginResponse = LoggedUserResponse(
                user = UserDetails(
                    token = "jwt-access-token",
                    username = "testuser",
                    id = "user-1",
                    accessToken = "jwt-access-token",
                    refreshToken = "jwt-refresh-token"
                )
            )

            requestHandler = { request ->
                assertEquals("/login", request.url.encodedPath)
                assertEquals("true", request.headers["x-return-tokens"])
                respond(
                    content = json.encodeToString(loginResponse),
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json")
                )
            }

            val (response, normalizedUrl) = dataSource.login("http://localhost:8080", "testuser", "password")
            assertEquals("testuser", response.user.username)
            assertEquals("jwt-access-token", response.user.token)
            assertEquals("http://localhost:8080", normalizedUrl)
        }
    }

    @Test
    fun testLogin_Failure() {
        runBlocking {
            requestHandler = { _ ->
                respond(
                    content = "Unauthorized",
                    status = HttpStatusCode.Unauthorized
                )
            }

            val exception = try {
                dataSource.login("http://localhost:8080", "testuser", "wrong")
                null
            } catch (e: Exception) {
                e
            }
            assertNotNull(exception)
            assertTrue(exception!!.message!!.contains("Invalid username or password"))
        }
    }

    @Test
    fun testHostNormalization() {
        runBlocking {
            serverUrlState = "http://my-abs-server.com/"

            val librariesResponse = LibrariesResponse(
                libraries = listOf(LibraryResponse(id = "lib-1", name = "Audiobooks", type = "audiobooks"))
            )

            requestHandler = { request ->
                assertEquals("http://my-abs-server.com/api/libraries", request.url.toString())
                respond(
                    content = json.encodeToString(librariesResponse),
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json")
                )
            }

            val response = dataSource.fetchLibraries()
            assertEquals(1, response.libraries.size)
            assertEquals("lib-1", response.libraries[0].id)
        }
    }

    @Test
    fun testAuthHeaderInjection() {
        runBlocking {
            serverUrlState = "http://localhost/"
            val now = System.currentTimeMillis() / 1000L
            val validToken = createToken(now + 1000, now - 1000)
            tokenState = validToken

            val librariesResponse = LibrariesResponse(libraries = emptyList())

            requestHandler = { request ->
                assertEquals("Bearer $validToken", request.headers["Authorization"])
                respond(
                    content = json.encodeToString(librariesResponse),
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json")
                )
            }

            dataSource.fetchLibraries()
        }
    }

    @Test
    fun testLayer1ProactiveRefresh() {
        runBlocking {
            serverUrlState = "http://localhost"

            val now = System.currentTimeMillis() / 1000L
            tokenState = createToken(now - 10, now - 1000)
            refreshTokenState = createToken(now + 1000, now - 1000)

            val refreshResponse = RefreshResponse(
                user = RefreshUserDetails(
                    accessToken = "new_access_token",
                    refreshToken = "new_refresh_token"
                )
            )

            val librariesResponse = LibrariesResponse(libraries = emptyList())
            val requests = mutableListOf<String>()

            requestHandler = { request ->
                val path = request.url.encodedPath
                requests.add(path)
                if (path.endsWith("/auth/refresh")) {
                    assertEquals("true", request.headers["x-return-tokens"])
                    assertEquals(refreshTokenState, request.headers["x-refresh-token"])
                    respond(
                        content = json.encodeToString(refreshResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json")
                    )
                } else if (path.endsWith("/api/libraries")) {
                    assertEquals("Bearer new_access_token", request.headers["Authorization"])
                    respond(
                        content = json.encodeToString(librariesResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json")
                    )
                } else {
                    respondError(HttpStatusCode.NotFound)
                }
            }

            dataSource.fetchLibraries()

            assertEquals(2, requests.size)
            assertEquals("/auth/refresh", requests[0])
            assertEquals("/api/libraries", requests[1])
            assertEquals("new_access_token", tokenState)
            assertEquals("new_refresh_token", refreshTokenState)
        }
    }

    @Test
    fun testLayer2ReactiveRefresh() {
        runBlocking {
            serverUrlState = "http://localhost"

            val now = System.currentTimeMillis() / 1000L
            tokenState = createToken(now + 500, now - 500)
            refreshTokenState = createToken(now + 1000, now - 1000)

            val refreshResponse = RefreshResponse(
                user = RefreshUserDetails(
                    accessToken = "new_access_token_reactive",
                    refreshToken = "new_refresh_token_reactive"
                )
            )

            val librariesResponse = LibrariesResponse(libraries = emptyList())
            val requests = mutableListOf<Pair<String, String?>>()

            requestHandler = { request ->
                val path = request.url.encodedPath
                val authHeader = request.headers["Authorization"]
                requests.add(path to authHeader)

                if (path.endsWith("/api/libraries")) {
                    if (authHeader == "Bearer ${createToken(now + 500, now - 500)}") {
                        respond(
                            content = "Unauthorized",
                            status = HttpStatusCode.Unauthorized
                        )
                    } else if (authHeader == "Bearer new_access_token_reactive") {
                        respond(
                            content = json.encodeToString(librariesResponse),
                            status = HttpStatusCode.OK,
                            headers = headersOf("Content-Type", "application/json")
                        )
                    } else {
                        respondError(HttpStatusCode.BadRequest)
                    }
                } else if (path.endsWith("/auth/refresh")) {
                    respond(
                        content = json.encodeToString(refreshResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json")
                    )
                } else {
                    respondError(HttpStatusCode.NotFound)
                }
            }

            dataSource.fetchLibraries()

            assertEquals(3, requests.size)
            assertEquals("/api/libraries", requests[0].first)
            assertEquals("Bearer ${createToken(now + 500, now - 500)}", requests[0].second)

            assertEquals("/auth/refresh", requests[1].first)

            assertEquals("/api/libraries", requests[2].first)
            assertEquals("Bearer new_access_token_reactive", requests[2].second)

            assertEquals("new_access_token_reactive", tokenState)
            assertEquals("new_refresh_token_reactive", refreshTokenState)
        }
    }

    @Test
    fun testDownloadFile_Progress() {
        runBlocking {
            serverUrlState = "http://localhost"
            val dataBytes = ByteArray(10) { it.toByte() }

            requestHandler = { request ->
                assertEquals("/api/items/book-1/file/ino-1/download", request.url.encodedPath)
                respond(
                    content = ByteReadChannel(dataBytes),
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/octet-stream")
                )
            }

            val tempFile = File.createTempFile("skald_download_test", ".tmp")
            tempFile.deleteOnExit()

            val progressList = dataSource.downloadFile("book-1", "ino-1", tempFile, totalBytes = 10).toList()

            assertTrue(progressList.isNotEmpty())
            assertEquals(1.0f, progressList.last())
            assertEquals(10L, tempFile.length())
            assertEquals(0.toByte(), tempFile.readBytes()[0])
            assertEquals(9.toByte(), tempFile.readBytes()[9])
        }
    }

    @Test
    fun testConditionalRequest_NotModified() {
        runBlocking {
            serverUrlState = "http://localhost"

            requestHandler = { request ->
                assertEquals("etag-123", request.headers["If-None-Match"])
                respond(
                    content = "",
                    status = HttpStatusCode.NotModified
                )
            }

            val result = dataSource.fetchLibraryItems("lib-1", limit = 10, page = 1, etag = "etag-123")
            assertEquals(NetworkResult.NotModified, result)
        }
    }

    @Test
    fun testConditionalRequest_SuccessWithETag() {
        runBlocking {
            serverUrlState = "http://localhost"
            val mockItemsResponse = LibraryItemsResponse(results = emptyList(), total = 0)

            requestHandler = { _ ->
                respond(
                    content = json.encodeToString(mockItemsResponse),
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        "Content-Type" to listOf("application/json"),
                        "ETag" to listOf("new-etag-456")
                    )
                )
            }

            val result = dataSource.fetchLibraryItems("lib-1", limit = 10, page = 1, etag = "etag-123")
            assertTrue(result is NetworkResult.Success)
            val successResult = result as NetworkResult.Success
            assertEquals("new-etag-456", successResult.etag)
        }
    }
}
