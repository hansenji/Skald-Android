package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.BookCollection
import dev.vikingsen.skald.core.model.BookWithProgress
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetCollectionDetailsUseCase(private val repository: AudiobookshelfRepository) {
    suspend fun getCollection(collectionId: String, forceRefresh: Boolean = false): Result<BookCollection> =
        repository.getCollectionDetails(collectionId, forceRefresh)

    fun getBooks(collectionId: String): Flow<List<BookWithProgress>> =
        repository.getBooksForCollectionFlow(collectionId)
}
