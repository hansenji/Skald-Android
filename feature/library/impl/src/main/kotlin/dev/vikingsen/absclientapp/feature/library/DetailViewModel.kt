package dev.vikingsen.absclientapp.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.model.PlaybackProgress
import dev.vikingsen.absclientapp.core.model.DownloadStatus
import dev.vikingsen.absclientapp.core.model.DownloadStatusState
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository
import dev.vikingsen.absclientapp.domain.usecase.GetBookWithProgressUseCase
import dev.vikingsen.absclientapp.domain.usecase.FetchBookDetailsUseCase
import dev.vikingsen.absclientapp.domain.usecase.DownloadAudioFileUseCase
import dev.vikingsen.absclientapp.domain.usecase.DeleteLocalBookFilesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModel(
    private val getBookWithProgressUseCase: GetBookWithProgressUseCase,
    private val fetchBookDetailsUseCase: FetchBookDetailsUseCase,
    private val downloadAudioFileUseCase: DownloadAudioFileUseCase,
    private val deleteLocalBookFilesUseCase: DeleteLocalBookFilesUseCase,
    private val repository: AudiobookshelfRepository
) : ViewModel() {
    private val _bookId = MutableStateFlow<String?>(null)

    val bookAndProgress: StateFlow<Pair<Book?, PlaybackProgress?>?> = _bookId
        .filterNotNull()
        .flatMapLatest { id -> getBookWithProgressUseCase(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
}
