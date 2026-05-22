package com.example.absclientapp.ui.player

import androidx.lifecycle.ViewModel
import com.example.absclientapp.data.database.BookEntity
import com.example.absclientapp.data.database.LocalChapter
import com.example.absclientapp.data.repository.AudiobookshelfRepository
import com.example.absclientapp.player.PlayerManager
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(
    private val playerManager: PlayerManager,
    private val repository: AudiobookshelfRepository
) : ViewModel() {
    val currentBook: StateFlow<BookEntity?> = playerManager.currentBook
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPosition: StateFlow<Double> = playerManager.currentPosition
    val duration: StateFlow<Double> = playerManager.duration
    val currentChapter: StateFlow<LocalChapter?> = playerManager.currentChapter
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
