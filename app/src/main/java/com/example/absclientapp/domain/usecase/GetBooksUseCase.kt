package com.example.absclientapp.domain.usecase

import com.example.absclientapp.domain.model.Book
import com.example.absclientapp.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetBooksUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(): Flow<List<Book>> = repository.getBooksFlow()
}
