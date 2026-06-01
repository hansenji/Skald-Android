package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository

class SyncPersonalizedShelvesUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(libraryId: String, forceRefresh: Boolean = false): Result<Unit> =
        repository.syncHomeShelves(libraryId, forceRefresh)
}
