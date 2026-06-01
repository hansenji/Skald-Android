package dev.vikingsen.skald.domain.usecase

import androidx.paging.PagingData
import dev.vikingsen.skald.core.model.Book
import dev.vikingsen.skald.core.model.BookWithProgress
import dev.vikingsen.skald.core.model.ReadStatusFilter
import dev.vikingsen.skald.core.model.SortOption
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
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
