package dev.vikingsen.absclientapp.domain.repository

interface SettingsRepository {
    fun getServerUrl(): String?
    fun getUsername(): String?
    fun getToken(): String?
    fun getLibraryId(): String?
    fun saveConnectionDetails(url: String, username: String, token: String)
    fun saveLibraryId(libraryId: String)
    fun getReadStatusFilter(): String?
    fun saveReadStatusFilter(filter: String)
    fun getSortOption(): String?
    fun saveSortOption(sort: String)
    fun getDownloadedOnlyFilter(): Boolean
    fun saveDownloadedOnlyFilter(downloadedOnly: Boolean)
    fun isLoggedIn(): Boolean
    fun clear()
    fun getSkipForwardDuration(): Int
    fun saveSkipForwardDuration(duration: Int)
    fun getSkipBackwardDuration(): Int
    fun saveSkipBackwardDuration(duration: Int)
    fun getPlaybackSpeed(): Float
    fun savePlaybackSpeed(speed: Float)
}
