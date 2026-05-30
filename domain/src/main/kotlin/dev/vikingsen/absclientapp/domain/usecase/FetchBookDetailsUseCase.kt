package dev.vikingsen.absclientapp.domain.usecase

import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository

class FetchBookDetailsUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(bookId: String, forceRefresh: Boolean = false): Result<Book> = 
        repository.fetchBookDetails(bookId, forceRefresh)
}
