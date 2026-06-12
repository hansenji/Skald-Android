package dev.vikingsen.skald.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.skald.core.model.Playlist
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddToPlaylistViewModel(
    private val repository: AudiobookshelfRepository,
    private val bookMenuActionUtil: BookMenuActionUtil
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = repository.getPlaylistsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.syncPlaylists()
        }
    }

    fun addToPlaylist(playlistId: String, bookId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = bookMenuActionUtil.addToPlaylist(playlistId, bookId)
            onResult(result)
        }
    }

    fun createPlaylistAndAdd(name: String, libraryId: String, bookId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = bookMenuActionUtil.createPlaylistWithBook(name, libraryId, bookId)
            onResult(result)
        }
    }
}
