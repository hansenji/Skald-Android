package com.example.absclientapp.domain.usecase

import com.example.absclientapp.domain.model.Book
import com.example.absclientapp.domain.repository.AudiobookshelfRepository

class FetchBookDetailsUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(bookId: String): Result<Book> = repository.fetchBookDetails(bookId)
}
