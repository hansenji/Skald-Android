package dev.vikingsen.skald.data.repository

import dev.vikingsen.skald.core.preferences.PreferencesManager
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.core.model.Library
import dev.vikingsen.skald.core.database.AppDatabaseProvider
import dev.vikingsen.skald.data.mapper.toDomain
import dev.vikingsen.skald.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val preferencesManager: PreferencesManager,
    private val dbProvider: AppDatabaseProvider
) : SettingsRepository {

    private val db = dbProvider.database
    private val libraryDao = db.libraryDao()

    override fun getServerUrl(): String? = preferencesManager.getServerUrl()

    override fun getUsername(): String? = preferencesManager.getUsername()

    override fun getToken(): String? = preferencesManager.getToken()

    override fun getLibraryId(): String? = preferencesManager.getLibraryId()

    override suspend fun saveConnectionDetails(url: String, username: String, token: String) {
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

    override suspend fun clear() {
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

    override fun getGoBackOnInterrupt(): Boolean = preferencesManager.getGoBackOnInterrupt()

    override fun saveGoBackOnInterrupt(enabled: Boolean) {
        preferencesManager.saveGoBackOnInterrupt(enabled)
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

    override fun getLibrarySeriesETag(libraryId: String): String? = preferencesManager.getLibrarySeriesETag(libraryId)

    override fun saveLibrarySeriesETag(libraryId: String, etag: String) {
        preferencesManager.saveLibrarySeriesETag(libraryId, etag)
    }

    override fun getSeriesFilter(): String? = preferencesManager.getSeriesFilter()

    override fun saveSeriesFilter(filter: String) {
        preferencesManager.saveSeriesFilter(filter)
    }

    override fun getSeriesSortOption(): String? = preferencesManager.getSeriesSortOption()

    override fun saveSeriesSortOption(sort: String) {
        preferencesManager.saveSeriesSortOption(sort)
    }

    override suspend fun getCachedLibraries(): List<Library> {
        return libraryDao.getAllLibraries().map { it.toDomain() }
    }

    override suspend fun saveCachedLibraries(libraries: List<Library>) {
        libraryDao.insertAll(libraries.map { it.toEntity() })
    }

    override fun getHideEmptyLibraryTabs(): Boolean = preferencesManager.getHideEmptyLibraryTabs()

    override fun saveHideEmptyLibraryTabs(enabled: Boolean) {
        preferencesManager.saveHideEmptyLibraryTabs(enabled)
    }

    override fun observeHideEmptyLibraryTabs(): Flow<Boolean> = preferencesManager.hideEmptyLibraryTabs
}
