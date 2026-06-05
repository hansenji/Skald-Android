package dev.vikingsen.skald.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.skald.core.model.BookCollection
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.GetMiniPlayerStateUseCase
import dev.vikingsen.skald.domain.usecase.GetCollectionDetailsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionDetailViewModel(
    private val getCollectionDetailsUseCase: GetCollectionDetailsUseCase,
    private val settingsRepository: SettingsRepository,
    private val getMiniPlayerStateUseCase: GetMiniPlayerStateUseCase
) : ViewModel() {

    val collectionId = MutableStateFlow<String?>(null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val collection: StateFlow<BookCollection?> = collectionId
        .filterNotNull()
        .flatMapLatest { id ->
            flow {
                _isRefreshing.value = true
                _error.value = null
                val result = getCollectionDetailsUseCase.getCollection(id, forceRefresh = true)
                if (result.isFailure) {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to load collection details"
                }
                _isRefreshing.value = false
                
                if (result.isSuccess) {
                    emit(result.getOrNull())
                } else {
                    emit(getCollectionDetailsUseCase.getCollection(id, forceRefresh = false).getOrNull())
                }
            }
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    val books: StateFlow<List<BookCardUiModel>> = collectionId
        .filterNotNull()
        .flatMapLatest { id -> getCollectionDetailsUseCase.getBooks(id) }
        .map { booksWithProgress ->
            val serverUrl = settingsRepository.getServerUrl() ?: ""
            val token = settingsRepository.getToken() ?: ""
            
            booksWithProgress
                .map { bookWithProgress ->
                    val book = bookWithProgress.book
                    val progress = bookWithProgress.progress
                    
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
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    val showMiniPlayer: StateFlow<Boolean> = getMiniPlayerStateUseCase()
        .map { it != null }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = false)

    fun setCollectionId(id: String) {
        collectionId.value = id
    }

    fun refresh() {
        val id = collectionId.value ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            val result = getCollectionDetailsUseCase.getCollection(id, forceRefresh = true)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to refresh collection details"
            }
            _isRefreshing.value = false
        }
    }
}
