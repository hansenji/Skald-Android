package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.Playlist
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetPlaylistsUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(): Flow<List<Playlist>> = repository.getPlaylistsFlow()
}
