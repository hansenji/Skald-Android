package dev.vikingsen.skald.domain.repository

import dev.vikingsen.skald.core.model.Book
import kotlinx.coroutines.flow.Flow

interface PlaybackStateProvider {
    val currentBook: Flow<Book?>
    val isPlaying: Flow<Boolean>
    val currentPosition: Flow<Double>
    val duration: Flow<Double>
    val isLoading: Flow<Boolean>
}
