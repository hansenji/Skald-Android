package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.HomeShelf
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetPersonalizedShelvesUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(libraryId: String): Flow<List<HomeShelf>> = repository.getHomeShelvesFlow(libraryId)
}
