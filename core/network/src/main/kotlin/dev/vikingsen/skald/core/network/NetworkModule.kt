package dev.vikingsen.skald.core.network

import android.util.Base64
import android.util.Log
import dev.vikingsen.skald.core.preferences.PreferencesManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.koin.dsl.bind
import org.koin.dsl.module

private val refreshMutex = Mutex()

data class JwtClaims(val exp: Long?, val iat: Long?)

fun parseJwt(token: String): JwtClaims? {
    val parts = token.split(".")
    if (parts.size < 2) return null
    return try {
        val payloadBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val payloadString = payloadBytes.decodeToString()
        val jsonElement = Json.parseToJsonElement(payloadString)
        val jsonObject = jsonElement.jsonObject
        val exp = jsonObject["exp"]?.jsonPrimitive?.longOrNull
        val iat = jsonObject["iat"]?.jsonPrimitive?.longOrNull
        JwtClaims(exp, iat)
    } catch (e: Exception) {
        null
    }
}

fun isAccessTokenExpired(token: String?): Boolean {
    if (token.isNullOrEmpty()) return true
    val claims = parseJwt(token) ?: return true
    val exp = claims.exp ?: return true
    val iat = claims.iat ?: return true
    val lifespan = exp - iat
    if (lifespan <= 0) return true
    val threshold = exp - (lifespan * 0.05)
    val currentTimeSeconds = System.currentTimeMillis() / 1000L
    return currentTimeSeconds >= threshold
}

fun isRefreshTokenExpired(token: String?): Boolean {
    if (token.isNullOrEmpty()) return true
    val claims = parseJwt(token) ?: return true
    val exp = claims.exp ?: return true
    val currentTimeSeconds = System.currentTimeMillis() / 1000L
    return currentTimeSeconds >= exp
}

suspend fun performRefreshHandshake(
    preferencesManager: PreferencesManager,
    serverUrl: String?,
    refreshToken: String?
): Boolean {
    if (serverUrl.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
        preferencesManager.clearTokens()
        return false
    }

    return try {
        val base = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        val client = org.koin.core.context.GlobalContext.get().get<HttpClient>()
        val response = client.post("${base}auth/refresh") {
            contentType(ContentType.Application.Json)
            headers["x-return-tokens"] = "true"
            headers["x-refresh-token"] = refreshToken
            setBody("{}")
        }

        if (response.status.value == 200) {
            val refreshResponse = response.body<RefreshResponse>()
            preferencesManager.saveTokens(
                refreshResponse.user.accessToken,
                refreshResponse.user.refreshToken
            )
            true
        } else {
            if (response.status.value == 401) {
                preferencesManager.clearTokens()
            }
            false
        }
    } catch (e: Exception) {
        false
    }
}

val coreNetworkModule = module {
    single<HttpClient> {
        val preferencesManager = get<PreferencesManager>()

        val apiPlugin = createClientPlugin("AbsApiPlugin") {
            onRequest { request, _ ->
                val path = request.url.encodedPath
                if (path.endsWith("/login") || path.endsWith("/auth/refresh")) {
                    return@onRequest
                }

                val serverUrl = preferencesManager.getServerUrl()
                val host = request.url.host
                val isRelative = host.isEmpty() || host == "localhost" || host == "127.0.0.1"

                if (isRelative && !serverUrl.isNullOrEmpty()) {
                    val base = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
                    val relative = request.url.encodedPath.let { if (it.startsWith("/")) it.substring(1) else it }
                    request.url.takeFrom(base + relative)
                }

                // Proactive refresh (Layer 1)
                val token = preferencesManager.getToken()
                val refreshToken = preferencesManager.getRefreshToken()

                if (isAccessTokenExpired(token)) {
                    if (isRefreshTokenExpired(refreshToken)) {
                        preferencesManager.clearTokens()
                    } else {
                        refreshMutex.withLock {
                            val currentToken = preferencesManager.getToken()
                            if (isAccessTokenExpired(currentToken)) {
                                performRefreshHandshake(
                                    preferencesManager = preferencesManager,
                                    serverUrl = serverUrl,
                                    refreshToken = refreshToken
                                )
                            }
                        }
                    }
                }

                val latestToken = preferencesManager.getToken()
                if (!latestToken.isNullOrEmpty() && !request.headers.contains("Authorization")) {
                    request.headers["Authorization"] = "Bearer $latestToken"
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

        // Reactive refresh (Layer 2)
        client.plugin(HttpSend).intercept { request ->
            var call = execute(request)

            if (call.response.status.value == 401) {
                val path = request.url.encodedPath
                if (!path.endsWith("/login") && !path.endsWith("/auth/refresh")) {
                    val serverUrl = preferencesManager.getServerUrl()
                    val refreshToken = preferencesManager.getRefreshToken()

                    val success = refreshMutex.withLock {
                        val currentToken = preferencesManager.getToken()
                        val requestToken = request.headers["Authorization"]?.removePrefix("Bearer ")

                        if (requestToken != currentToken) {
                            true
                        } else {
                            if (isRefreshTokenExpired(refreshToken)) {
                                preferencesManager.clearTokens()
                                false
                            } else {
                                performRefreshHandshake(
                                    preferencesManager = preferencesManager,
                                    serverUrl = serverUrl,
                                    refreshToken = refreshToken
                                )
                            }
                        }
                    }

                    if (success) {
                        val newToken = preferencesManager.getToken()
                        if (!newToken.isNullOrEmpty()) {
                            request.headers["Authorization"] = "Bearer $newToken"
                        }
                        call = execute(request)
                    }
                }
            }

            call
        }

        client
    }

    single<AudiobookshelfRemoteDataSource> {
        AudiobookshelfRemoteDataSourceImpl(get(), get())
    }
}
