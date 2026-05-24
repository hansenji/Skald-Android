package com.example.absclientapp.domain.usecase

import com.example.absclientapp.domain.model.PlaybackProgress
import com.example.absclientapp.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetPlaybackProgressUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(): Flow<List<PlaybackProgress>> = repository.getAllProgressFlow()
}
