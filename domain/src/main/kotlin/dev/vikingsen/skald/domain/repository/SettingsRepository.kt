package dev.vikingsen.skald.domain.repository

import dev.vikingsen.skald.core.model.Library

interface SettingsRepository {
    fun getServerUrl(): String?
    fun getUsername(): String?
    fun getToken(): String?
    fun getLibraryId(): String?
    suspend fun saveConnectionDetails(url: String, username: String, token: String)
    fun saveLibraryId(libraryId: String)
    fun getReadStatusFilter(): String?
    fun saveReadStatusFilter(filter: String)
    fun getSortOption(): String?
    fun saveSortOption(sort: String)
    fun getDownloadedOnlyFilter(): Boolean
    fun saveDownloadedOnlyFilter(downloadedOnly: Boolean)
    fun isLoggedIn(): Boolean
    suspend fun clear()
    fun getSkipForwardDuration(): Int
    fun saveSkipForwardDuration(duration: Int)
    fun getSkipBackwardDuration(): Int
    fun saveSkipBackwardDuration(duration: Int)
    fun getPlaybackSpeed(): Float
    fun savePlaybackSpeed(speed: Float)

    fun getLibrarySyncIntervalHours(): Int
    fun saveLibrarySyncIntervalHours(hours: Int)
    fun getLibraryLastSyncTimestamp(): Long
    fun saveLibraryLastSyncTimestamp(timestamp: Long)
    fun getLibraryETag(libraryId: String): String?
    fun saveLibraryETag(libraryId: String, etag: String)
    suspend fun getCachedLibraries(): List<Library>
    suspend fun saveCachedLibraries(libraries: List<Library>)
}
