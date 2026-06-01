package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository

class SyncLibraryBooksUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(libraryId: String, forceRefresh: Boolean = false): Result<Unit> = 
        repository.syncLibraryBooks(libraryId, forceRefresh)
}
