package dev.vikingsen.absclientapp.core.player

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.model.AudioFile
import dev.vikingsen.absclientapp.core.model.Chapter
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository
import dev.vikingsen.absclientapp.domain.usecase.SaveProgressUseCase
import dev.vikingsen.absclientapp.domain.usecase.StartPlaybackSessionUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

class PlayerManager(
    private val context: Context,
    private val exoPlayer: ExoPlayer,
    private val settingsRepository: SettingsRepository,
    private val saveProgressUseCase: SaveProgressUseCase,
    private val startPlaybackSessionUseCase: StartPlaybackSessionUseCase
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionUpdateJob: Job? = null
    private var sleepTimerJob: Job? = null
    
    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0.0)
    val currentPosition: StateFlow<Double> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0.0)
    val duration: StateFlow<Double> = _duration.asStateFlow()

    private val _currentChapter = MutableStateFlow<Chapter?>(null)
    val currentChapter: StateFlow<Chapter?> = _currentChapter.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow(0L) // in milliseconds
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining.asStateFlow()

    private var sortedFiles: List<AudioFile> = emptyList()
    private var cumulativeDurations: DoubleArray = DoubleArray(0)
    
    private var sessionId: String? = null
    private var accumulatedTimeListened = 0.0
    private var lastRecordedAbsolutePosition = 0.0

    private val deviceId: String by lazy {
        runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull() ?: UUID.randomUUID().toString()
    }

    private val deviceName: String by lazy {
        "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                _isPlaying.value = isPlayingChanged
                if (isPlayingChanged) {
                    startTrackingPosition()
                } else {
                    stopTrackingPosition()
                    // Sync immediately on pause
                    syncProgressNow()
                }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                _playbackSpeed.value = playbackParameters.speed
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                // Handle natural track transitions or seeking
                updateAbsolutePosition()
            }
        })
    }

    fun playBook(book: Book, startPosition: Double) {
        scope.launch {
            // Stop current playback
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            
            _currentBook.value = book
            sortedFiles = book.audioFiles.sortedBy { it.index }
            
            // Calculate cumulative durations
            cumulativeDurations = DoubleArray(sortedFiles.size + 1)
            var sum = 0.0
            cumulativeDurations[0] = 0.0
            for (i in sortedFiles.indices) {
                sum += sortedFiles[i].duration
                cumulativeDurations[i + 1] = sum
            }
            _duration.value = sum
            
            // Create MediaItems
            val mediaItems = sortedFiles.map { file ->
                val uriString = if (file.localPath != null && File(file.localPath).exists()) {
                    file.localPath
                } else {
                    val serverUrl = settingsRepository.getServerUrl() ?: ""
                    val sanitizedBase = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
                    "${sanitizedBase}api/items/${book.id}/file/${file.ino}/download"
                }

                val coverUri = if (book.coverPath != null && File(book.coverPath).exists()) {
                    android.net.Uri.fromFile(File(book.coverPath))
                } else {
                    val serverUrl = settingsRepository.getServerUrl() ?: ""
                    val sanitizedBase = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
                    android.net.Uri.parse("${sanitizedBase}api/items/${book.id}/cover")
                }

                val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(book.title)
                    .setArtist(book.author)
                    .setAlbumArtist(book.narrator)
                    .setArtworkUri(coverUri)
                    .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
                    .build()

                MediaItem.Builder()
                    .setUri(uriString)
                    .setMediaId("${book.id}_${file.ino}")
                    .setMediaMetadata(mediaMetadata)
                    .build()
            }
            
            exoPlayer.setMediaItems(mediaItems)
            exoPlayer.prepare()
            
            // Seek to start position
            seekTo(startPosition)
            
            // Start playback session on server
            sessionId = null
            accumulatedTimeListened = 0.0
            lastRecordedAbsolutePosition = startPosition
            
            exoPlayer.play()
            
            // Start Foreground Service
            runCatching {
                val intent = android.content.Intent(context, AudiobookPlayerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
            
            // Start session async
            launch(Dispatchers.IO) {
                val result = startPlaybackSessionUseCase(book.id, deviceId, deviceName)
                if (result.isSuccess) {
                    sessionId = result.getOrNull()
                }
            }
        }
    }

    fun play() {
        exoPlayer.play()
        runCatching {
            val intent = android.content.Intent(context, AudiobookPlayerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun seekTo(position: Double) {
        if (sortedFiles.isEmpty()) return
        val target = position.coerceIn(0.0, _duration.value)
        
        var fileIndex = 0
        for (i in sortedFiles.indices) {
            if (target >= cumulativeDurations[i] && target < cumulativeDurations[i + 1]) {
                fileIndex = i
                break
            }
        }
        if (target >= _duration.value) {
            fileIndex = sortedFiles.lastIndex.coerceAtLeast(0)
        }
        
        val fileOffset = target - cumulativeDurations[fileIndex]
        exoPlayer.seekTo(fileIndex, (fileOffset * 1000).toLong())
        updateAbsolutePosition()
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemaining.value = 0L
            return
        }
        val durationMs = minutes * 60 * 1000L
        _sleepTimerRemaining.value = durationMs
        
        sleepTimerJob = scope.launch {
            var remaining = durationMs
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _sleepTimerRemaining.value = remaining.coerceAtLeast(0)
            }
            pause()
            _sleepTimerRemaining.value = 0L
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemaining.value = 0L
    }

    private fun startTrackingPosition() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive) {
                updateAbsolutePosition()
                delay(250)
            }
        }
    }

    private fun stopTrackingPosition() {
        positionUpdateJob?.cancel()
    }

    private fun updateAbsolutePosition() {
        val fileIndex = exoPlayer.currentMediaItemIndex
        if (fileIndex !in sortedFiles.indices) return
        
        val fileOffset = exoPlayer.currentPosition / 1000.0
        val absPos = cumulativeDurations[fileIndex] + fileOffset
        _currentPosition.value = absPos
        
        // Update chapter
        val book = _currentBook.value
        if (book != null) {
            val chapter = book.chapters.find { absPos >= it.start && absPos < it.end }
            _currentChapter.value = chapter
        }

        // Track time listened for syncing
        val delta = absPos - lastRecordedAbsolutePosition
        if (delta > 0 && delta < 2.0 && exoPlayer.isPlaying) {
            accumulatedTimeListened += delta
        }
        lastRecordedAbsolutePosition = absPos

        // Periodically sync progress (every 10 seconds of active playback)
        if (accumulatedTimeListened >= 10.0) {
            syncProgressNow()
        }
    }

    private fun syncProgressNow() {
        val book = _currentBook.value ?: return
        val currentPos = _currentPosition.value
        val totalDur = _duration.value
        val timeListenedToSend = accumulatedTimeListened
        val activeSessionId = sessionId
        
        accumulatedTimeListened = 0.0

        scope.launch(Dispatchers.IO) {
            // Always save locally first
            saveProgressUseCase.saveLocal(book.id, currentPos, totalDur)
            
            // Sync with server if online and we have an active session
            if (activeSessionId != null && timeListenedToSend > 0) {
                saveProgressUseCase.syncPlayback(activeSessionId, timeListenedToSend, currentPos)
            } else {
                // If offline, save progress for future batch/static update
                val progress = if (totalDur > 0) (currentPos / totalDur).toFloat() else 0f
                saveProgressUseCase.syncStatic(book.id, currentPos, progress, progress >= 0.99f)
            }
        }
    }

    fun release() {
        scope.cancel()
        exoPlayer.release()
    }
}
