package dev.vikingsen.skald.feature.library

import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.usecase.DeleteLocalBookFilesUseCase

class BookMenuActionUtil(
    private val repository: AudiobookshelfRepository,
    private val deleteLocalBookFilesUseCase: DeleteLocalBookFilesUseCase
) {
    suspend fun toggleFinished(bookId: String, currentFinishedState: Boolean): Result<Unit> {
        return repository.updatePlaybackFinished(bookId, !currentFinishedState)
    }

    suspend fun discardProgress(bookId: String): Result<Unit> {
        return repository.discardProgress(bookId)
    }

    suspend fun deleteDownload(bookId: String): Result<Unit> {
        return deleteLocalBookFilesUseCase(bookId)
    }

    suspend fun addToPlaylist(playlistId: String, bookId: String): Result<Unit> {
        return repository.addBookToPlaylist(playlistId, bookId)
    }

    suspend fun createPlaylistWithBook(name: String, libraryId: String, bookId: String): Result<Unit> {
        return repository.createPlaylistWithBook(name, libraryId, bookId)
    }
}
