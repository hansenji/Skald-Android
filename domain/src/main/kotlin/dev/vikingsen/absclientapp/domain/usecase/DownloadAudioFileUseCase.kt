package dev.vikingsen.absclientapp.domain.usecase

import dev.vikingsen.absclientapp.core.model.AudioFile
import dev.vikingsen.absclientapp.core.model.DownloadStatusState
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class DownloadAudioFileUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(bookId: String): Result<Unit> {
        return repository.enqueueBookDownloads(bookId)
    }
}
