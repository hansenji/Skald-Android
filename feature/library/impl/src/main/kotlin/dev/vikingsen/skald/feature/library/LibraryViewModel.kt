package dev.vikingsen.skald.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dev.vikingsen.skald.core.model.Library
import dev.vikingsen.skald.core.model.ReadStatusFilter
import dev.vikingsen.skald.core.model.SortOption
import dev.vikingsen.skald.core.model.SeriesFilter
import dev.vikingsen.skald.core.model.SeriesSortOption
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.usecase.FetchLibrariesUseCase
import dev.vikingsen.skald.domain.usecase.GetBooksUseCase
import dev.vikingsen.skald.domain.usecase.LogoutUseCase
import dev.vikingsen.skald.domain.usecase.SyncLibraryBooksUseCase
import dev.vikingsen.skald.domain.usecase.GetMiniPlayerStateUseCase
import dev.vikingsen.skald.domain.usecase.SyncGlobalProgressUseCase
import dev.vikingsen.skald.domain.usecase.GetSeriesUseCase
import dev.vikingsen.skald.domain.usecase.SyncLibrarySeriesUseCase
import dev.vikingsen.skald.domain.usecase.GetAuthorsUseCase
import dev.vikingsen.skald.domain.usecase.SyncLibraryAuthorsUseCase
import dev.vikingsen.skald.domain.usecase.GetCollectionsUseCase
import dev.vikingsen.skald.domain.usecase.SyncLibraryCollectionsUseCase
import dev.vikingsen.skald.core.model.Author
import dev.vikingsen.skald.core.model.AuthorsSortOption
import dev.vikingsen.skald.core.model.BookCollection
import dev.vikingsen.skald.core.model.CollectionsSortOption
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

enum class LibraryTab {
    BOOKS, SERIES, COLLECTIONS, AUTHORS, PLAYLISTS
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
    val progress: PlaybackProgressUiModel?,
    val seriesSequence: String? = null
)

data class LibraryUiModel(
    val id: String,
    val name: String
)

