package com.example.absclientapp.data.repository

import com.example.absclientapp.data.PreferencesManager
import com.example.absclientapp.domain.repository.SettingsRepository

class SettingsRepositoryImpl(
    private val preferencesManager: PreferencesManager
) : SettingsRepository {

    override fun getServerUrl(): String? = preferencesManager.getServerUrl()

    override fun getUsername(): String? = preferencesManager.getUsername()

    override fun getToken(): String? = preferencesManager.getToken()

    override fun getLibraryId(): String? = preferencesManager.getLibraryId()

    override fun saveConnectionDetails(url: String, username: String, token: String) {
        preferencesManager.saveConnectionDetails(url, username, token)
    }

    override fun saveLibraryId(libraryId: String) {
        preferencesManager.saveLibraryId(libraryId)
    }

    override fun isLoggedIn(): Boolean = preferencesManager.isLoggedIn()

    override fun clear() {
        preferencesManager.clear()
    }
}
