package com.example.absclientapp.domain.usecase

import com.example.absclientapp.domain.model.AudioFile
import com.example.absclientapp.domain.model.DownloadStatusState
import com.example.absclientapp.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class DownloadAudioFileUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(bookId: String): Result<Unit> {
        return repository.enqueueBookDownloads(bookId)
    }
}
