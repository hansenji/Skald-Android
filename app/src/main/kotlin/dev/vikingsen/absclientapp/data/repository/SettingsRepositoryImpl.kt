package dev.vikingsen.absclientapp.data.repository

import dev.vikingsen.absclientapp.data.PreferencesManager
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository

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

    override fun getReadStatusFilter(): String? = preferencesManager.getReadStatusFilter()

    override fun saveReadStatusFilter(filter: String) {
        preferencesManager.saveReadStatusFilter(filter)
    }

    override fun getSortOption(): String? = preferencesManager.getSortOption()

    override fun saveSortOption(sort: String) {
        preferencesManager.saveSortOption(sort)
    }

    override fun getDownloadedOnlyFilter(): Boolean = preferencesManager.getDownloadedOnlyFilter()

    override fun saveDownloadedOnlyFilter(downloadedOnly: Boolean) {
        preferencesManager.saveDownloadedOnlyFilter(downloadedOnly)
    }

    override fun isLoggedIn(): Boolean = preferencesManager.isLoggedIn()

    override fun clear() {
        preferencesManager.clear()
    }
}
