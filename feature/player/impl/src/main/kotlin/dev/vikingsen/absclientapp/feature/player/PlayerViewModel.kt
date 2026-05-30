package dev.vikingsen.absclientapp.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.model.Chapter
import dev.vikingsen.absclientapp.core.model.formatDuration
import dev.vikingsen.absclientapp.core.model.formatPosition
import dev.vikingsen.absclientapp.core.player.PlayerManager
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository
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
    val chapters: List<ChapterUiModel>
)

class PlayerViewModel(
    private val playerManager: PlayerManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<PlayerUiState?> = combine(
        listOf(
            playerManager.currentBook,
            playerManager.isPlaying,
            playerManager.currentPosition,
            playerManager.duration,
            playerManager.currentChapter,
            playerManager.playbackSpeed,
            playerManager.sleepTimerRemaining
        )
    ) { array ->
        val book = array[0] as Book?
        val isPlaying = array[1] as Boolean
        val position = array[2] as Double
        val duration = array[3] as Double
        val chapter = array[4] as Chapter?
        val speed = array[5] as Float
        val timer = array[6] as Long

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
                chapters = chapters
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
        playerManager.seekTo(playerManager.currentPosition.value + 30.0)
    }

    fun skipBackward() {
        playerManager.seekTo(playerManager.currentPosition.value - 10.0)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int) {
        playerManager.setSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        playerManager.cancelSleepTimer()
    }
}
