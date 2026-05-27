package dev.vikingsen.absclientapp.feature.player

import androidx.lifecycle.ViewModel
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.model.Chapter
import dev.vikingsen.absclientapp.core.player.PlayerManager
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(
    private val playerManager: PlayerManager
) : ViewModel() {
    val currentBook: StateFlow<Book?> = playerManager.currentBook
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPosition: StateFlow<Double> = playerManager.currentPosition
    val duration: StateFlow<Double> = playerManager.duration
    val currentChapter: StateFlow<Chapter?> = playerManager.currentChapter
    val playbackSpeed: StateFlow<Float> = playerManager.playbackSpeed
    val sleepTimerRemaining: StateFlow<Long> = playerManager.sleepTimerRemaining

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
