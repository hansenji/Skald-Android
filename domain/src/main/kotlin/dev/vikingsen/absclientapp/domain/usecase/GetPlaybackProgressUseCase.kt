package dev.vikingsen.absclientapp.domain.usecase

import dev.vikingsen.absclientapp.core.model.PlaybackProgress
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetPlaybackProgressUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(): Flow<List<PlaybackProgress>> = repository.getAllProgressFlow()
}
