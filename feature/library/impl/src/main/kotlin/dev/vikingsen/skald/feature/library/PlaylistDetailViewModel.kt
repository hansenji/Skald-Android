package dev.vikingsen.skald.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.skald.core.model.Playlist
import dev.vikingsen.skald.core.model.PlaylistItem
import dev.vikingsen.skald.core.model.formatDuration
import dev.vikingsen.skald.core.model.formatPosition
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.GetMiniPlayerStateUseCase
import dev.vikingsen.skald.domain.usecase.PlayPlaylistUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistDetailViewModel(
    private val repository: AudiobookshelfRepository,
    private val playPlaylistUseCase: PlayPlaylistUseCase,
    private val settingsRepository: SettingsRepository,
    private val getMiniPlayerStateUseCase: GetMiniPlayerStateUseCase,
    private val bookMenuActionUtil: BookMenuActionUtil
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

    val serverUrl: String = settingsRepository.getServerUrl() ?: ""

    private val _activeBookId = MutableStateFlow<String?>(null)
    val activeBookId: StateFlow<String?> = _activeBookId.asStateFlow()

    val activeBookDetail: StateFlow<BookDetailUiModel?> = _activeBookId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getBookWithProgressFlow(id).map { pair ->
                val book = pair.first ?: return@map null
                val progress = pair.second
                
                val serverUrl = settingsRepository.getServerUrl() ?: ""
                val token = settingsRepository.getToken() ?: ""
                
                val coverUrl = if (!book.coverPath.isNullOrEmpty()) book.coverPath!!
                               else "${serverUrl.trimEnd('/')}/api/items/${book.id}/cover"
                val authHeader = if (!book.coverPath.isNullOrEmpty()) null
                                 else "Bearer $token"
                                 
                val progressLeftText = progress?.let {
                    val left = book.duration - it.currentTime
                    formatDuration(left)
                }

                BookDetailUiModel(
                    id = book.id,
                    libraryId = book.libraryId,
                    title = book.title,
                    author = book.author,
                    narrator = book.narrator,
                    duration = book.duration,
                    durationText = formatDuration(book.duration),
                    coverUrl = coverUrl,
                    authorizationHeader = authHeader,
                    isDownloaded = book.isDownloaded,
                    description = book.description,
                    chapters = book.chapters.mapIndexed { index, chapter ->
                        ChapterUiModel(
                            title = chapter.title.ifEmpty { "Chapter ${index + 1}" },
                            start = chapter.start,
                            end = chapter.end,
                            startText = formatPosition(chapter.start),
                            durationText = formatDuration(chapter.end - chapter.start)
                        )
                    },
                    progress = progress?.let {
                        PlaybackProgressUiModel(
                            progress = it.progress,
                            isFinished = it.isFinished,
                            currentTime = it.currentTime,
                            lastUpdated = it.lastUpdated
                        )
                    },
                    progressLeftText = progressLeftText
                )
            }
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)



    fun selectBookForMenu(bookId: String?) {
        _activeBookId.value = bookId
    }

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

    fun toggleFinished(bookId: String, isFinished: Boolean) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = bookMenuActionUtil.toggleFinished(bookId, isFinished)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to update finished status"
            }
            _isRefreshing.value = false
        }
    }

    fun discardProgress(bookId: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = bookMenuActionUtil.discardProgress(bookId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to discard progress"
            }
            _isRefreshing.value = false
        }
    }

    fun deleteDownloadedBook(bookId: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = bookMenuActionUtil.deleteDownload(bookId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to delete downloaded files"
            }
            _isRefreshing.value = false
        }
    }

    fun removeFromPlaylist(bookId: String) {
        val id = _playlistId.value ?: return
        val items = _playlistItems.value.filter { it.libraryItemId != bookId }
        updateLocalState(items)
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.removePlaylistItem(id, bookId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to remove item from playlist"
                loadPlaylistDetails(id, forceRefresh = false)
            }
            _isRefreshing.value = false
        }
    }
}
