package dev.vikingsen.skald.feature.assistant.engine

import dev.vikingsen.skald.feature.assistant.mcp.SkaldMcpToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FastSemanticMatcherTest {

    @Test
    fun `test resume patterns`() {
        listOf("resume", "continue", "play", "unpause", "resume playback", "continue my book").forEach { phrase ->
            val match = FastSemanticMatcher.match(phrase)
            assertNotNull("Expected match for '$phrase'", match)
            assertEquals(SkaldMcpToolRegistry.TOOL_RESUME_PLAYBACK, match?.toolName)
        }
    }

    @Test
    fun `test pause patterns`() {
        listOf("pause", "stop", "pause playback", "stop the book").forEach { phrase ->
            val match = FastSemanticMatcher.match(phrase)
            assertNotNull("Expected match for '$phrase'", match)
            assertEquals(SkaldMcpToolRegistry.TOOL_PAUSE_PLAYBACK, match?.toolName)
        }
    }

    @Test
    fun `test sleep timer patterns`() {
        val thirtyMin = FastSemanticMatcher.match("sleep timer 30 minutes")
        assertNotNull(thirtyMin)
        assertEquals(SkaldMcpToolRegistry.TOOL_SET_SLEEP_TIMER, thirtyMin?.toolName)
        assertEquals(30, thirtyMin?.arguments?.get("minutes"))

        val fifteenM = FastSemanticMatcher.match("set a sleep timer for 15m")
        assertNotNull(fifteenM)
        assertEquals(15, fifteenM?.arguments?.get("minutes"))

        val endOfChapter = FastSemanticMatcher.match("sleep timer end of chapter")
        assertNotNull(endOfChapter)
        assertEquals(true, endOfChapter?.arguments?.get("endOfChapter"))

        val cancel = FastSemanticMatcher.match("cancel sleep timer")
        assertNotNull(cancel)
        assertEquals(0, cancel?.arguments?.get("minutes"))
    }

    @Test
    fun `test speed patterns`() {
        val speedMatch = FastSemanticMatcher.match("set speed to 1.5x")
        assertNotNull(speedMatch)
        assertEquals(SkaldMcpToolRegistry.TOOL_SET_PLAYBACK_SPEED, speedMatch?.toolName)
        assertEquals(1.5f, speedMatch?.arguments?.get("speed"))

        val speedMatch2 = FastSemanticMatcher.match("speed 2.0")
        assertNotNull(speedMatch2)
        assertEquals(2.0f, speedMatch2?.arguments?.get("speed"))
    }

    @Test
    fun `test play search patterns`() {
        val playDune = FastSemanticMatcher.match("play Dune")
        assertNotNull(playDune)
        assertEquals(SkaldMcpToolRegistry.TOOL_SEARCH_AND_PLAY, playDune?.toolName)
        assertEquals("Dune", playDune?.arguments?.get("query"))

        val searchAndPlayAesop = FastSemanticMatcher.match("search and play Aesop")
        assertNotNull(searchAndPlayAesop)
        assertEquals(SkaldMcpToolRegistry.TOOL_SEARCH_AND_PLAY, searchAndPlayAesop?.toolName)
        assertEquals("Aesop", searchAndPlayAesop?.arguments?.get("query"))

        val listenHobbit = FastSemanticMatcher.match("listen to The Hobbit")
        assertNotNull(listenHobbit)
        assertEquals("The Hobbit", listenHobbit?.arguments?.get("query"))
    }

    @Test
    fun `test download patterns`() {
        val downloadMatch = FastSemanticMatcher.match("download Project Hail Mary")
        assertNotNull(downloadMatch)
        assertEquals(SkaldMcpToolRegistry.TOOL_SEARCH_AND_DOWNLOAD, downloadMatch?.toolName)
        assertEquals("Project Hail Mary", downloadMatch?.arguments?.get("query"))

        val searchAndDownload = FastSemanticMatcher.match("search and download Dune")
        assertNotNull(searchAndDownload)
        assertEquals(SkaldMcpToolRegistry.TOOL_SEARCH_AND_DOWNLOAD, searchAndDownload?.toolName)
        assertEquals("Dune", searchAndDownload?.arguments?.get("query"))
    }

    @Test
    fun `test status patterns`() {
        val status = FastSemanticMatcher.match("what's playing")
        assertNotNull(status)
        assertEquals(SkaldMcpToolRegistry.TOOL_GET_PLAYBACK_STATE, status?.toolName)

        val library = FastSemanticMatcher.match("library summary")
        assertNotNull(library)
        assertEquals(SkaldMcpToolRegistry.TOOL_GET_LIBRARY_SUMMARY, library?.toolName)
    }

    @Test
    fun `test unknown phrase returns null`() {
        assertNull(FastSemanticMatcher.match("tell me a joke"))
    }
}
