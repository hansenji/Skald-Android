package dev.vikingsen.skald.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.skald.core.model.ReadStatusFilter
import dev.vikingsen.skald.core.model.SortOption
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.FetchLibrariesUseCase
import dev.vikingsen.skald.domain.usecase.GetBooksUseCase
import dev.vikingsen.skald.domain.usecase.GetPlaybackProgressUseCase
import dev.vikingsen.skald.domain.usecase.GetPersonalizedShelvesUseCase
import dev.vikingsen.skald.domain.usecase.SyncPersonalizedShelvesUseCase
import dev.vikingsen.skald.domain.usecase.LogoutUseCase
import dev.vikingsen.skald.domain.usecase.GetMiniPlayerStateUseCase
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
import dev.vikingsen.skald.core.model.HomeEpisodeMetadata
import kotlinx.serialization.json.Json

data class ShelfUiModel(
    val id: String,
    val label: String,
    val total: Int,
    val type: String, // "book", "series", "authors", "episode", "podcast"
    val items: List<ShelfItemUiModel>
)

data class ShelfItemUiModel(
    val entityId: String,
    val title: String?,
    val subtitle: String?,
    val imageUrl: String?,
    val authorizationHeader: String?,
    val additionalData: String? = null,
    val progress: Float? = null,
    val isFinished: Boolean = false,
    val isDownloaded: Boolean = false,
    val episodePubDate: String? = null,
    val episodeDurationText: String? = null
)

data class LibraryUiModel(
    val id: String,
    val name: String
)

data class HomeUiState(
    val shelves: List<ShelfUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val getPersonalizedShelvesUseCase: GetPersonalizedShelvesUseCase,
    private val syncPersonalizedShelvesUseCase: SyncPersonalizedShelvesUseCase,
    private val getBooksUseCase: GetBooksUseCase,
    private val getPlaybackProgressUseCase: GetPlaybackProgressUseCase,
    private val fetchLibrariesUseCase: FetchLibrariesUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val settingsRepository: SettingsRepository,
    private val getMiniPlayerStateUseCase: GetMiniPlayerStateUseCase
) : ViewModel() {

    val selectedLibraryId = MutableStateFlow(settingsRepository.getLibraryId() ?: "")
    val libraries = MutableStateFlow<List<LibraryUiModel>>(emptyList())
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val showMiniPlayer: StateFlow<Boolean> = getMiniPlayerStateUseCase()
        .map { it != null }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = false)

    val uiState: StateFlow<HomeUiState> = combine(
        selectedLibraryId,
        isRefreshing,
        errorMessage
    ) { libraryId, refreshing, errorMsg ->
        Triple(libraryId, refreshing, errorMsg)
    }.flatMapLatest { (libraryId, refreshing, errorMsg) ->
        if (libraryId.isEmpty()) {
            flowOf(HomeUiState(errorMessage = "No library selected"))
        } else {
            combine(
                getPersonalizedShelvesUseCase(libraryId),
                getBooksUseCase(),
                getPlaybackProgressUseCase()
            ) { shelves, books, progressList ->
                val booksMap = books.associateBy { it.id }
                val progressMap = progressList.associateBy { it.bookId }

                val serverUrl = settingsRepository.getServerUrl() ?: ""
                val token = settingsRepository.getToken() ?: ""

                val mappedShelves = shelves.map { shelf ->
                    ShelfUiModel(
                        id = shelf.id,
                        label = shelf.label,
                        total = shelf.total,
                        type = shelf.type,
                        items = shelf.items.map { item ->
                            val localBook = booksMap[item.entityId]
                            val progress = progressMap[item.entityId]

                            val imageUrl = item.imageUrl
                            val coverUrl = if (!imageUrl.isNullOrEmpty()) {
                                if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                                    imageUrl
                                } else if (imageUrl.startsWith("/")) {
                                    "${serverUrl.trimEnd('/')}$imageUrl"
                                } else {
                                    "${serverUrl.trimEnd('/')}/api/items/${item.entityId}/cover"
                                }
                            } else {
                                "${serverUrl.trimEnd('/')}/api/items/${item.entityId}/cover"
                            }
                            val authHeader = if (!imageUrl.isNullOrEmpty() && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                                null
                            } else {
                                "Bearer $token"
                            }

                            val episode = item.additionalData?.let {
                                runCatching { Json.decodeFromString<HomeEpisodeMetadata>(it) }.getOrNull()
                            }
                            val durationText = episode?.duration?.let { seconds ->
                                if (seconds > 0.0) {
                                    val hours = (seconds / 3600).toInt()
                                    val minutes = ((seconds % 3600) / 60).toInt()
                                    if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                                } else null
                            }

                            ShelfItemUiModel(
                                entityId = item.entityId,
                                title = item.title,
                                subtitle = item.subtitle ?: localBook?.author,
                                imageUrl = coverUrl,
                                authorizationHeader = authHeader,
                                additionalData = item.additionalData,
                                progress = progress?.progress,
                                isFinished = progress?.isFinished ?: false,
                                isDownloaded = localBook?.isDownloaded ?: false,
                                episodePubDate = episode?.pubDate,
                                episodeDurationText = durationText
                            )
                        }
                    )
                }

                HomeUiState(
                    shelves = mappedShelves,
                    isLoading = shelves.isEmpty() && refreshing,
                    isRefreshing = refreshing,
                    errorMessage = errorMsg
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            libraries.value = settingsRepository.getCachedLibraries().map {
                LibraryUiModel(it.id, it.name)
            }
        }
        fetchLibrariesList()
        autoSync()
    }

    fun setLibraryId(libraryId: String) {
        if (selectedLibraryId.value == libraryId) return
        selectedLibraryId.value = libraryId
        settingsRepository.saveLibraryId(libraryId)
        refresh(forceRefresh = true)
    }

    fun refresh(forceRefresh: Boolean = false) {
        val libraryId = selectedLibraryId.value
        if (libraryId.isEmpty()) {
            errorMessage.value = "No library selected"
            return
        }

        isRefreshing.value = true
        errorMessage.value = null
        viewModelScope.launch {
            // Also refresh library list
            val libsResult = fetchLibrariesUseCase()
            if (libsResult.isSuccess) {
                libraries.value = libsResult.getOrThrow().map { LibraryUiModel(it.id, it.name) }
            }

            val result = syncPersonalizedShelvesUseCase(libraryId, forceRefresh)
            if (result.isFailure) {
                errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to sync personalized shelves"
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

                if (selectedLibraryId.value.isEmpty()) {
                    val audiobookLib = libs.find { it.type == "audiobook" } ?: libs.firstOrNull()
                    if (audiobookLib != null) {
                        setLibraryId(audiobookLib.id)
                    }
                }
            }
        }
    }

    private fun autoSync() {
        viewModelScope.launch {
            val libraryId = selectedLibraryId.value
            if (libraryId.isNotEmpty()) {
                refresh(forceRefresh = false)
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
