package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.Author
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetAuthorsUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(libraryId: String): Flow<List<Author>> = repository.getAuthorsFlow(libraryId)
}
