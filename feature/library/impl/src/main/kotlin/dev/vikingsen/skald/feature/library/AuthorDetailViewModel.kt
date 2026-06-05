package dev.vikingsen.skald.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.skald.core.model.Author
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.GetMiniPlayerStateUseCase
import dev.vikingsen.skald.domain.usecase.GetAuthorDetailsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AuthorDetailViewModel(
    private val getAuthorDetailsUseCase: GetAuthorDetailsUseCase,
    private val settingsRepository: SettingsRepository,
    private val getMiniPlayerStateUseCase: GetMiniPlayerStateUseCase
) : ViewModel() {

    val authorId = MutableStateFlow<String?>(null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val author: StateFlow<Author?> = authorId
        .filterNotNull()
        .flatMapLatest { id ->
            flow {
                _isRefreshing.value = true
                _error.value = null
                val result = getAuthorDetailsUseCase.getAuthor(id, forceRefresh = true)
                if (result.isFailure) {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to load author details"
                }
                _isRefreshing.value = false
                
                if (result.isSuccess) {
                    emit(result.getOrNull())
                } else {
                    emit(getAuthorDetailsUseCase.getAuthor(id, forceRefresh = false).getOrNull())
                }
            }
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    val books: StateFlow<List<BookCardUiModel>> = authorId
        .filterNotNull()
        .flatMapLatest { id -> getAuthorDetailsUseCase.getBooks(id) }
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

    fun setAuthorId(id: String) {
        authorId.value = id
    }

    fun refresh() {
        val id = authorId.value ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            val result = getAuthorDetailsUseCase.getAuthor(id, forceRefresh = true)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to refresh author details"
            }
            _isRefreshing.value = false
        }
    }
}
