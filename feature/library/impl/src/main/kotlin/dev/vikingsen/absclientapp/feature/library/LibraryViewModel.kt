package dev.vikingsen.absclientapp.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.model.PlaybackProgress
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository
import dev.vikingsen.absclientapp.domain.usecase.GetBooksUseCase
import dev.vikingsen.absclientapp.domain.usecase.GetPlaybackProgressUseCase
import dev.vikingsen.absclientapp.domain.usecase.LogoutUseCase
import dev.vikingsen.absclientapp.domain.usecase.SyncLibraryBooksUseCase
import dev.vikingsen.absclientapp.domain.usecase.GetMiniPlayerStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ReadStatusFilter {
    ALL,
    UNREAD,
    IN_PROGRESS,
    READ
}

enum class SortOption {
    TITLE_ASC,
    TITLE_DESC,
    AUTHOR_ASC,
    AUTHOR_DESC,
    DURATION_ASC,
    DURATION_DESC,
    LAST_PLAYED
}

data class PlaybackProgressUiModel(
    val progress: Float,
    val isFinished: Boolean,
    val currentTime: Double,
    val lastUpdated: Long
)

data class BookCardUiModel(
    val id: String,
    val title: String,
    val author: String,
    val narrator: String,
    val coverUrl: String,
    val authorizationHeader: String?,
    val isDownloaded: Boolean,
    val duration: Double,
    val progress: PlaybackProgressUiModel?
)

class LibraryViewModel(
    private val getBooksUseCase: GetBooksUseCase,
    private val getPlaybackProgressUseCase: GetPlaybackProgressUseCase,
    private val syncLibraryBooksUseCase: SyncLibraryBooksUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val settingsRepository: SettingsRepository,
    private val getMiniPlayerStateUseCase: GetMiniPlayerStateUseCase
) : ViewModel() {

    val showMiniPlayer: StateFlow<Boolean> = getMiniPlayerStateUseCase()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val initialFilterStatus = settingsRepository.getReadStatusFilter()?.let {
        runCatching { ReadStatusFilter.valueOf(it) }.getOrNull()
    } ?: ReadStatusFilter.ALL

    private val initialSortOption = settingsRepository.getSortOption()?.let {
        runCatching { SortOption.valueOf(it) }.getOrNull()
    } ?: SortOption.TITLE_ASC

    private val initialDownloadedOnly = settingsRepository.getDownloadedOnlyFilter()

    private val _filterStatus = MutableStateFlow(initialFilterStatus)
    val filterStatus: StateFlow<ReadStatusFilter> = _filterStatus.asStateFlow()

    private val _filterDownloadedOnly = MutableStateFlow(initialDownloadedOnly)
    val filterDownloadedOnly: StateFlow<Boolean> = _filterDownloadedOnly.asStateFlow()

    private val _sortBy = MutableStateFlow(initialSortOption)
    val sortBy: StateFlow<SortOption> = _sortBy.asStateFlow()

    private val _books = getBooksUseCase()
    private val _progress = getPlaybackProgressUseCase()

    val books: StateFlow<List<BookCardUiModel>> = combine(
        _books,
        _progress,
        _filterStatus,
        _filterDownloadedOnly,
        _sortBy
    ) { booksList, progressList, status, downloadedOnly, sort ->
        val progressMap = progressList.associateBy { it.bookId }
        val serverUrl = settingsRepository.getServerUrl() ?: ""
        val token = settingsRepository.getToken() ?: ""
        
        booksList
            .map { book ->
                val progress = progressMap[book.id]
                val coverUrl = if (!book.coverPath.isNullOrEmpty()) book.coverPath!!
                               else "${serverUrl.trimEnd('/')}/api/items/${book.id}/cover"
                val authHeader = if (!book.coverPath.isNullOrEmpty()) null
                                 else "Bearer $token"
                BookCardUiModel(
                    id = book.id,
                    title = book.title,
                    author = book.author,
                    narrator = book.narrator,
                    coverUrl = coverUrl,
                    authorizationHeader = authHeader,
                    isDownloaded = book.isDownloaded,
                    duration = book.duration,
                    progress = progress?.let {
                        PlaybackProgressUiModel(
                            progress = it.progress,
                            isFinished = it.isFinished,
                            currentTime = it.currentTime,
                            lastUpdated = it.lastUpdated
                        )
                    }
                )
            }
            .filter { card ->
                // Filter by Downloaded
                if (downloadedOnly && !card.isDownloaded) return@filter false
                
                // Filter by Read Status
                val progress = card.progress
                when (status) {
                    ReadStatusFilter.ALL -> true
                    ReadStatusFilter.UNREAD -> progress == null || (progress.progress == 0f && !progress.isFinished)
                    ReadStatusFilter.IN_PROGRESS -> progress != null && progress.progress > 0f && !progress.isFinished
                    ReadStatusFilter.READ -> progress?.isFinished == true || (progress != null && progress.progress >= 0.99f)
                }
            }
            .sortedWith { a, b ->
                when (sort) {
                    SortOption.TITLE_ASC -> a.title.compareTo(b.title, ignoreCase = true)
                    SortOption.TITLE_DESC -> b.title.compareTo(a.title, ignoreCase = true)
                    SortOption.AUTHOR_ASC -> a.author.compareTo(b.author, ignoreCase = true)
                    SortOption.AUTHOR_DESC -> b.author.compareTo(a.author, ignoreCase = true)
                    SortOption.DURATION_ASC -> a.duration.compareTo(b.duration)
                    SortOption.DURATION_DESC -> b.duration.compareTo(a.duration)
                    SortOption.LAST_PLAYED -> {
                        val timeA = a.progress?.lastUpdated ?: 0L
                        val timeB = b.progress?.lastUpdated ?: 0L
                        if (timeA == 0L && timeB == 0L) {
                            a.title.compareTo(b.title, ignoreCase = true)
                        } else {
                            timeB.compareTo(timeA)
                        }
                    }
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
    }

    fun setFilterStatus(status: ReadStatusFilter) {
        _filterStatus.value = status
        settingsRepository.saveReadStatusFilter(status.name)
    }

    fun setFilterDownloadedOnly(downloadedOnly: Boolean) {
        _filterDownloadedOnly.value = downloadedOnly
        settingsRepository.saveDownloadedOnlyFilter(downloadedOnly)
    }

    fun setSortBy(sort: SortOption) {
        _sortBy.value = sort
        settingsRepository.saveSortOption(sort.name)
    }

    fun refresh() {
        val libraryId = settingsRepository.getLibraryId()
        if (libraryId.isNullOrEmpty()) {
            _error.value = "No library selected"
            return
        }

        _isRefreshing.value = true
        _error.value = null
        viewModelScope.launch {
            val result = syncLibraryBooksUseCase(libraryId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to sync library"
            }
            _isRefreshing.value = false
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onComplete()
        }
    }
}
