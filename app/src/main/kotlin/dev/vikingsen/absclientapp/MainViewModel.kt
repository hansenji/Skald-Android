package dev.vikingsen.absclientapp

import androidx.lifecycle.ViewModel
import dev.vikingsen.absclientapp.core.preferences.PreferencesManager
import dev.vikingsen.absclientapp.feature.library.api.Library
import dev.vikingsen.absclientapp.feature.home.api.Home
import dev.vikingsen.absclientapp.feature.login.api.Login
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean> = preferencesManager.isLoggedInFlow

    val startDestination: NavKey
        get() = if (preferencesManager.isLoggedIn()) Home else Login
}
