package dev.vikingsen.absclientapp.data.repository

import dev.vikingsen.absclientapp.core.preferences.PreferencesManager
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository
import dev.vikingsen.absclientapp.core.model.Library

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

    override fun getSkipForwardDuration(): Int = preferencesManager.getSkipForwardDuration()

    override fun saveSkipForwardDuration(duration: Int) {
        preferencesManager.saveSkipForwardDuration(duration)
    }

    override fun getSkipBackwardDuration(): Int = preferencesManager.getSkipBackwardDuration()

    override fun saveSkipBackwardDuration(duration: Int) {
        preferencesManager.saveSkipBackwardDuration(duration)
    }

    override fun getPlaybackSpeed(): Float = preferencesManager.getPlaybackSpeed()

    override fun savePlaybackSpeed(speed: Float) {
        preferencesManager.savePlaybackSpeed(speed)
    }

    override fun getLibrarySyncIntervalHours(): Int = preferencesManager.getLibrarySyncIntervalHours()

    override fun saveLibrarySyncIntervalHours(hours: Int) {
        preferencesManager.saveLibrarySyncIntervalHours(hours)
    }

    override fun getLibraryLastSyncTimestamp(): Long = preferencesManager.getLibraryLastSyncTimestamp()

    override fun saveLibraryLastSyncTimestamp(timestamp: Long) {
        preferencesManager.saveLibraryLastSyncTimestamp(timestamp)
    }

    override fun getLibraryETag(libraryId: String): String? = preferencesManager.getLibraryETag(libraryId)

    override fun saveLibraryETag(libraryId: String, etag: String) {
        preferencesManager.saveLibraryETag(libraryId, etag)
    }

    override fun getCachedLibraries(): List<Library> = preferencesManager.getLibraries()

    override fun saveCachedLibraries(libraries: List<Library>) {
        preferencesManager.saveLibraries(libraries)
    }
}
