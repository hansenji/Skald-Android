package com.example.absclientapp.domain.usecase

import com.example.absclientapp.domain.repository.AudiobookshelfRepository

class StartPlaybackSessionUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(bookId: String, deviceId: String, deviceName: String): Result<String> {
        return repository.startPlaybackSession(bookId, deviceId, deviceName)
    }
}
