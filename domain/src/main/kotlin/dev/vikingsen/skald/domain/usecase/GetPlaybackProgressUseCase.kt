package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.PlaybackProgress
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetPlaybackProgressUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(): Flow<List<PlaybackProgress>> = repository.getAllProgressFlow()
}
