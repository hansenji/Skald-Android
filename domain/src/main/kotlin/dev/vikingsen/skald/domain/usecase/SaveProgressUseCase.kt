package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository

class SaveProgressUseCase(private val repository: AudiobookshelfRepository) {
    suspend fun saveLocal(bookId: String, currentTime: Double, totalDuration: Double) {
        repository.saveLocalProgress(bookId, currentTime, totalDuration)
    }

    suspend fun syncPlayback(sessionId: String, timeListened: Double, currentTime: Double): Result<Unit> {
        return repository.syncPlaybackProgress(sessionId, timeListened, currentTime)
    }

    suspend fun syncStatic(bookId: String, currentTime: Double, progress: Float, isFinished: Boolean): Result<Unit> {
        return repository.syncStaticProgress(bookId, currentTime, progress, isFinished)
    }
}
