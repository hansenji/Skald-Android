package dev.vikingsen.absclientapp.domain.usecase

import dev.vikingsen.absclientapp.core.model.HomeShelf
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetPersonalizedShelvesUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(libraryId: String): Flow<List<HomeShelf>> = repository.getHomeShelvesFlow(libraryId)
}
