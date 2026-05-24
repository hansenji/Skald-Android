package com.example.absclientapp.domain.usecase

import com.example.absclientapp.domain.model.Book
import com.example.absclientapp.domain.model.PlaybackProgress
import com.example.absclientapp.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetBookWithProgressUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(bookId: String): Flow<Pair<Book?, PlaybackProgress?>> {
        return repository.getBookWithProgressFlow(bookId)
    }
}
