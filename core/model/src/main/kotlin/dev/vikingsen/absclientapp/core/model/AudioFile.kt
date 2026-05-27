package dev.vikingsen.absclientapp.core.model

data class AudioFile(
    val index: Int,
    val ino: String,
    val duration: Double,
    val mimeType: String,
    val filename: String,
    val size: Long,
    val localPath: String?,
    val downloadStatus: DownloadStatus
)
