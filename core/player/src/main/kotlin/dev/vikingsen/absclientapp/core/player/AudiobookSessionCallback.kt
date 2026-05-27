package dev.vikingsen.absclientapp.core.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository
import dev.vikingsen.absclientapp.domain.usecase.GetBooksUseCase
import dev.vikingsen.absclientapp.domain.usecase.GetPlaybackProgressUseCase
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

    private fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val networks = cm.allNetworks
            if (networks.isEmpty()) return false
            networks.any { net ->
                val cap = cm.getNetworkCapabilities(net)
                cap != null && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        } catch (e: Exception) {
            true // Default to online if system check fails or permission is missing
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main)

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
        // Expose custom commands to connecting controllers (like Android Auto / Bluetooth / Wearable)
        val connectionResult = super.onConnect(session, controller)
        val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
        for (cmd in ALL_CUSTOM_COMMANDS) {
            availableSessionCommands.add(SessionCommand(cmd, Bundle.EMPTY))
        }
        return MediaSession.ConnectionResult.accept(
            availableSessionCommands.build(),
            connectionResult.availablePlayerCommands
        )
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
                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.25f, 2.5f, 2.75f, 3.0f)
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
                val isOffline = !isOnline()
                val playableBooks = if (isOffline) books.filter { it.isDownloaded } else books

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
                    // Direct playback by ID
                    val cleanId = firstItem.mediaId.substringBefore("_")
                    val book = books.find { it.id == cleanId }
                    if (book != null) {
                        if (isOffline && !book.isDownloaded) {
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
            } catch (e: Exception) {
                future.setException(e)
            }
        }

        return future
    }
}
