package dev.vikingsen.skald.feature.assistant.engine

import dev.vikingsen.skald.feature.assistant.mcp.SkaldMcpToolRegistry

data class MatchedIntent(
    val toolName: String,
    val arguments: Map<String, Any?> = emptyMap()
)

object FastSemanticMatcher {

    private val resumePatterns = listOf(
        Regex("""^(resume|continue|unpause)(\s+playback|\s+playing|\s+audiobook|\s+(?:the|my)\s+book)?$""", RegexOption.IGNORE_CASE),
        Regex("""^(play)$""", RegexOption.IGNORE_CASE)
    )

    private val pausePatterns = listOf(
        Regex("""^(pause|stop)(\s+playback|\s+playing|\s+audiobook|\s+(?:the|my)\s+book)?$""", RegexOption.IGNORE_CASE)
    )

    private val sleepTimerMinutesPattern = Regex(
        """^(?:set\s+)?(?:a\s+)?sleep\s+timer\s+(?:for\s+|in\s+)?(\d+)\s*(?:m|min|mins|minutes)?$""",
        RegexOption.IGNORE_CASE
    )

    private val sleepTimerCancelPattern = Regex(
        """^(cancel|stop|turn\s+off|disable)\s+(?:the\s+)?sleep\s+timer$""",
        RegexOption.IGNORE_CASE
    )

    private val sleepTimerEndOfChapterPattern = Regex(
        """^(?:set\s+)?(?:sleep\s+timer\s+)?(?:at\s+|for\s+)?end\s+of\s+chapter$""",
        RegexOption.IGNORE_CASE
    )

    private val speedPattern = Regex(
        """^(?:set\s+)?(?:playback\s+)?speed\s+(?:to\s+)?([0-2](?:\.\d+)?)\s*x?$""",
        RegexOption.IGNORE_CASE
    )

    private val downloadPattern = Regex(
        """^(?:(?:search|find)\s+and\s+download|download)\s+(?:the\s+audiobook\s+|audiobook\s+|the\s+book\s+|book\s+)?(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val playSearchPattern = Regex(
        """^(?:(?:search|find)\s+and\s+play|play|start|listen\s+to|search\s+for|find)\s+(?:the\s+audiobook\s+|audiobook\s+|the\s+book\s+|book\s+)?(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val playbackStatePattern = Regex(
        """^(what('?s|\s+is)\s+(playing|this|the\s+book|my\s+progress)|current\s+book|status)$""",
        RegexOption.IGNORE_CASE
    )

    private val librarySummaryPattern = Regex(
        """^(library\s+summary|how\s+many\s+books|what('?s|\s+is)\s+in\s+my\s+library|books\s+in\s+progress)$""",
        RegexOption.IGNORE_CASE
    )

    fun match(input: String): MatchedIntent? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        // 1. Resume
        if (resumePatterns.any { it.matches(trimmed) }) {
            return MatchedIntent(SkaldMcpToolRegistry.TOOL_RESUME_PLAYBACK)
        }

        // 2. Pause
        if (pausePatterns.any { it.matches(trimmed) }) {
            return MatchedIntent(SkaldMcpToolRegistry.TOOL_PAUSE_PLAYBACK)
        }

        // 3. Sleep Timer Cancel
        if (sleepTimerCancelPattern.matches(trimmed)) {
            return MatchedIntent(
                SkaldMcpToolRegistry.TOOL_SET_SLEEP_TIMER,
                mapOf("minutes" to 0)
            )
        }

        // 4. Sleep Timer End of Chapter
        if (sleepTimerEndOfChapterPattern.matches(trimmed)) {
            return MatchedIntent(
                SkaldMcpToolRegistry.TOOL_SET_SLEEP_TIMER,
                mapOf("endOfChapter" to true)
            )
        }

        // 5. Sleep Timer Minutes
        val sleepMatch = sleepTimerMinutesPattern.find(trimmed)
        if (sleepMatch != null) {
            val mins = sleepMatch.groupValues[1].toIntOrNull() ?: 0
            return MatchedIntent(
                SkaldMcpToolRegistry.TOOL_SET_SLEEP_TIMER,
                mapOf("minutes" to mins)
            )
        }

        // 6. Speed
        val speedMatch = speedPattern.find(trimmed)
        if (speedMatch != null) {
            val speed = speedMatch.groupValues[1].toFloatOrNull() ?: 1.0f
            return MatchedIntent(
                SkaldMcpToolRegistry.TOOL_SET_PLAYBACK_SPEED,
                mapOf("speed" to speed)
            )
        }

        // 7. Download
        val downloadMatch = downloadPattern.find(trimmed)
        if (downloadMatch != null) {
            val query = downloadMatch.groupValues[1].trim()
            if (query.isNotBlank()) {
                return MatchedIntent(
                    SkaldMcpToolRegistry.TOOL_SEARCH_AND_DOWNLOAD,
                    mapOf("query" to query)
                )
            }
        }

        // 8. Play Search
        val playMatch = playSearchPattern.find(trimmed)
        if (playMatch != null) {
            val query = playMatch.groupValues[1].trim()
            if (query.isNotBlank()) {
                return MatchedIntent(
                    SkaldMcpToolRegistry.TOOL_SEARCH_AND_PLAY,
                    mapOf("query" to query)
                )
            }
        }

        // 9. Playback state
        if (playbackStatePattern.matches(trimmed)) {
            return MatchedIntent(SkaldMcpToolRegistry.TOOL_GET_PLAYBACK_STATE)
        }

        // 10. Library summary
        if (librarySummaryPattern.matches(trimmed)) {
            return MatchedIntent(SkaldMcpToolRegistry.TOOL_GET_LIBRARY_SUMMARY)
        }

        return null
    }
}
