package dev.vikingsen.skald.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.skald.core.model.Playlist
import dev.vikingsen.skald.core.model.PlaylistItem
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.GetMiniPlayerStateUseCase
import dev.vikingsen.skald.domain.usecase.PlayPlaylistUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(
    private val repository: AudiobookshelfRepository,
    private val playPlaylistUseCase: PlayPlaylistUseCase,
    private val settingsRepository: SettingsRepository,
    private val getMiniPlayerStateUseCase: GetMiniPlayerStateUseCase
) : ViewModel() {

    private val _playlistId = MutableStateFlow<String?>(null)
    val playlistId: StateFlow<String?> = _playlistId.asStateFlow()

    val authorizationHeader: String?
        get() = settingsRepository.getToken()?.let { "Bearer $it" }

    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist.asStateFlow()

    private val _playlistItems = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val playlistItems: StateFlow<List<PlaylistItem>> = _playlistItems.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val showMiniPlayer: StateFlow<Boolean> = getMiniPlayerStateUseCase()
        .map { it != null }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = false)

    fun setPlaylistId(id: String) {
        if (_playlistId.value == id) return
        _playlistId.value = id
        loadPlaylistDetails(id, forceRefresh = false)
    }

    fun refresh() {
        val id = _playlistId.value ?: return
        loadPlaylistDetails(id, forceRefresh = true)
    }

    private fun loadPlaylistDetails(id: String, forceRefresh: Boolean) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            val result = repository.getPlaylistDetails(id, forceRefresh = forceRefresh)
            if (result.isSuccess) {
                val p = result.getOrThrow()
                _playlist.value = p
                _playlistItems.value = p.items
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to load playlist details"
                // Try to load cached version if we failed to forceRefresh
                if (forceRefresh) {
                    val cachedResult = repository.getPlaylistDetails(id, forceRefresh = false)
                    if (cachedResult.isSuccess) {
                        val p = cachedResult.getOrThrow()
                        _playlist.value = p
                        _playlistItems.value = p.items
                    }
                }
            }
            _isRefreshing.value = false
        }
    }

    fun playPlaylist(startIndex: Int = 0) {
        val currentPlaylist = _playlist.value ?: return
        val updatedPlaylist = currentPlaylist.copy(items = _playlistItems.value)
        playPlaylistUseCase(updatedPlaylist, startIndex = startIndex)
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val items = _playlistItems.value.toMutableList()
        if (fromIndex !in items.indices || toIndex !in items.indices) return
        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)
        updateLocalState(items)
    }

    fun syncReorder() {
        val id = _playlistId.value ?: return
        viewModelScope.launch {
            val result = repository.updatePlaylistItems(id, _playlistItems.value)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to sync playlist reordering"
            }
        }
    }

    fun deleteItem(item: PlaylistItem) {
        val id = _playlistId.value ?: return
        val items = _playlistItems.value.filter { it.id != item.id }
        updateLocalState(items)
        viewModelScope.launch {
            val result = repository.updatePlaylistItems(id, items)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to delete item from playlist"
            }
        }
    }

    private fun updateLocalState(newItems: List<PlaylistItem>) {
        _playlistItems.value = newItems
        val current = _playlist.value ?: return
        val totalDuration = newItems.sumOf { it.duration }
        _playlist.value = current.copy(
            items = newItems,
            itemCount = newItems.size,
            duration = totalDuration
        )
    }
}
