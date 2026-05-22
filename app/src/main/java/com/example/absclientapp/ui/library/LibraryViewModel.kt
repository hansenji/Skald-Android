package com.example.absclientapp.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.absclientapp.data.database.BookEntity
import com.example.absclientapp.data.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: AudiobookshelfRepository) : ViewModel() {
    val books: StateFlow<List<BookEntity>> = repository.getBooksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val libraryId = repository.preferencesManager.getLibraryId()
        if (libraryId.isNullOrEmpty()) {
            _error.value = "No library selected"
            return
        }

        _isRefreshing.value = true
        _error.value = null
        viewModelScope.launch {
            val result = repository.syncLibraryBooks(libraryId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to sync library"
            }
            _isRefreshing.value = false
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.preferencesManager.clear()
            repository.clearLocalData()
            onComplete()
        }
    }
}
