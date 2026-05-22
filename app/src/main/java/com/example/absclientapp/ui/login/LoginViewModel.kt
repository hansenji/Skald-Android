package com.example.absclientapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.absclientapp.data.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: AudiobookshelfRepository) : ViewModel() {
    private val _serverUrl = MutableStateFlow(repository.preferencesManager.getServerUrl() ?: "")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _username = MutableStateFlow(repository.preferencesManager.getUsername() ?: "")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
    }

    fun updateUsername(user: String) {
        _username.value = user
    }

    fun updatePassword(pass: String) {
        _password.value = pass
    }

    fun login(onSuccess: () -> Unit) {
        val url = _serverUrl.value.trim()
        val user = _username.value.trim()
        val pass = _password.value.trim()

        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            _error.value = "All fields are required"
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val loginResult = repository.login(url, user, pass)
            if (loginResult.isSuccess) {
                val librariesResult = repository.fetchLibraries()
                if (librariesResult.isSuccess) {
                    val libs = librariesResult.getOrThrow()
                    val audiobookLib = libs.find { it.type == "audiobook" } ?: libs.firstOrNull()
                    if (audiobookLib != null) {
                        repository.preferencesManager.saveLibraryId(audiobookLib.id)
                        launch {
                            repository.syncLibraryBooks(audiobookLib.id)
                        }
                    }
                    _isLoading.value = false
                    onSuccess()
                } else {
                    _isLoading.value = false
                    _error.value = "Failed to fetch libraries: ${librariesResult.exceptionOrNull()?.message}"
                }
            } else {
                _isLoading.value = false
                _error.value = loginResult.exceptionOrNull()?.message ?: "Login failed"
            }
        }
    }
}
