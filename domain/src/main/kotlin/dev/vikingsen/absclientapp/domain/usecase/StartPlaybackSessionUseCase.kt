package dev.vikingsen.absclientapp.domain.usecase

import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository

class StartPlaybackSessionUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(bookId: String, deviceId: String, deviceName: String): Result<String> {
        return repository.startPlaybackSession(bookId, deviceId, deviceName)
    }
}
