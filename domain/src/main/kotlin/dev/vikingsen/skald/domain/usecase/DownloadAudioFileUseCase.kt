package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.AudioFile
import dev.vikingsen.skald.core.model.DownloadStatusState
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class DownloadAudioFileUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(bookId: String): Result<Unit> {
        return repository.enqueueBookDownloads(bookId)
    }
}