data class SeriesCardUiModel(
    val id: String,
    val name: String,
    val bookCount: Int,
    val readBookCount: Int,
    val progress: Float,
    val covers: List<String>,
    val authorizationHeader: String?,
    val lastPlayed: Long,
    val inProgress: Boolean
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
    private val getSeriesUseCase: GetSeriesUseCase,
    private val syncLibrarySeriesUseCase: SyncLibrarySeriesUseCase,
    private val getAuthorsUseCase: GetAuthorsUseCase,
    private val syncLibraryAuthorsUseCase: SyncLibraryAuthorsUseCase,
    private val getCollectionsUseCase: GetCollectionsUseCase,
    private val syncLibraryCollectionsUseCase: SyncLibraryCollectionsUseCase,
    private val repository: AudiobookshelfRepository,
    private val fetchLibrariesUseCase: FetchLibrariesUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val settingsRepository: SettingsRepository,
    private val getMiniPlayerStateUseCase: GetMiniPlayerStateUseCase,
    private val syncGlobalProgressUseCase: SyncGlobalProgressUseCase
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

    val currentTab = MutableStateFlow(LibraryTab.BOOKS)

    val hideEmptyTabsFlow: StateFlow<Boolean> = settingsRepository.observeHideEmptyLibraryTabs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.getHideEmptyLibraryTabs()
        )

    val visibleTabs: StateFlow<List<LibraryTab>> = combine(
        hideEmptyTabsFlow,
        selectedLibraryId.flatMapLatest { libraryId ->
            if (libraryId.isEmpty()) flowOf(0)
            else repository.getSeriesFlow(libraryId).map { it.size }
        },
        selectedLibraryId.flatMapLatest { libraryId ->
            if (libraryId.isEmpty()) flowOf(0)
            else repository.getAuthorsFlow(libraryId).map { it.size }
        },
        selectedLibraryId.flatMapLatest { libraryId ->
            if (libraryId.isEmpty()) flowOf(0)
            else getCollectionsUseCase(libraryId).map { it.size }
        }
    ) { hideEmpty, seriesCount, authorsCount, collectionsCount ->
        if (hideEmpty) {
            val tabs = mutableListOf(LibraryTab.BOOKS)
            if (seriesCount > 0) {
                tabs.add(LibraryTab.SERIES)
            }
            if (collectionsCount > 0) {
                tabs.add(LibraryTab.COLLECTIONS)
            }
            if (authorsCount > 0) {
                tabs.add(LibraryTab.AUTHORS)
            }
            tabs
        } else {
            LibraryTab.entries.toList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = if (settingsRepository.getHideEmptyLibraryTabs()) {
            listOf(LibraryTab.BOOKS)
        } else {
            LibraryTab.entries.toList()
        }
    )

    val seriesFilter = MutableStateFlow(
        settingsRepository.getSeriesFilter()?.let {
            runCatching { SeriesFilter.valueOf(it) }.getOrNull()
        } ?: SeriesFilter.ALL
    )
    val seriesSort = MutableStateFlow(
        settingsRepository.getSeriesSortOption()?.let {
            runCatching { SeriesSortOption.valueOf(it) }.getOrNull()
        } ?: SeriesSortOption.NAME_ASC
    )

    fun setSeriesFilter(filter: SeriesFilter) {
        seriesFilter.value = filter
        settingsRepository.saveSeriesFilter(filter.name)
    }

    fun setSeriesSort(sort: SeriesSortOption) {
        seriesSort.value = sort
        settingsRepository.saveSeriesSortOption(sort.name)
    }

    val authorsSort = MutableStateFlow(
        settingsRepository.getAuthorsSortOption()?.let {
            runCatching { AuthorsSortOption.valueOf(it) }.getOrNull()
        } ?: AuthorsSortOption.NAME_ASC
    )

    fun setAuthorsSort(sort: AuthorsSortOption) {
        authorsSort.value = sort
        settingsRepository.saveAuthorsSortOption(sort.name)
    }

    val authors: Flow<List<Author>> = selectedLibraryId
        .flatMapLatest { libraryId ->
            if (libraryId.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    getAuthorsUseCase(libraryId),
                    searchQuery,
                    authorsSort
                ) { authorsList, query, sort ->
                    authorsList
                        .filter { author ->
                            query.isEmpty() || author.name.contains(query, ignoreCase = true)
                        }
                        .sortedWith { a, b ->
                            when (sort) {
                                AuthorsSortOption.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                                AuthorsSortOption.NAME_DESC -> b.name.compareTo(a.name, ignoreCase = true)
                                AuthorsSortOption.BOOKS_COUNT_DESC -> b.bookCount.compareTo(a.bookCount)
                            }
                        }
                }
            }
        }

    val collectionsSort = MutableStateFlow(
        settingsRepository.getCollectionsSortOption()?.let {
            runCatching { CollectionsSortOption.valueOf(it) }.getOrNull()
        } ?: CollectionsSortOption.NAME_ASC
    )

    fun setCollectionsSort(sort: CollectionsSortOption) {
        collectionsSort.value = sort
        settingsRepository.saveCollectionsSortOption(sort.name)
    }

    val collections: Flow<List<BookCollection>> = selectedLibraryId
        .flatMapLatest { libraryId ->
            if (libraryId.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    getCollectionsUseCase(libraryId),
                    searchQuery,
                    collectionsSort
                ) { collectionsList, query, sort ->
                    collectionsList
                        .filter { col ->
                            query.isEmpty() || col.name.contains(query, ignoreCase = true)
                        }
                        .sortedWith { a, b ->
                            when (sort) {
                                CollectionsSortOption.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                                CollectionsSortOption.NAME_DESC -> b.name.compareTo(a.name, ignoreCase = true)
                                CollectionsSortOption.BOOKS_COUNT_DESC -> b.bookIds.size.compareTo(a.bookIds.size)
                                CollectionsSortOption.LAST_MODIFIED -> b.lastUpdated.compareTo(a.lastUpdated)
                            }
                        }
                }
            }
        }


    val series: Flow<List<SeriesCardUiModel>> = selectedLibraryId
        .flatMapLatest { libraryId ->
            if (libraryId.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    getSeriesUseCase(libraryId),
                    repository.getBooksWithProgressForLibraryFlow(libraryId),
                    searchQuery,
                    seriesFilter,
                    seriesSort
                ) { seriesList, booksList, query, filter, sort ->
                    val booksBySeries = booksList.groupBy { it.book.seriesId }
                    val serverUrl = settingsRepository.getServerUrl() ?: ""
                    val token = settingsRepository.getToken() ?: ""
                    val authHeader = "Bearer $token"

                    seriesList
                        .map { series ->
                            val seriesBooks = booksBySeries[series.id] ?: emptyList()
                            val readBookCount = seriesBooks.count { it.progress?.isFinished == true || (it.progress?.progress ?: 0f) >= 0.99f }
                            val inProgressBookCount = seriesBooks.count { (it.progress?.progress ?: 0f) > 0f && it.progress?.isFinished != true }
                            val lastPlayed = seriesBooks.mapNotNull { it.progress?.lastUpdated }.maxOrNull() ?: 0L

                            // collage covers
                            val covers = seriesBooks
                                .sortedBy { it.book.seriesSequence.toSequenceNumber() }
                                .take(3)
                                .map { bookWithProgress ->
                                    val book = bookWithProgress.book
                                    if (!book.coverPath.isNullOrEmpty()) book.coverPath!!
                                    else "${serverUrl.trimEnd('/')}/api/items/${book.id}/cover"
                                }

                            val progress = if (series.bookCount > 0) readBookCount.toFloat() / series.bookCount else 0f

                            SeriesCardUiModel(
                                id = series.id,
                                name = series.name,
                                bookCount = series.bookCount,
                                readBookCount = readBookCount,
                                progress = progress,
                                covers = covers,
                                authorizationHeader = authHeader,
                                lastPlayed = lastPlayed,
                                inProgress = inProgressBookCount > 0 || (readBookCount > 0 && readBookCount < series.bookCount)
                            )
                        }
                        .filter { item ->
                            if (query.isNotEmpty() && !item.name.contains(query, ignoreCase = true)) {
                                false
                            } else {
                                when (filter) {
                                    SeriesFilter.ALL -> true
                                    SeriesFilter.IN_PROGRESS -> item.inProgress
                                    SeriesFilter.COMPLETED -> item.readBookCount == item.bookCount && item.bookCount > 0
                                }
                            }
                        }
                        .sortedWith { a, b ->
                            when (sort) {
                                SeriesSortOption.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                                SeriesSortOption.NAME_DESC -> b.name.compareTo(a.name, ignoreCase = true)
                                SeriesSortOption.BOOKS_COUNT_DESC -> b.bookCount.compareTo(a.bookCount)
                                SeriesSortOption.RECENTLY_UPDATED -> b.lastPlayed.compareTo(a.lastPlayed)
                            }
                        }
                }
            }
        }

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
                        },
                        seriesSequence = book.seriesSequence
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
        viewModelScope.launch {
            visibleTabs.collect { tabs ->
                if (currentTab.value !in tabs) {
                    currentTab.value = LibraryTab.BOOKS
                }
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

    fun setCurrentTab(tab: LibraryTab) {
        currentTab.value = tab
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
        isRefreshing.value = true
        error.value = null
        viewModelScope.launch {
            // Also refresh library list
            val libsResult = fetchLibrariesUseCase()
            if (libsResult.isSuccess) {
                val libs = libsResult.getOrThrow()
                libraries.value = libs.map { LibraryUiModel(it.id, it.name) }
                // Auto-selection on first load if none selected
                if (selectedLibraryId.value.isEmpty()) {
                    val audiobookLib = libs.find { it.type == "audiobook" } ?: libs.firstOrNull()
                    if (audiobookLib != null) {
                        setLibraryId(audiobookLib.id)
                    }
                }
            }
            
            if (libraryId.isNotEmpty()) {
                // Sync books, series, authors & collections
                val result = syncLibraryBooksUseCase(libraryId, forceRefresh)
                val seriesResult = syncLibrarySeriesUseCase(libraryId, forceRefresh)
                val authorsResult = syncLibraryAuthorsUseCase(libraryId, forceRefresh)
                val collectionsResult = syncLibraryCollectionsUseCase(libraryId, forceRefresh)
                val progressResult = syncGlobalProgressUseCase(forceRefresh)
                if (result.isFailure || seriesResult.isFailure || authorsResult.isFailure || collectionsResult.isFailure || progressResult.isFailure) {
                    error.value = result.exceptionOrNull()?.message 
                        ?: seriesResult.exceptionOrNull()?.message
                        ?: authorsResult.exceptionOrNull()?.message
                        ?: collectionsResult.exceptionOrNull()?.message
                        ?: progressResult.exceptionOrNull()?.message 
                        ?: "Failed to sync library"
                }
            } else if (libsResult.isFailure) {
                error.value = libsResult.exceptionOrNull()?.message ?: "Failed to fetch libraries"
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
                    val seriesResult = syncLibrarySeriesUseCase(libraryId)
                    val authorsResult = syncLibraryAuthorsUseCase(libraryId)
                    val collectionsResult = syncLibraryCollectionsUseCase(libraryId)
                    val progressResult = syncGlobalProgressUseCase()
                    if (result.isFailure || seriesResult.isFailure || authorsResult.isFailure || collectionsResult.isFailure || progressResult.isFailure) {
                        error.value = result.exceptionOrNull()?.message 
                            ?: seriesResult.exceptionOrNull()?.message
                            ?: authorsResult.exceptionOrNull()?.message
                            ?: collectionsResult.exceptionOrNull()?.message
                            ?: progressResult.exceptionOrNull()?.message 
                            ?: "Failed to sync library"
                    }
                    isRefreshing.value = false
                }
            }
        }
    }

    private fun String?.toSequenceNumber(): Double {
        if (this == null) return Double.MAX_VALUE
        val cleaned = this.trim().takeWhile { it.isDigit() || it == '.' }
        return cleaned.toDoubleOrNull() ?: Double.MAX_VALUE
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onComplete()
        }
    }
}
