package com.example.absclientapp.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.absclientapp.data.database.BookEntity
import com.example.absclientapp.data.database.PlaybackProgressEntity
import com.example.absclientapp.data.repository.AudiobookshelfRepository
import com.example.absclientapp.data.repository.DownloadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModel(private val repository: AudiobookshelfRepository) : ViewModel() {
    private val _bookId = MutableStateFlow<String?>(null)

    val bookAndProgress: StateFlow<Pair<BookEntity?, PlaybackProgressEntity?>?> = _bookId
        .filterNotNull()
        .flatMapLatest { id -> repository.getBookWithProgressFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadingFileName = MutableStateFlow<String?>(null)
    val downloadingFileName: StateFlow<String?> = _downloadingFileName.asStateFlow()

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
            val result = repository.fetchBookDetails(id)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to load details"
            }
            _isLoading.value = false
        }
    }

    fun downloadBook() {
        val book = bookAndProgress.value?.first ?: return
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            _downloadError.value = null
            
            val filesToDownload = book.audioFiles.filter { it.downloadStatus != "COMPLETED" }
            if (filesToDownload.isEmpty()) {
                _isDownloading.value = false
                return@launch
            }
            
            var completedCount = book.audioFiles.count { it.downloadStatus == "COMPLETED" }
            for (file in filesToDownload) {
                _downloadingFileName.value = file.filename
                
                var downloadFailed = false
                repository.downloadAudioFile(book.id, file).collect { state ->
                    when (state) {
                        is DownloadState.Progress -> {
                            _downloadProgress.value = (completedCount + state.progress) / book.audioFiles.size
                        }
                        is DownloadState.Completed -> {
                            completedCount++
                            _downloadProgress.value = completedCount.toFloat() / book.audioFiles.size
                        }
                        is DownloadState.Error -> {
                            _downloadError.value = "Failed: ${state.error.message}"
                            downloadFailed = true
                        }
                    }
                }
                if (downloadFailed) {
                    break
                }
            }
            _isDownloading.value = false
            _downloadingFileName.value = null
        }
    }
}
