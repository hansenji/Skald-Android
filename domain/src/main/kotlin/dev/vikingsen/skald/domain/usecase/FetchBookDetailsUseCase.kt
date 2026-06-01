package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.Book
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository

class FetchBookDetailsUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(bookId: String, forceRefresh: Boolean = false): Result<Book> = 
        repository.fetchBookDetails(bookId, forceRefresh)
}
