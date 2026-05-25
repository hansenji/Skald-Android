package dev.vikingsen.absclientapp.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("abs_client_prefs", Context.MODE_PRIVATE)

    private val _serverUrl = MutableStateFlow(getServerUrl())
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

    private val _username = MutableStateFlow(getUsername())
    val username: StateFlow<String?> = _username.asStateFlow()

    fun saveConnectionDetails(url: String, user: String, token: String) {
        val sanitizedUrl = if (url.endsWith("/")) url.substring(0, url.length - 1) else url
        prefs.edit().apply {
            putString("server_url", sanitizedUrl)
            putString("username", user)
            putString("token", token)
            apply()
        }
        _serverUrl.value = sanitizedUrl
        _username.value = user
    }

    fun getServerUrl(): String? = prefs.getString("server_url", null)
    fun getUsername(): String? = prefs.getString("username", null)
    fun getToken(): String? = prefs.getString("token", null)

    fun saveLibraryId(libraryId: String) {
        prefs.edit().putString("library_id", libraryId).apply()
    }

    fun getLibraryId(): String? = prefs.getString("library_id", null)

    fun saveReadStatusFilter(filter: String) {
        prefs.edit().putString("filter_read_status", filter).apply()
    }

    fun getReadStatusFilter(): String? = prefs.getString("filter_read_status", null)

    fun saveSortOption(sort: String) {
        prefs.edit().putString("sort_option", sort).apply()
    }

    fun getSortOption(): String? = prefs.getString("sort_option", null)

    fun saveDownloadedOnlyFilter(downloadedOnly: Boolean) {
        prefs.edit().putBoolean("filter_downloaded_only", downloadedOnly).apply()
    }

    fun getDownloadedOnlyFilter(): Boolean = prefs.getBoolean("filter_downloaded_only", false)

    fun clear() {
        prefs.edit().clear().apply()
        _serverUrl.value = null
        _username.value = null
    }

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty() && !getServerUrl().isNullOrEmpty()
}
