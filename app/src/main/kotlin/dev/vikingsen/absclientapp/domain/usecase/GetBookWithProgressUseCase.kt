package dev.vikingsen.absclientapp.domain.usecase

import dev.vikingsen.absclientapp.domain.model.Book
import dev.vikingsen.absclientapp.domain.model.PlaybackProgress
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetBookWithProgressUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(bookId: String): Flow<Pair<Book?, PlaybackProgress?>> {
        return repository.getBookWithProgressFlow(bookId)
    }
}
