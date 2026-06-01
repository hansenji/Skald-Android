package dev.vikingsen.skald.core.network

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T, val etag: String?) : NetworkResult<T>
    data object NotModified : NetworkResult<Nothing>
    data class Error(val message: String) : NetworkResult<Nothing>
}
