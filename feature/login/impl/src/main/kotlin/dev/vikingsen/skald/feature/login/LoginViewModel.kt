package dev.vikingsen.skald.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.FetchLibrariesUseCase
import dev.vikingsen.skald.domain.usecase.LoginUseCase
import dev.vikingsen.skald.domain.usecase.SyncLibraryBooksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val fetchLibrariesUseCase: FetchLibrariesUseCase,
    private val syncLibraryBooksUseCase: SyncLibraryBooksUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val serverUrl: StateFlow<String>
        field = MutableStateFlow(settingsRepository.getServerUrl() ?: "")

    val username: StateFlow<String>
        field = MutableStateFlow(settingsRepository.getUsername() ?: "")

    val password: StateFlow<String>
        field = MutableStateFlow("")

    val isLoading: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val error: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    fun updateServerUrl(url: String) {
        serverUrl.value = url
    }

    fun updateUsername(user: String) {
        username.value = user
    }

    fun updatePassword(pass: String) {
        password.value = pass
    }

    fun login(onSuccess: () -> Unit) {
        val url = serverUrl.value.trim()
        val user = username.value.trim()
        val pass = password.value.trim()

        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            error.value = "All fields are required"
            return
        }

        isLoading.value = true
        error.value = null

        viewModelScope.launch {
            val loginResult = loginUseCase(url, user, pass)
            if (loginResult.isSuccess) {
                val librariesResult = fetchLibrariesUseCase()
                if (librariesResult.isSuccess) {
                    val libs = librariesResult.getOrThrow()
                    val audiobookLib = libs.find { it.type == "audiobook" } ?: libs.firstOrNull()
                    if (audiobookLib != null) {
                        settingsRepository.saveLibraryId(audiobookLib.id)
                        launch {
                            syncLibraryBooksUseCase(audiobookLib.id)
                        }
                    }
                    isLoading.value = false
                    onSuccess()
                } else {
                    isLoading.value = false
                    error.value = "Failed to fetch libraries: ${librariesResult.exceptionOrNull()?.message}"
                }
            } else {
                isLoading.value = false
                error.value = loginResult.exceptionOrNull()?.message ?: "Login failed"
            }
        }
    }
}
