package dev.vikingsen.skald.feature.settings

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.LogoutUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val logoutUseCase: LogoutUseCase,
    private val settingsRepository: SettingsRepository,
    private val audiobookshelfRepository: AudiobookshelfRepository,
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _activeLibraryName = MutableStateFlow<String?>(null)
    val activeLibraryName: StateFlow<String?> = _activeLibraryName.asStateFlow()

    private val _skipForwardDuration = MutableStateFlow(30)
    val skipForwardDuration: StateFlow<Int> = _skipForwardDuration.asStateFlow()

    private val _skipBackwardDuration = MutableStateFlow(10)
    val skipBackwardDuration: StateFlow<Int> = _skipBackwardDuration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _goBackOnInterrupt = MutableStateFlow(true)
    val goBackOnInterrupt: StateFlow<Boolean> = _goBackOnInterrupt.asStateFlow()

    private val _hideEmptyLibraryTabs = MutableStateFlow(false)
    val hideEmptyLibraryTabs: StateFlow<Boolean> = _hideEmptyLibraryTabs.asStateFlow()

    private val _syncIntervalHours = MutableStateFlow(24)
    val syncIntervalHours: StateFlow<Int> = _syncIntervalHours.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(0L)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _cacheSize = MutableStateFlow("0 B")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    init {
        loadSettings()
        calculateCacheSize()
        checkOfflineStatus()
    }

    fun loadSettings() {
        _serverUrl.value = settingsRepository.getServerUrl()
        _username.value = settingsRepository.getUsername()
        _skipForwardDuration.value = settingsRepository.getSkipForwardDuration()
        _skipBackwardDuration.value = settingsRepository.getSkipBackwardDuration()
        _playbackSpeed.value = settingsRepository.getPlaybackSpeed()
        _goBackOnInterrupt.value = settingsRepository.getGoBackOnInterrupt()
        _hideEmptyLibraryTabs.value = settingsRepository.getHideEmptyLibraryTabs()
        _syncIntervalHours.value = settingsRepository.getLibrarySyncIntervalHours()
        _lastSyncTimestamp.value = settingsRepository.getLibraryLastSyncTimestamp()

        viewModelScope.launch {
            val libraryId = settingsRepository.getLibraryId()
            if (!libraryId.isNullOrEmpty()) {
                val libs = settingsRepository.getCachedLibraries()
                val activeLib = libs.find { it.id == libraryId }
                _activeLibraryName.value = activeLib?.name ?: "Unknown"
            } else {
                _activeLibraryName.value = null
            }
        }
    }

    fun updateSkipForwardDuration(duration: Int) {
        _skipForwardDuration.value = duration
        settingsRepository.saveSkipForwardDuration(duration)
    }

    fun updateSkipBackwardDuration(duration: Int) {
        _skipBackwardDuration.value = duration
        settingsRepository.saveSkipBackwardDuration(duration)
    }

    fun updatePlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        settingsRepository.savePlaybackSpeed(speed)
    }

    fun updateGoBackOnInterrupt(enabled: Boolean) {
        _goBackOnInterrupt.value = enabled
        settingsRepository.saveGoBackOnInterrupt(enabled)
    }

    fun updateHideEmptyLibraryTabs(enabled: Boolean) {
        _hideEmptyLibraryTabs.value = enabled
        settingsRepository.saveHideEmptyLibraryTabs(enabled)
    }

    fun updateSyncInterval(hours: Int) {
        _syncIntervalHours.value = hours
        settingsRepository.saveLibrarySyncIntervalHours(hours)
    }

    fun syncNow() {
        val libraryId = settingsRepository.getLibraryId()
        if (libraryId.isNullOrEmpty()) return
        
        _isSyncing.value = true
        checkOfflineStatus()
        if (_isOffline.value) {
            _isSyncing.value = false
            return
        }

        viewModelScope.launch {
            try {
                audiobookshelfRepository.syncLibraryBooks(libraryId, forceRefresh = true)
                audiobookshelfRepository.syncGlobalProgress(forceRefresh = true)
                loadSettings()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val downloadsFolder = File(context.getExternalFilesDir(null), "downloads")
                if (downloadsFolder.exists()) {
                    downloadsFolder.deleteRecursively()
                }
                
                audiobookshelfRepository.clearLocalData()
                
                calculateCacheSize()
                loadSettings()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun calculateCacheSize() {
        viewModelScope.launch(ioDispatcher) {
            val downloadsFolder = File(context.getExternalFilesDir(null), "downloads")
            val downloadsSize = getDirSize(downloadsFolder)
            val dbFile = context.getDatabasePath("skald_db")
            val dbSize = if (dbFile.exists()) dbFile.length() else 0L
            val totalSize = downloadsSize + dbSize
            _cacheSize.value = formatSize(totalSize)
        }
    }

    fun checkOfflineStatus() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _isOffline.value = !hasInternet
    }

    private fun getDirSize(dir: File): Long {
        var size = 0L
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) getDirSize(file) else file.length()
            }
        } else if (dir.exists()) {
            size += dir.length()
        }
        return size
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onComplete()
        }
    }
}
