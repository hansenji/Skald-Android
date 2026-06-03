package dev.vikingsen.skald

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.skald.core.preferences.PreferencesManager
import dev.vikingsen.skald.domain.usecase.SyncGlobalProgressUseCase
import dev.vikingsen.skald.feature.library.api.Library
import dev.vikingsen.skald.feature.home.api.Home
import dev.vikingsen.skald.feature.login.api.Login
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val preferencesManager: PreferencesManager,
    private val syncGlobalProgressUseCase: SyncGlobalProgressUseCase
) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean> = preferencesManager.isLoggedInFlow

    val startDestination: NavKey
        get() = if (preferencesManager.isLoggedIn()) Home else Login

    fun syncGlobalProgress() {
        if (preferencesManager.isLoggedIn()) {
            viewModelScope.launch {
                syncGlobalProgressUseCase()
            }
        }
    }
}
