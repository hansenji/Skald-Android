package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository

class SyncLibraryAuthorsUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(libraryId: String, forceRefresh: Boolean = false): Result<Unit> =
        repository.syncLibraryAuthors(libraryId, forceRefresh)
}
