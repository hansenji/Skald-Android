package com.example.absclientapp.domain.repository

interface SettingsRepository {
    fun getServerUrl(): String?
    fun getUsername(): String?
    fun getToken(): String?
    fun getLibraryId(): String?
    fun saveConnectionDetails(url: String, username: String, token: String)
    fun saveLibraryId(libraryId: String)
    fun isLoggedIn(): Boolean
    fun clear()
}
