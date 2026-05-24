package com.example.absclientapp.domain.usecase

import com.example.absclientapp.domain.repository.AudiobookshelfRepository

class DeleteLocalBookFilesUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(bookId: String): Result<Unit> {
        return repository.deleteLocalBookFiles(bookId)
    }
}
