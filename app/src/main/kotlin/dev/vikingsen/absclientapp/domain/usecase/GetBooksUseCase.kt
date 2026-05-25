package dev.vikingsen.absclientapp.domain.usecase

import dev.vikingsen.absclientapp.domain.model.Book
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetBooksUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(): Flow<List<Book>> = repository.getBooksFlow()
}
