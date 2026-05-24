package com.example.absclientapp.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.absclientapp.domain.model.Book
import com.example.absclientapp.domain.repository.SettingsRepository
import com.example.absclientapp.domain.usecase.GetBooksUseCase
import com.example.absclientapp.domain.usecase.LogoutUseCase
import com.example.absclientapp.domain.usecase.SyncLibraryBooksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val getBooksUseCase: GetBooksUseCase,
    private val syncLibraryBooksUseCase: SyncLibraryBooksUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val books: StateFlow<List<Book>> = getBooksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
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
