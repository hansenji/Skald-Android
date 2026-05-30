package dev.vikingsen.absclientapp.core.preferences

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers


@Serializable
data class SecureTokens(
    val accessToken: String? = null,
    val refreshToken: String? = null
)

object SecureTokensSerializer : Serializer<SecureTokens> {
    override val defaultValue: SecureTokens = SecureTokens()

    override suspend fun readFrom(input: InputStream): SecureTokens {
        return try {
            Json.decodeFromString(SecureTokens.serializer(), input.readBytes().decodeToString())
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: SecureTokens, output: OutputStream) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            output.write(Json.encodeToString(SecureTokens.serializer(), t).encodeToByteArray())
        }
    }
}
