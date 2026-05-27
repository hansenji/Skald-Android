package dev.vikingsen.absclientapp.domain.usecase

import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository

class DeleteLocalBookFilesUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(bookId: String): Result<Unit> {
        return repository.deleteLocalBookFiles(bookId)
    }
}
