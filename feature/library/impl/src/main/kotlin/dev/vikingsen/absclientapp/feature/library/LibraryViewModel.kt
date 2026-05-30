package dev.vikingsen.absclientapp.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dev.vikingsen.absclientapp.core.model.Library
import dev.vikingsen.absclientapp.core.model.ReadStatusFilter
import dev.vikingsen.absclientapp.core.model.SortOption
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository
import dev.vikingsen.absclientapp.domain.usecase.FetchLibrariesUseCase
import dev.vikingsen.absclientapp.domain.usecase.GetBooksUseCase
import dev.vikingsen.absclientapp.domain.usecase.LogoutUseCase
import dev.vikingsen.absclientapp.domain.usecase.SyncLibraryBooksUseCase
import dev.vikingsen.absclientapp.domain.usecase.GetMiniPlayerStateUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

data class LibraryUiModel(
    val id: String,
    val name: String
)

private data class CombinedParams(
    val libraryId: String,
    val query: String,
    val status: ReadStatusFilter,
    val downloadedOnly: Boolean,
    val sort: SortOption
)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val getBooksUseCase: GetBooksUseCase,
    private val syncLibraryBooksUseCase: SyncLibraryBooksUseCase,
    private val fetchLibrariesUseCase: FetchLibrariesUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val settingsRepository: SettingsRepository,
    private val getMiniPlayerStateUseCase: GetMiniPlayerStateUseCase
) : ViewModel() {

    val showMiniPlayer: StateFlow<Boolean> = getMiniPlayerStateUseCase()
        .map { it != null }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = false)

    private val initialFilterStatus = settingsRepository.getReadStatusFilter()?.let {
        runCatching { ReadStatusFilter.valueOf(it) }.getOrNull()
    } ?: ReadStatusFilter.ALL

    private val initialSortOption = settingsRepository.getSortOption()?.let {
        runCatching { SortOption.valueOf(it) }.getOrNull()
    } ?: SortOption.TITLE_ASC

    private val initialDownloadedOnly = settingsRepository.getDownloadedOnlyFilter()

    val selectedLibraryId = MutableStateFlow(settingsRepository.getLibraryId() ?: "")
    val searchQuery = MutableStateFlow("")
    val filterStatus = MutableStateFlow(initialFilterStatus)
    val filterDownloadedOnly = MutableStateFlow(initialDownloadedOnly)
    val sortBy = MutableStateFlow(initialSortOption)

    val libraries = MutableStateFlow<List<LibraryUiModel>>(emptyList())
    val syncIntervalHours = MutableStateFlow(settingsRepository.getLibrarySyncIntervalHours())

    val books: Flow<PagingData<BookCardUiModel>> = combine(
        selectedLibraryId,
        searchQuery,
        filterStatus,
        filterDownloadedOnly,
        sortBy
    ) { libraryId, query, status, downloadedOnly, sort ->
        CombinedParams(libraryId, query, status, downloadedOnly, sort)
    }.flatMapLatest { params ->
        if (params.libraryId.isEmpty()) {
            flowOf(PagingData.empty())
        } else {
            getBooksUseCase(
                libraryId = params.libraryId,
                query = params.query,
                filter = params.status,
                downloadedOnly = params.downloadedOnly,
                sortBy = params.sort
            ).map { pagingData ->
                pagingData.map { bookWithProgress ->
                    val book = bookWithProgress.book
                    val progress = bookWithProgress.progress
                    val serverUrl = settingsRepository.getServerUrl() ?: ""
                    val token = settingsRepository.getToken() ?: ""
                    
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
            }.cachedIn(viewModelScope)
        }
    }

    val isRefreshing = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            libraries.value = settingsRepository.getCachedLibraries().map {
                LibraryUiModel(it.id, it.name)
            }
        }
        fetchLibrariesList()
        checkAndPeriodicSync()
    }

    fun setLibraryId(libraryId: String) {
        if (selectedLibraryId.value == libraryId) return
        selectedLibraryId.value = libraryId
        settingsRepository.saveLibraryId(libraryId)
        
        // Trigger full book list sync for newly selected library
        refresh()
    }

    fun setFilterStatus(status: ReadStatusFilter) {
        filterStatus.value = status
        settingsRepository.saveReadStatusFilter(status.name)
    }

    fun setFilterDownloadedOnly(downloadedOnly: Boolean) {
        filterDownloadedOnly.value = downloadedOnly
        settingsRepository.saveDownloadedOnlyFilter(downloadedOnly)
    }

    fun setSortBy(sort: SortOption) {
        sortBy.value = sort
        settingsRepository.saveSortOption(sort.name)
    }

    fun setSyncIntervalHours(hours: Int) {
        syncIntervalHours.value = hours
        settingsRepository.saveLibrarySyncIntervalHours(hours)
    }

    fun refresh(forceRefresh: Boolean = false) {
        val libraryId = selectedLibraryId.value
        if (libraryId.isEmpty()) {
            error.value = "No library selected"
            return
        }

        isRefreshing.value = true
        error.value = null
        viewModelScope.launch {
            // Also refresh library list
            val libsResult = fetchLibrariesUseCase()
            if (libsResult.isSuccess) {
                libraries.value = libsResult.getOrThrow().map { LibraryUiModel(it.id, it.name) }
            }
            
            // Sync books
            val result = syncLibraryBooksUseCase(libraryId, forceRefresh)
            if (result.isFailure) {
                error.value = result.exceptionOrNull()?.message ?: "Failed to sync library"
            }
            isRefreshing.value = false
        }
    }

    private fun fetchLibrariesList() {
        viewModelScope.launch {
            val result = fetchLibrariesUseCase()
            if (result.isSuccess) {
                val libs = result.getOrThrow()
                libraries.value = libs.map { LibraryUiModel(it.id, it.name) }
                
                // Auto-selection on first load if none selected
                if (selectedLibraryId.value.isEmpty()) {
                    val audiobookLib = libs.find { it.type == "audiobook" } ?: libs.firstOrNull()
                    if (audiobookLib != null) {
                        setLibraryId(audiobookLib.id)
                    }
                }
            }
        }
    }

    private fun checkAndPeriodicSync() {
        val libraryId = selectedLibraryId.value
        if (libraryId.isEmpty()) return
        
        val lastSync = settingsRepository.getLibraryLastSyncTimestamp()
        val intervalHours = settingsRepository.getLibrarySyncIntervalHours()
        
        if (intervalHours > 0) {
            val intervalMs = intervalHours.toLong() * 60L * 60L * 1000L
            val elapsed = System.currentTimeMillis() - lastSync
            if (elapsed > intervalMs) {
                viewModelScope.launch {
                    isRefreshing.value = true
                    error.value = null
                    val result = syncLibraryBooksUseCase(libraryId)
                    if (result.isFailure) {
                        error.value = result.exceptionOrNull()?.message ?: "Failed to sync library"
                    }
                    isRefreshing.value = false
                }
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onComplete()
        }
    }
}
