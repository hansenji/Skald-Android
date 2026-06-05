package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.Series
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetSeriesUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(libraryId: String): Flow<List<Series>> = repository.getSeriesFlow(libraryId)
}
