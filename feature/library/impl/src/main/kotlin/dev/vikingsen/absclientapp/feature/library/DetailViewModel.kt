package dev.vikingsen.absclientapp.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.model.PlaybackProgress
import dev.vikingsen.absclientapp.core.model.DownloadStatus
import dev.vikingsen.absclientapp.core.model.DownloadStatusState
import dev.vikingsen.absclientapp.core.model.formatDuration
import dev.vikingsen.absclientapp.core.model.formatPosition
import dev.vikingsen.absclientapp.core.player.PlayerManager
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository
import dev.vikingsen.absclientapp.domain.usecase.GetBookWithProgressUseCase
import dev.vikingsen.absclientapp.domain.usecase.FetchBookDetailsUseCase
import dev.vikingsen.absclientapp.domain.usecase.DownloadAudioFileUseCase
import dev.vikingsen.absclientapp.domain.usecase.DeleteLocalBookFilesUseCase
import dev.vikingsen.absclientapp.domain.usecase.GetMiniPlayerStateUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChapterUiModel(
    val title: String,
    val start: Double,
    val end: Double,
    val startText: String,
    val durationText: String
)

data class BookDetailUiModel(
    val id: String,
    val title: String,
    val author: String,
    val narrator: String,
    val duration: Double,
    val durationText: String,
    val coverUrl: String,
    val authorizationHeader: String?,
    val isDownloaded: Boolean,
    val description: String,
    val chapters: List<ChapterUiModel>,
    val progress: PlaybackProgressUiModel?,
    val progressLeftText: String?
)

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModel(
    private val getBookWithProgressUseCase: GetBookWithProgressUseCase,
    private val fetchBookDetailsUseCase: FetchBookDetailsUseCase,
    private val downloadAudioFileUseCase: DownloadAudioFileUseCase,
    private val deleteLocalBookFilesUseCase: DeleteLocalBookFilesUseCase,
    private val repository: AudiobookshelfRepository,
    private val settingsRepository: SettingsRepository,
    private val playerManager: PlayerManager,
    private val getMiniPlayerStateUseCase: GetMiniPlayerStateUseCase
) : ViewModel() {
    private val _bookId = MutableStateFlow<String?>(null)

    val bookAndProgress: StateFlow<Pair<Book?, PlaybackProgress?>?> = _bookId
        .filterNotNull()
        .flatMapLatest { id -> getBookWithProgressUseCase(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bookDetail: StateFlow<BookDetailUiModel?> = bookAndProgress
        .map { pair ->
            val book = pair?.first ?: return@map null
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val showMiniPlayer: StateFlow<Boolean> = getMiniPlayerStateUseCase()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val isDownloading: StateFlow<Boolean> = bookAndProgress
        .map { pair ->
            val book = pair?.first
            book?.audioFiles?.any { it.downloadStatus == DownloadStatus.DOWNLOADING } ?: false
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val downloadProgress: StateFlow<Float> = _bookId
        .filterNotNull()
        .flatMapLatest { id -> repository.getBookDownloadFlow(id) }
        .map { state ->
            when (state) {
                is DownloadStatusState.Progress -> state.progress
                is DownloadStatusState.Completed -> 1f
                is DownloadStatusState.Error -> 0f
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val downloadingFileName: StateFlow<String?> = bookAndProgress
        .map { pair ->
            val book = pair?.first
            book?.audioFiles?.find { it.downloadStatus == DownloadStatus.DOWNLOADING }?.filename
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    fun setBookId(id: String) {
        if (_bookId.value == id) return
        _bookId.value = id
        fetchBookDetails(id)
    }

    private fun fetchBookDetails(id: String) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            val result = fetchBookDetailsUseCase(id)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to load details"
            }
            _isLoading.value = false
        }
    }

    fun downloadBook() {
        val book = bookAndProgress.value?.first ?: return
        viewModelScope.launch {
            _downloadError.value = null
            
            val filesToDownload = book.audioFiles.filter { it.downloadStatus != DownloadStatus.COMPLETED }
            if (filesToDownload.isEmpty()) return@launch
            
            val result = downloadAudioFileUseCase(book.id)
            if (result.isFailure) {
                _downloadError.value = result.exceptionOrNull()?.message ?: "Failed to start download"
            }
        }
    }

    fun deleteDownloadedBook() {
        val book = bookAndProgress.value?.first ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _downloadError.value = null
            val result = deleteLocalBookFilesUseCase(book.id)
            if (result.isFailure) {
                _downloadError.value = result.exceptionOrNull()?.message ?: "Failed to delete downloaded files"
            }
            _isLoading.value = false
        }
    }

    fun playBook(startPosition: Double) {
        val book = bookAndProgress.value?.first ?: return
        playerManager.playBook(book, startPosition)
    }
}
