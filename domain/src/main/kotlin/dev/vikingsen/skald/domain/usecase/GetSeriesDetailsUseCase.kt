package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.Series
import dev.vikingsen.skald.core.model.BookWithProgress
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetSeriesDetailsUseCase(private val repository: AudiobookshelfRepository) {
    suspend fun getSeries(seriesId: String): Series? = repository.getSeriesById(seriesId)
    fun getBooks(seriesId: String): Flow<List<BookWithProgress>> = repository.getBooksForSeriesFlow(seriesId)
}
