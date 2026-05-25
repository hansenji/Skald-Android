package dev.vikingsen.absclientapp.domain.usecase

import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository

class SyncLibraryBooksUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(libraryId: String): Result<Unit> = repository.syncLibraryBooks(libraryId)
}
