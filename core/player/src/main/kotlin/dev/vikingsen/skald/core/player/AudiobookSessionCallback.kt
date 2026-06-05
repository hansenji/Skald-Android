package dev.vikingsen.skald.core.player

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dev.vikingsen.skald.core.model.Book
import dev.vikingsen.skald.core.model.PlaybackConstants
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.GetBooksUseCase
import dev.vikingsen.skald.domain.usecase.GetPlaybackProgressUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class AudiobookSessionCallback(
    private val context: Context,
    private val playerManager: PlayerManager,
    private val settingsRepository: SettingsRepository,
    private val getBooksUseCase: GetBooksUseCase,
    private val getPlaybackProgressUseCase: GetPlaybackProgressUseCase
) : MediaLibrarySession.Callback {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var activeSession: MediaSession? = null

    init {
        scope.launch {
            playerManager.currentBook.collect { book ->
                val session = activeSession ?: return@collect
                val layout = buildCustomLayout()
                session.setCustomLayout(layout)
                session.setMediaButtonPreferences(layout)
                for (controller in session.connectedControllers) {
                    session.setCustomLayout(controller, layout)
                }
            }
        }
        scope.launch {
            playerManager.playbackSpeed.collect { speed ->
                val session = activeSession ?: return@collect
                val layout = buildCustomLayout()
                session.setCustomLayout(layout)
                session.setMediaButtonPreferences(layout)
                for (controller in session.connectedControllers) {
                    session.setCustomLayout(controller, layout)
                }
            }
        }
    }

    companion object {
        const val COMMAND_SKIP_FORWARD = "COMMAND_SKIP_FORWARD"
        const val COMMAND_SKIP_BACKWARD = "COMMAND_SKIP_BACKWARD"
        const val COMMAND_CYCLE_SPEED = "COMMAND_CYCLE_SPEED"
        const val COMMAND_SKIP_TO_NEXT_CHAPTER = "COMMAND_SKIP_TO_NEXT_CHAPTER"
        const val COMMAND_SKIP_TO_PREVIOUS_CHAPTER = "COMMAND_SKIP_TO_PREVIOUS_CHAPTER"

        val ALL_CUSTOM_COMMANDS = listOf(
            COMMAND_SKIP_FORWARD,
            COMMAND_SKIP_BACKWARD,
            COMMAND_CYCLE_SPEED,
            COMMAND_SKIP_TO_NEXT_CHAPTER,
            COMMAND_SKIP_TO_PREVIOUS_CHAPTER
        )
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        activeSession = session
        // Expose custom commands to connecting controllers (like Android Auto / Bluetooth / Wearable)
        val connectionResult = super.onConnect(session, controller)
        val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
        for (cmd in ALL_CUSTOM_COMMANDS) {
            availableSessionCommands.add(SessionCommand(cmd, Bundle()))
        }
        return MediaSession.ConnectionResult.accept(
            availableSessionCommands.build(),
            connectionResult.availablePlayerCommands
        )
    }

    override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
        activeSession = session
        val layout = buildCustomLayout()
        session.setCustomLayout(controller, layout)
        session.setMediaButtonPreferences(layout)
    }

    private fun getSpeedIconResId(speed: Float): Int {
        return when {
            speed < 0.625f -> R.drawable.ic_speed_0_50
            speed < 0.875f -> R.drawable.ic_speed_0_75
            speed < 1.125f -> R.drawable.ic_speed_1_00
            speed < 1.375f -> R.drawable.ic_speed_1_25
            speed < 1.625f -> R.drawable.ic_speed_1_50
            speed < 1.875f -> R.drawable.ic_speed_1_75
            speed < 2.125f -> R.drawable.ic_speed_2_00
            speed < 2.375f -> R.drawable.ic_speed_2_25
            speed < 2.625f -> R.drawable.ic_speed_2_50
            speed < 2.875f -> R.drawable.ic_speed_2_75
            else -> R.drawable.ic_speed_3_00
        }
    }

    private fun buildCustomLayout(): List<CommandButton> {
        val book = playerManager.currentBook.value
        val hasChapters = book?.chapters?.isNotEmpty() == true

        val layout = mutableListOf<CommandButton>()

        // 1. Skip Backward (Replay) -> maps to Slot 2
        layout.add(
            CommandButton.Builder()
                .setSessionCommand(SessionCommand(COMMAND_SKIP_BACKWARD, Bundle()))
                .setDisplayName(context.getString(R.string.control_skip_backward))
                .setIconResId(R.drawable.ic_replay)
                .setEnabled(true)
                .build()
        )

        // 2. Skip Forward (Forward Media) -> maps to Slot 4
        layout.add(
            CommandButton.Builder()
                .setSessionCommand(SessionCommand(COMMAND_SKIP_FORWARD, Bundle()))
                .setDisplayName(context.getString(R.string.control_skip_forward))
                .setIconResId(R.drawable.ic_forward_media)
                .setEnabled(true)
                .build()
        )

        // 3. Playback Speed -> maps to Slot 1
        val speedIcon = getSpeedIconResId(playerManager.playbackSpeed.value)
        layout.add(
            CommandButton.Builder()
                .setSessionCommand(SessionCommand(COMMAND_CYCLE_SPEED, Bundle()))
                .setDisplayName(context.getString(R.string.control_playback_speed))
                .setIconResId(speedIcon)
                .setEnabled(true)
                .build()
        )

        // 4. Previous & Next Chapter (only if book has chapters, positioned behind overflow) -> maps to Slot 5 / Overflow
        if (hasChapters) {
            val prevChapterExtras = Bundle().apply {
                putBoolean("com.google.android.gms.car.media.ALWAYS_IN_OVERFLOW", true)
            }
            layout.add(
                CommandButton.Builder()
                    .setSessionCommand(SessionCommand(COMMAND_SKIP_TO_PREVIOUS_CHAPTER, Bundle()))
                    .setDisplayName(context.getString(R.string.control_prev_chapter))
                    .setIconResId(R.drawable.ic_skip_previous)
                    .setExtras(prevChapterExtras)
                    .setEnabled(true)
                    .build()
            )
            val nextChapterExtras = Bundle().apply {
                putBoolean("com.google.android.gms.car.media.ALWAYS_IN_OVERFLOW", true)
            }
            layout.add(
                CommandButton.Builder()
                    .setSessionCommand(SessionCommand(COMMAND_SKIP_TO_NEXT_CHAPTER, Bundle()))
                    .setDisplayName(context.getString(R.string.control_next_chapter))
                    .setIconResId(R.drawable.ic_skip_next)
                    .setExtras(nextChapterExtras)
                    .setEnabled(true)
                    .build()
            )
        }

        return layout
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            COMMAND_SKIP_FORWARD -> {
                val duration = settingsRepository.getSkipForwardDuration()
                val current = playerManager.currentPosition.value
                val total = playerManager.duration.value
                val target = current + duration
                if (target >= total) {
                    playerManager.seekTo(total)
                    playerManager.pause()
                } else {
                    playerManager.seekTo(target)
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            COMMAND_SKIP_BACKWARD -> {
                val duration = settingsRepository.getSkipBackwardDuration()
                val current = playerManager.currentPosition.value
                val target = (current - duration).coerceAtLeast(0.0)
                playerManager.seekTo(target)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            COMMAND_CYCLE_SPEED -> {
                val speeds = PlaybackConstants.PLAYBACK_SPEEDS
                val currentSpeed = playerManager.playbackSpeed.value
                // Find next speed in the cycle. Default to 1.0f if not found.
                val currentIndex = speeds.indexOfFirst { Math.abs(it - currentSpeed) < 0.01f }
                val nextSpeed = if (currentIndex != -1 && currentIndex < speeds.lastIndex) {
                    speeds[currentIndex + 1]
                } else {
                    speeds[0]
                }
                playerManager.setPlaybackSpeed(nextSpeed)
                settingsRepository.savePlaybackSpeed(nextSpeed)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            COMMAND_SKIP_TO_NEXT_CHAPTER -> {
                val current = playerManager.currentPosition.value
                val book = playerManager.currentBook.value
                if (book != null) {
                    val nextChapter = book.chapters.find { it.start > current }
                    if (nextChapter != null) {
                        playerManager.seekTo(nextChapter.start)
                    } else {
                        // On final chapter, seek to end
                        playerManager.seekTo(playerManager.duration.value)
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            COMMAND_SKIP_TO_PREVIOUS_CHAPTER -> {
                val current = playerManager.currentPosition.value
                val book = playerManager.currentBook.value
                if (book != null) {
                    val activeChapter = book.chapters.find { current >= it.start && current < it.end }
                    if (activeChapter != null) {
                        val elapsedInChapter = current - activeChapter.start
                        if (elapsedInChapter > 5.0) {
                            playerManager.seekTo(activeChapter.start)
                        } else {
                            val prevChapter = book.chapters.filter { it.start < activeChapter.start }
                                .maxByOrNull { it.start }
                            if (prevChapter != null) {
                                playerManager.seekTo(prevChapter.start)
                            } else {
                                playerManager.seekTo(0.0)
                            }
                        }
                    } else {
                        playerManager.seekTo(0.0)
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }
        return super.onCustomCommand(session, controller, customCommand, args)
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        val firstItem = mediaItems.firstOrNull() ?: return Futures.immediateFuture(emptyList())
        val future = SettableFuture.create<List<MediaItem>>()

        scope.launch {
            try {
                val books = getBooksUseCase().first()
                val progressList = getPlaybackProgressUseCase().first()
                val playableBooks = books.filter { it.isDownloaded }

                // Check search query (Voice command)
                val searchQuery = firstItem.requestMetadata.searchQuery ?: firstItem.mediaMetadata.title?.toString()
                if (!searchQuery.isNullOrEmpty() && (firstItem.mediaId == "play_from_search" || firstItem.mediaId.isEmpty())) {
                    val isGeneric = searchQuery.isBlank() || searchQuery.lowercase().trim() in listOf(
                        "audiobook", "book", "continue reading", "resume",
                        "continue reading my book", "read my audiobook",
                        "pickup where i last left off"
                    )

                    val resolvedBook: Book? = if (isGeneric) {
                        // Find most recently played audiobook in progress
                        val activeProgress = progressList
                            .filter { it.progress > 0f && it.progress < 0.99f && !it.isFinished }
                            .maxByOrNull { it.lastUpdated }
                        
                        val book = activeProgress?.let { prog -> playableBooks.find { it.id == prog.bookId } }
                        book ?: playableBooks.firstOrNull()
                    } else {
                        // Search by query term (title or author)
                        val matches = playableBooks.filter {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.author.contains(searchQuery, ignoreCase = true)
                        }
                        
                        // Pick the best match (exact title match first, or highest progress lastUpdated)
                        matches.find { it.title.equals(searchQuery, ignoreCase = true) }
                            ?: matches.maxByOrNull { book ->
                                progressList.find { it.bookId == book.id }?.lastUpdated ?: 0L
                            }
                    }

                    if (resolvedBook != null) {
                        val progress = progressList.find { it.bookId == resolvedBook.id }
                        val startPos = progress?.currentTime ?: 0.0
                        playerManager.playBook(resolvedBook, startPos)
                        future.set(playerManager.createMediaItemsForBook(resolvedBook))
                    } else {
                        future.set(emptyList())
                    }
                } else {
                    val cleanId = firstItem.mediaId.substringBefore("_")
                    val book = books.find { it.id == cleanId }
                    if (book != null) {
                        if (!book.isDownloaded) {
                            future.set(emptyList())
                            return@launch
                        }
                        val progress = progressList.find { it.bookId == book.id }
                        val startPos = progress?.currentTime ?: 0.0
                        playerManager.playBook(book, startPos)
                        future.set(playerManager.createMediaItemsForBook(book))
                    } else {
                        future.set(mediaItems)
                    }
                }
            } catch (e: Throwable) {
                println("DEBUG: Exception in onAddMediaItems: $e")
                e.printStackTrace()
                future.setException(e)
            }
        }

        return future
    }
}
