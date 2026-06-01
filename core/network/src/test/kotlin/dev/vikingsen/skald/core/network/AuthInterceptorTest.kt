package dev.vikingsen.skald.core.network

import android.util.Base64
import dev.vikingsen.skald.core.preferences.PreferencesManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64 as JavaBase64

class AuthInterceptorTest {

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
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createToken(exp: Long, iat: Long): String {
        val payload = "{\"exp\":$exp,\"iat\":$iat}"
        val encodedPayload = JavaBase64.getUrlEncoder().withoutPadding().encodeToString(payload.encodeToByteArray())
        return "header.$encodedPayload.signature"
    }

    @Test
    fun testParseJwt() {
        val now = System.currentTimeMillis() / 1000L
        val token = createToken(now + 1000, now - 1000)
        
        val claims = parseJwt(token)
        assertEquals(now + 1000, claims?.exp)
        assertEquals(now - 1000, claims?.iat)
    }

    @Test
    fun testIsAccessTokenExpired_Expired() {
        val now = System.currentTimeMillis() / 1000L
        // Token expired 10 seconds ago
        val token = createToken(now - 10, now - 1000)
        assertTrue(isAccessTokenExpired(token))
    }

    @Test
    fun testIsAccessTokenExpired_Valid() {
        val now = System.currentTimeMillis() / 1000L
        // Token has lifespan of 1000 seconds, and expires in 800 seconds (well above 5% threshold of 50 seconds)
        val token = createToken(now + 800, now - 200)
        assertFalse(isAccessTokenExpired(token))
    }

    @Test
    fun testIsAccessTokenExpired_WithinPreemptiveThreshold() {
        val now = System.currentTimeMillis() / 1000L
        // Token has lifespan of 1000 seconds (exp - iat = 1000). 5% threshold is 50 seconds.
        // Token expires in 30 seconds (within the 5% threshold).
        val token = createToken(now + 30, now - 970)
        assertTrue(isAccessTokenExpired(token))
    }

    @Test
    fun testIsRefreshTokenExpired_Expired() {
        val now = System.currentTimeMillis() / 1000L
        val token = createToken(now - 10, now - 1000)
        assertTrue(isRefreshTokenExpired(token))
    }

    @Test
    fun testIsRefreshTokenExpired_Valid() {
        val now = System.currentTimeMillis() / 1000L
        val token = createToken(now + 1000, now - 1000)
        assertFalse(isRefreshTokenExpired(token))
    }
}
