package dev.vikingsen.skald.feature.assistant.mcp

import dev.vikingsen.skald.core.model.BookWithProgress
import dev.vikingsen.skald.core.model.PlaybackConstants
import dev.vikingsen.skald.core.player.PlayerManager
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.DownloadAudioFileUseCase
import dev.vikingsen.skald.domain.usecase.FetchBookDetailsUseCase
import dev.vikingsen.skald.domain.usecase.GetPlaylistsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SkaldMcpToolRegistry(
    private val playerManager: PlayerManager,
    private val repository: AudiobookshelfRepository,
    private val settingsRepository: SettingsRepository,
    private val fetchBookDetailsUseCase: FetchBookDetailsUseCase,
    private val downloadAudioFileUseCase: DownloadAudioFileUseCase,
    private val getPlaylistsUseCase: GetPlaylistsUseCase
) {

    val tools: List<McpToolDefinition> = listOf(
        McpToolDefinition(
            name = TOOL_RESUME_PLAYBACK,
            description = "Resumes playback of the currently loaded or most recently played audiobook.",
            category = ToolCategory.PLAYBACK
        ),
        McpToolDefinition(
            name = TOOL_PAUSE_PLAYBACK,
            description = "Pauses current audiobook playback.",
            category = ToolCategory.PLAYBACK
        ),
        McpToolDefinition(
            name = TOOL_PLAY_AUDIOBOOK,
            description = "Plays a specific audiobook by ID.",
            category = ToolCategory.PLAYBACK,
            parameters = mapOf(
                "bookId" to McpParameterDefinition("string", "Library item ID of the audiobook", isRequired = true),
                "startFromBeginning" to McpParameterDefinition("boolean", "Start from beginning ignoring saved progress", default = false)
            )
        ),
        McpToolDefinition(
            name = TOOL_SEARCH_AND_PLAY,
            description = "Searches the library by title, author, or narrator, and plays the best matching audiobook.",
            category = ToolCategory.PLAYBACK,
            parameters = mapOf(
                "query" to McpParameterDefinition("string", "Search query (title, author, or narrator)", isRequired = true)
            )
        ),
        McpToolDefinition(
            name = TOOL_SET_SLEEP_TIMER,
            description = "Sets or cancels a sleep timer in minutes or at the end of the chapter.",
            category = ToolCategory.SETTINGS,
            parameters = mapOf(
                "minutes" to McpParameterDefinition("integer", "Duration in minutes before pausing (0 to cancel, 1-480)", default = 0),
                "endOfChapter" to McpParameterDefinition("boolean", "Stop at end of chapter", default = false)
            )
        ),
        McpToolDefinition(
            name = TOOL_SET_PLAYBACK_SPEED,
            description = "Sets the playback speed multiplier between 0.5x and 2.0x.",
            category = ToolCategory.SETTINGS,
            parameters = mapOf(
                "speed" to McpParameterDefinition("number", "Speed multiplier (0.5 to 2.0)", isRequired = true)
            )
        ),
        McpToolDefinition(
            name = TOOL_PLAY_PLAYLIST,
            description = "Plays a playlist by ID or name from the beginning.",
            category = ToolCategory.PLAYBACK,
            parameters = mapOf(
                "playlistId" to McpParameterDefinition("string", "Audiobookshelf playlist ID"),
                "playlistName" to McpParameterDefinition("string", "Playlist name to search for")
            )
        ),
        McpToolDefinition(
            name = TOOL_DOWNLOAD_AUDIOBOOK,
            description = "Enqueues an audiobook for offline download.",
            category = ToolCategory.DOWNLOAD,
            requiresConfirmation = true,
            parameters = mapOf(
                "bookId" to McpParameterDefinition("string", "Library item ID of the audiobook to download", isRequired = true)
            )
        ),
        McpToolDefinition(
            name = TOOL_SEARCH_AND_DOWNLOAD,
            description = "Searches the library and enqueues the matching audiobook for download.",
            category = ToolCategory.DOWNLOAD,
            requiresConfirmation = true,
            parameters = mapOf(
                "query" to McpParameterDefinition("string", "Search query (title, author, or narrator)", isRequired = true)
            )
        ),
        McpToolDefinition(
            name = TOOL_MARK_FINISHED,
            description = "Marks an audiobook as finished or unfinished.",
            category = ToolCategory.SETTINGS,
            parameters = mapOf(
                "bookId" to McpParameterDefinition("string", "Library item ID of the audiobook", isRequired = true),
                "isFinished" to McpParameterDefinition("boolean", "True for finished, false for unfinished", default = true)
            )
        ),
        McpToolDefinition(
            name = TOOL_GET_PLAYBACK_STATE,
            description = "Retrieves the current playback state and currently loaded book details.",
            category = ToolCategory.QUERY
        ),
        McpToolDefinition(
            name = TOOL_GET_LIBRARY_SUMMARY,
            description = "Retrieves a summary of books in progress and total books in the library.",
            category = ToolCategory.QUERY
        )
    )

    suspend fun planExecution(
        toolName: String,
        arguments: Map<String, Any?> = emptyMap()
    ): ToolExecutionPlan = withContext(Dispatchers.IO) {
        when (toolName) {
            TOOL_DOWNLOAD_AUDIOBOOK -> {
                val bookId = arguments["bookId"]?.toString().orEmpty()
                val book = if (bookId.isNotBlank()) fetchBookDetailsUseCase(bookId).getOrNull() else null
                val title = book?.title ?: "Audiobook $bookId"
                ToolExecutionPlan.RequiresConfirmation(
                    toolName = toolName,
                    arguments = arguments,
                    promptTitle = "Download Audiobook",
                    promptDetails = "Do you want to download \"$title\" for offline listening?",
                    targetId = bookId
                )
            }
            TOOL_SEARCH_AND_DOWNLOAD -> {
                val query = arguments["query"]?.toString().orEmpty()
                val candidate = findBestBookMatch(query)
                if (candidate != null) {
                    val fullBook = fetchBookDetailsUseCase(candidate.book.id).getOrNull() ?: candidate.book
                    ToolExecutionPlan.RequiresConfirmation(
                        toolName = toolName,
                        arguments = arguments + ("resolvedBookId" to candidate.book.id),
                        promptTitle = "Download Audiobook",
                        promptDetails = "Do you want to download \"${fullBook.title}\" by ${fullBook.author} for offline listening?",
                        targetId = candidate.book.id
                    )
                } else {
                    ToolExecutionPlan.Immediate(toolName, arguments)
                }
            }
            else -> ToolExecutionPlan.Immediate(toolName, arguments)
        }
    }

    suspend fun executeTool(
        toolName: String,
        arguments: Map<String, Any?> = emptyMap()
    ): McpToolResult = withContext(Dispatchers.IO) {
        try {
            when (toolName) {
                TOOL_RESUME_PLAYBACK -> executeResumePlayback()
                TOOL_PAUSE_PLAYBACK -> executePausePlayback()
                TOOL_PLAY_AUDIOBOOK -> executePlayAudiobook(arguments)
                TOOL_SEARCH_AND_PLAY -> executeSearchAndPlay(arguments)
                TOOL_SET_SLEEP_TIMER -> executeSetSleepTimer(arguments)
                TOOL_SET_PLAYBACK_SPEED -> executeSetPlaybackSpeed(arguments)
                TOOL_PLAY_PLAYLIST -> executePlayPlaylist(arguments)
                TOOL_DOWNLOAD_AUDIOBOOK -> executeDownloadAudiobook(arguments)
                TOOL_SEARCH_AND_DOWNLOAD -> executeSearchAndDownload(arguments)
                TOOL_MARK_FINISHED -> executeMarkFinished(arguments)
                TOOL_GET_PLAYBACK_STATE -> executeGetPlaybackState()
                TOOL_GET_LIBRARY_SUMMARY -> executeGetLibrarySummary()
                else -> McpToolResult.Error("Unknown tool: $toolName")
            }
        } catch (e: Exception) {
            McpToolResult.Error(e.message ?: "Execution failed for $toolName")
        }
    }

    private suspend fun executeResumePlayback(): McpToolResult {
        requireLoggedIn()
        val current = playerManager.currentBook.first()
        if (current != null) {
            withContext(Dispatchers.Main) {
                playerManager.play()
            }
            return McpToolResult.Success("Resumed playback of \"${current.title}\"")
        }

        val libraryId = settingsRepository.getLibraryId()
            ?: return McpToolResult.Error("No library selected. Please select a library in Skald.")

        var allBooks = repository.getBooksWithProgressForLibraryFlow(libraryId).first()
        if (allBooks.isEmpty()) {
            runCatching { repository.syncLibraryBooks(libraryId) }
            allBooks = repository.getBooksWithProgressForLibraryFlow(libraryId).first()
        }

        val mostRecent = allBooks
            .filter { bwp ->
                val p = bwp.progress
                p != null && !p.isFinished && p.currentTime > 0.0
            }
            .maxByOrNull { bwp -> bwp.progress?.lastUpdated ?: 0L }
            ?: allBooks.firstOrNull()

        if (mostRecent != null) {
            val book = fetchBookDetailsUseCase(mostRecent.book.id).getOrNull()
            if (book != null) {
                withContext(Dispatchers.Main) {
                    playerManager.playBook(book, mostRecent.progress?.currentTime ?: 0.0)
                }
                return McpToolResult.Success("Resumed \"${book.title}\"")
            }
        }

        return McpToolResult.Error("No audiobook is currently loaded or in-progress.")
    }

    private suspend fun executePausePlayback(): McpToolResult {
        val current = playerManager.currentBook.first()
            ?: return McpToolResult.Error("No audiobook is currently playing.")
        withContext(Dispatchers.Main) {
            playerManager.pause()
        }
        return McpToolResult.Success("Paused playback of \"${current.title}\"")
    }

    private suspend fun executePlayAudiobook(arguments: Map<String, Any?>): McpToolResult {
        requireLoggedIn()
        val bookId = arguments["bookId"]?.toString().orEmpty()
        if (bookId.isBlank()) return McpToolResult.Error("bookId must not be blank.")

        val startFromBeginning = arguments["startFromBeginning"] as? Boolean ?: false
        val book = fetchBookDetailsUseCase(bookId).getOrElse {
            return McpToolResult.Error("Could not load book details: ${it.message}")
        }

        val startPosition = if (startFromBeginning) {
            0.0
        } else {
            repository.getBookWithProgressFlow(bookId).first().second?.currentTime ?: 0.0
        }

        withContext(Dispatchers.Main) {
            playerManager.playBook(book, startPosition)
        }
        return McpToolResult.Success("Now playing \"${book.title}\"")
    }

    private suspend fun executeSearchAndPlay(arguments: Map<String, Any?>): McpToolResult {
        requireLoggedIn()
        val query = arguments["query"]?.toString().orEmpty()
        if (query.isBlank()) return McpToolResult.Error("Search query must not be blank.")

        val candidate = findBestBookMatch(query)
            ?: return McpToolResult.Error("No audiobook found matching \"$query\".")

        val book = fetchBookDetailsUseCase(candidate.book.id).getOrElse {
            return McpToolResult.Error("Could not load book details: ${it.message}")
        }
        val startPosition = candidate.progress?.currentTime ?: 0.0

        withContext(Dispatchers.Main) {
            playerManager.playBook(book, startPosition)
        }
        return McpToolResult.Success("Found and playing \"${book.title}\" by ${book.author}")
    }

    private suspend fun executeSetSleepTimer(arguments: Map<String, Any?>): McpToolResult {
        if (playerManager.currentBook.first() == null) {
            return McpToolResult.Error("No audiobook is currently loaded.")
        }
        val endOfChapter = arguments["endOfChapter"] as? Boolean ?: false
        val minutes = (arguments["minutes"] as? Number)?.toInt() ?: 0

        withContext(Dispatchers.Main) {
            when {
                endOfChapter -> playerManager.setSleepTimerEndOfChapter()
                minutes == 0 -> playerManager.cancelSleepTimer()
                minutes < 0 || minutes > 480 -> throw IllegalArgumentException("minutes must be between 1 and 480.")
                else -> playerManager.setSleepTimer(minutes)
            }
        }

        val message = when {
            endOfChapter -> "Sleep timer set for the end of the chapter."
            minutes == 0 -> "Sleep timer canceled."
            else -> "Sleep timer set for $minutes minutes."
        }
        return McpToolResult.Success(message)
    }

    private suspend fun executeSetPlaybackSpeed(arguments: Map<String, Any?>): McpToolResult {
        val speed = (arguments["speed"] as? Number)?.toFloat()
            ?: return McpToolResult.Error("Speed multiplier is required.")

        if (speed !in PlaybackConstants.SPEED_RANGE) {
            return McpToolResult.Error("Speed must be between ${PlaybackConstants.SPEED_RANGE.start} and ${PlaybackConstants.SPEED_RANGE.endInclusive}x.")
        }
        val snapped = PlaybackConstants.PLAYBACK_SPEEDS.minByOrNull { kotlin.math.abs(it - speed) } ?: speed
        withContext(Dispatchers.Main) {
            playerManager.setPlaybackSpeed(snapped)
        }
        return McpToolResult.Success("Playback speed set to ${snapped}x")
    }

    private suspend fun executePlayPlaylist(arguments: Map<String, Any?>): McpToolResult {
        requireLoggedIn()
        val playlistId = arguments["playlistId"]?.toString()
        val playlistName = arguments["playlistName"]?.toString()

        if (playlistId.isNullOrBlank() && playlistName.isNullOrBlank()) {
            return McpToolResult.Error("Provide playlistId or playlistName.")
        }

        val playlists = getPlaylistsUseCase().first()
        val playlist = when {
            !playlistId.isNullOrBlank() -> playlists.find { it.id == playlistId }
            else -> playlists.find { it.name.contains(playlistName ?: "", ignoreCase = true) }
        } ?: return McpToolResult.Error("Playlist not found.")

        if (playlist.items.isEmpty()) {
            return McpToolResult.Error("Playlist \"${playlist.name}\" is empty.")
        }

        withContext(Dispatchers.Main) {
            playerManager.playPlaylist(playlist, startIndex = 0, startPosition = 0.0)
        }
        return McpToolResult.Success("Playing playlist \"${playlist.name}\"")
    }

    private suspend fun executeDownloadAudiobook(arguments: Map<String, Any?>): McpToolResult {
        requireLoggedIn()
        val bookId = arguments["bookId"]?.toString().orEmpty()
        if (bookId.isBlank()) return McpToolResult.Error("bookId must not be blank.")

        val book = fetchBookDetailsUseCase(bookId).getOrElse {
            return McpToolResult.Error("Could not load book: ${it.message}")
        }
        if (book.isDownloaded) {
            return McpToolResult.Success("\"${book.title}\" is already downloaded.")
        }

        downloadAudioFileUseCase(bookId).getOrElse {
            return McpToolResult.Error("Failed to start download: ${it.message}")
        }
        return McpToolResult.Success("Download started for \"${book.title}\".")
    }

    private suspend fun executeSearchAndDownload(arguments: Map<String, Any?>): McpToolResult {
        requireLoggedIn()
        val resolvedId = arguments["resolvedBookId"]?.toString()
        val targetId = if (!resolvedId.isNullOrBlank()) {
            resolvedId
        } else {
            val query = arguments["query"]?.toString().orEmpty()
            val candidate = findBestBookMatch(query)
                ?: return McpToolResult.Error("No audiobook found matching \"$query\".")
            candidate.book.id
        }

        return executeDownloadAudiobook(mapOf("bookId" to targetId))
    }

    private suspend fun executeMarkFinished(arguments: Map<String, Any?>): McpToolResult {
        requireLoggedIn()
        val bookId = arguments["bookId"]?.toString().orEmpty()
        if (bookId.isBlank()) return McpToolResult.Error("bookId is required.")
        val isFinished = arguments["isFinished"] as? Boolean ?: true

        repository.updatePlaybackFinished(bookId, isFinished).getOrElse {
            return McpToolResult.Error("Failed to update status: ${it.message}")
        }
        val statusText = if (isFinished) "finished" else "unfinished"
        return McpToolResult.Success("Marked book as $statusText.")
    }

    private suspend fun executeGetPlaybackState(): McpToolResult {
        val current = playerManager.currentBook.first()
            ?: return McpToolResult.Success("No audiobook currently loaded.")
        val isPlaying = playerManager.isPlaying.first()
        val progress = playerManager.currentPosition.first()
        val speed = playerManager.playbackSpeed.first()

        val state = buildString {
            append("Currently ")
            append(if (isPlaying) "playing" else "paused")
            append(": \"${current.title}\" by ${current.author}. ")
            append("Speed: ${speed}x. ")
        }
        return McpToolResult.Success(state, payload = current)
    }

    private suspend fun executeGetLibrarySummary(): McpToolResult {
        requireLoggedIn()
        val libraryId = settingsRepository.getLibraryId()
            ?: return McpToolResult.Error("No library selected.")

        var allBooks = repository.getBooksWithProgressForLibraryFlow(libraryId).first()
        if (allBooks.isEmpty()) {
            runCatching { repository.syncLibraryBooks(libraryId) }
            allBooks = repository.getBooksWithProgressForLibraryFlow(libraryId).first()
        }

        val inProgress = allBooks.count { it.progress != null && !it.progress!!.isFinished && it.progress!!.currentTime > 0 }
        val downloaded = allBooks.count { it.book.isDownloaded }

        val summary = "You have ${allBooks.size} books in your library. $inProgress currently in progress, and $downloaded downloaded."
        return McpToolResult.Success(summary, payload = allBooks)
    }

    private suspend fun findBestBookMatch(query: String): BookWithProgress? {
        val libraryId = settingsRepository.getLibraryId() ?: return null
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return null

        var allBooks = repository.getBooksWithProgressForLibraryFlow(libraryId).first()
        if (allBooks.isEmpty()) {
            runCatching { repository.syncLibraryBooks(libraryId) }
            allBooks = repository.getBooksWithProgressForLibraryFlow(libraryId).first()
        }

        var matching = filterBooksByQuery(allBooks, cleanQuery)
        if (matching.isEmpty()) {
            runCatching { repository.syncLibraryBooks(libraryId) }
            allBooks = repository.getBooksWithProgressForLibraryFlow(libraryId).first()
            matching = filterBooksByQuery(allBooks, cleanQuery)
        }

        if (matching.isEmpty()) return null

        val readingMatches = matching.filter { bwp ->
            val p = bwp.progress
            p != null && !p.isFinished && p.currentTime > 0.0
        }
        val candidates = readingMatches.ifEmpty { matching }

        return candidates.sortedWith(
            compareBy<BookWithProgress> { bwp ->
                val title = bwp.book.title
                when {
                    title.equals(cleanQuery, ignoreCase = true) -> 0
                    title.startsWith(cleanQuery, ignoreCase = true) -> 1
                    title.contains(cleanQuery, ignoreCase = true) -> 2
                    bwp.book.author.contains(cleanQuery, ignoreCase = true) -> 3
                    else -> 4
                }
            }.thenByDescending { it.progress?.lastUpdated ?: 0L }
        ).firstOrNull()
    }

    private fun filterBooksByQuery(books: List<BookWithProgress>, query: String): List<BookWithProgress> {
        val directMatches = books.filter { bwp ->
            val b = bwp.book
            b.title.contains(query, ignoreCase = true) ||
                b.author.contains(query, ignoreCase = true) ||
                b.narrator.contains(query, ignoreCase = true)
        }
        if (directMatches.isNotEmpty()) return directMatches

        val stopWords = setOf("the", "a", "an", "and", "audiobook", "book", "of", "to", "in")
        val tokens = query.lowercase().split(Regex("""\s+""")).filter { it.isNotBlank() && it !in stopWords }
        if (tokens.isEmpty()) return emptyList()

        return books.filter { bwp ->
            val b = bwp.book
            val haystack = "${b.title} ${b.author} ${b.narrator}".lowercase()
            tokens.all { haystack.contains(it) }
        }
    }

    private fun requireLoggedIn() {
        if (!settingsRepository.isLoggedIn()) {
            throw IllegalStateException("Not logged in to Skald. Please open the app and connect to your Audiobookshelf server.")
        }
    }

    companion object {
        const val TOOL_RESUME_PLAYBACK = "resumeCurrentPlayback"
        const val TOOL_PAUSE_PLAYBACK = "pauseCurrentPlayback"
        const val TOOL_PLAY_AUDIOBOOK = "playAudiobook"
        const val TOOL_SEARCH_AND_PLAY = "searchAndPlayAudiobook"
        const val TOOL_SET_SLEEP_TIMER = "setSleepTimer"
        const val TOOL_SET_PLAYBACK_SPEED = "setPlaybackSpeed"
        const val TOOL_PLAY_PLAYLIST = "playPlaylist"
        const val TOOL_DOWNLOAD_AUDIOBOOK = "downloadAudiobook"
        const val TOOL_SEARCH_AND_DOWNLOAD = "searchAndDownloadAudiobook"
        const val TOOL_MARK_FINISHED = "markBookFinished"
        const val TOOL_GET_PLAYBACK_STATE = "getCurrentPlaybackState"
        const val TOOL_GET_LIBRARY_SUMMARY = "getLibrarySummary"
    }
}
