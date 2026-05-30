package dev.vikingsen.absclientapp.domain.usecase

import androidx.paging.PagingData
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.model.BookWithProgress
import dev.vikingsen.absclientapp.core.model.ReadStatusFilter
import dev.vikingsen.absclientapp.core.model.SortOption
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetBooksUseCase(private val repository: AudiobookshelfRepository) {
    operator fun invoke(): Flow<List<Book>> = repository.getBooksFlow()

    operator fun invoke(
        libraryId: String,
        query: String,
        filter: ReadStatusFilter,
        downloadedOnly: Boolean,
        sortBy: SortOption
    ): Flow<PagingData<BookWithProgress>> = repository.getBooksPaged(libraryId, query, filter, downloadedOnly, sortBy)
}
