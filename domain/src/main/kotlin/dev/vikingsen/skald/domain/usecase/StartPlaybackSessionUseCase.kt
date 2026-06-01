package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository

class StartPlaybackSessionUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(bookId: String, deviceId: String, deviceName: String): Result<String> {
        return repository.startPlaybackSession(bookId, deviceId, deviceName)
    }
}
