package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.Author
import dev.vikingsen.skald.core.model.BookWithProgress
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow

class GetAuthorDetailsUseCase(private val repository: AudiobookshelfRepository) {
    suspend fun getAuthor(authorId: String, forceRefresh: Boolean = false): Result<Author> =
        repository.getAuthorDetails(authorId, forceRefresh)

    fun getBooks(authorId: String): Flow<List<BookWithProgress>> =
        repository.getBooksForAuthorFlow(authorId)
}
