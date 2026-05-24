package com.example.absclientapp.domain.usecase

import com.example.absclientapp.domain.repository.AudiobookshelfRepository

class SyncLibraryBooksUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(libraryId: String): Result<Unit> = repository.syncLibraryBooks(libraryId)
}
