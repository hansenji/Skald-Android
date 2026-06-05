package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.BookCollection
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetCollectionsUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(libraryId: String): Flow<List<BookCollection>> = repository.getCollectionsFlow(libraryId)
}
