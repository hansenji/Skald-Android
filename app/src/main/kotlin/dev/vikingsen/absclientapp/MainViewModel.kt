package dev.vikingsen.absclientapp

import androidx.lifecycle.ViewModel
import dev.vikingsen.absclientapp.core.preferences.PreferencesManager
import dev.vikingsen.absclientapp.feature.library.api.Library
import dev.vikingsen.absclientapp.feature.login.api.Login
import androidx.navigation3.runtime.NavKey

class MainViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    val startDestination: NavKey
        get() = if (preferencesManager.isLoggedIn()) Library else Login
}
