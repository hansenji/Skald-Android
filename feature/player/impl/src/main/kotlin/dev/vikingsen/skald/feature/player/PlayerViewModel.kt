package dev.vikingsen.skald.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.skald.core.model.Book
import dev.vikingsen.skald.core.model.Chapter
import dev.vikingsen.skald.core.model.formatDuration
import dev.vikingsen.skald.core.model.formatPosition
import dev.vikingsen.skald.core.player.PlayerManager
import dev.vikingsen.skald.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*

data class ChapterUiModel(
    val title: String,
    val start: Double,
    val end: Double,
    val startText: String,
    val durationText: String
)

data class PlayerUiState(
    val title: String,
    val author: String,
    val coverUrl: String,
    val authorizationHeader: String?,
    val isPlaying: Boolean,
    val currentPosition: Double,
    val currentPositionText: String,
    val duration: Double,
    val durationText: String,
    val timeRemainingText: String,
    val currentChapterTitle: String?,
    val playbackSpeed: Float,
    val sleepTimerRemaining: Long,
    val sleepTimerText: String,
    val chapters: List<ChapterUiModel>,
    val skipForwardDuration: Int,
    val skipBackwardDuration: Int,
    val isLoading: Boolean
)

class PlayerViewModel(
    private val playerManager: PlayerManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val settingsRefreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<PlayerUiState?> = combine(
        listOf(
            playerManager.currentBook,
            playerManager.isPlaying,
            playerManager.currentPosition,
            playerManager.duration,
            playerManager.currentChapter,
            playerManager.playbackSpeed,
            playerManager.sleepTimerRemaining,
            settingsRefreshTrigger,
            playerManager.isLoading
        )
    ) { array ->
        val book = array[0] as Book?
        val isPlaying = array[1] as Boolean
        val position = array[2] as Double
        val duration = array[3] as Double
        val chapter = array[4] as Chapter?
        val speed = array[5] as Float
        val timer = array[6] as Long
        val isLoading = array[8] as Boolean

        if (book == null) null
        else {
            val serverUrl = settingsRepository.getServerUrl() ?: ""
            val token = settingsRepository.getToken() ?: ""
            val coverUrl = if (!book.coverPath.isNullOrEmpty()) book.coverPath!!
                           else "${serverUrl.trimEnd('/')}/api/items/${book.id}/cover"
            val authHeader = if (!book.coverPath.isNullOrEmpty()) null
                             else "Bearer $token"
                             
            val chapters = book.chapters.mapIndexed { index, ch ->
                ChapterUiModel(
                    title = ch.title.ifEmpty { "Chapter ${index + 1}" },
                    start = ch.start,
                    end = ch.end,
                    startText = formatPosition(ch.start),
                    durationText = formatDuration(ch.end - ch.start)
                )
            }
            
            val timerText = if (timer > 0) {
                val rem = timer / 1000
                "${rem / 60}:${(rem % 60).toString().padStart(2, '0')}"
            } else "Timer"

            val forwardDur = settingsRepository.getSkipForwardDuration()
            val backwardDur = settingsRepository.getSkipBackwardDuration()

            PlayerUiState(
                title = book.title,
                author = book.author,
                coverUrl = coverUrl,
                authorizationHeader = authHeader,
                isPlaying = isPlaying,
                currentPosition = position,
                currentPositionText = formatPosition(position),
                duration = duration,
                durationText = formatPosition(duration),
                timeRemainingText = "-" + formatPosition((duration - position).coerceAtLeast(0.0)),
                currentChapterTitle = chapter?.title,
                playbackSpeed = speed,
                sleepTimerRemaining = timer,
                sleepTimerText = timerText,
                chapters = chapters,
                skipForwardDuration = forwardDur,
                skipBackwardDuration = backwardDur,
                isLoading = isLoading
            )
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    fun play() {
        playerManager.play()
    }

    fun pause() {
        playerManager.pause()
    }

    fun seekTo(position: Double) {
        playerManager.seekTo(position)
    }

    fun skipForward() {
        val duration = settingsRepository.getSkipForwardDuration().toDouble()
        val target = playerManager.currentPosition.value + duration
        val total = playerManager.duration.value
        if (target >= total) {
            playerManager.seekTo(total)
            playerManager.pause()
        } else {
            playerManager.seekTo(target)
        }
    }

    fun skipBackward() {
        val duration = settingsRepository.getSkipBackwardDuration().toDouble()
        val target = (playerManager.currentPosition.value - duration).coerceAtLeast(0.0)
        playerManager.seekTo(target)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        val currentSpeed = playerManager.playbackSpeed.value
        val currentIndex = speeds.indexOfFirst { kotlin.math.abs(it - currentSpeed) < 0.01f }
        val nextSpeed = if (currentIndex != -1 && currentIndex < speeds.lastIndex) {
            speeds[currentIndex + 1]
        } else {
            speeds[0]
        }
        playerManager.setPlaybackSpeed(nextSpeed)
    }

    fun setSleepTimer(minutes: Int) {
        playerManager.setSleepTimer(minutes)
    }

    fun setSleepTimerEndOfChapter() {
        playerManager.setSleepTimerEndOfChapter()
    }

    fun cancelSleepTimer() {
        playerManager.cancelSleepTimer()
    }

    fun setSkipForwardDuration(duration: Int) {
        settingsRepository.saveSkipForwardDuration(duration)
        settingsRefreshTrigger.value += 1
    }

    fun setSkipBackwardDuration(duration: Int) {
        settingsRepository.saveSkipBackwardDuration(duration)
        settingsRefreshTrigger.value += 1
    }

    fun skipToNextChapter() {
        val current = playerManager.currentPosition.value
        val book = playerManager.currentBook.value ?: return
        val nextChapter = book.chapters.find { it.start > current }
        if (nextChapter != null) {
            playerManager.seekTo(nextChapter.start)
        } else {
            playerManager.seekTo(playerManager.duration.value)
        }
    }

    fun skipToPreviousChapter() {
        val current = playerManager.currentPosition.value
        val book = playerManager.currentBook.value ?: return
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
}
