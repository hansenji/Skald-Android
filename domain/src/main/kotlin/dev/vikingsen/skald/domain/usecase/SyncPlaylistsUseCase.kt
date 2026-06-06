package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository

class SyncPlaylistsUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<Unit> =
        repository.syncPlaylists(forceRefresh)
}
