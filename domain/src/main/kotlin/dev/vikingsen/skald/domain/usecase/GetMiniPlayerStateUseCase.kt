package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.MiniPlayerState
import dev.vikingsen.skald.domain.repository.PlaybackStateProvider
import dev.vikingsen.skald.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetMiniPlayerStateUseCase(
    private val playbackStateProvider: PlaybackStateProvider,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<MiniPlayerState?> {
        return combine(
            playbackStateProvider.currentBook,
            playbackStateProvider.isPlaying,
            playbackStateProvider.currentPosition,
            playbackStateProvider.duration,
            playbackStateProvider.isLoading
        ) { book, isPlaying, position, duration, isLoading ->
            if (book == null) null
            else {
                val serverUrl = settingsRepository.getServerUrl() ?: ""
                val token = settingsRepository.getToken() ?: ""
                val coverUrl = if (!book.coverPath.isNullOrEmpty()) book.coverPath!!
                               else "${serverUrl.trimEnd('/')}/api/items/${book.id}/cover"
                val authHeader = if (!book.coverPath.isNullOrEmpty()) null
                                 else "Bearer $token"
                val progress = if (duration > 0.0) (position / duration).toFloat() else 0f

                MiniPlayerState(
                    bookId = book.id,
                    title = book.title,
                    author = book.author,
                    coverUrl = coverUrl,
                    authorizationHeader = authHeader,
                    isPlaying = isPlaying,
                    progress = progress,
                    isLoading = isLoading
                )
            }
        }
    }
}
