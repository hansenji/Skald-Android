package dev.vikingsen.skald.core.model

sealed interface DownloadStatusState {
    data class Progress(val progress: Float) : DownloadStatusState
    data class Completed(val path: String) : DownloadStatusState
    data class Error(val error: Throwable) : DownloadStatusState
}
