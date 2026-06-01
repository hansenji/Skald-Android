package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.Book
import dev.vikingsen.skald.core.model.PlaybackProgress
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetBookWithProgressUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(bookId: String): Flow<Pair<Book?, PlaybackProgress?>> {
        return repository.getBookWithProgressFlow(bookId)
    }
}
