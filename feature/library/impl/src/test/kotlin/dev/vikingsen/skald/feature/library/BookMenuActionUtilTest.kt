package dev.vikingsen.skald.feature.library

import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.usecase.DeleteLocalBookFilesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookMenuActionUtilTest {

    private val repository = mockk<AudiobookshelfRepository>(relaxed = true)
    private val deleteLocalBookFilesUseCase = mockk<DeleteLocalBookFilesUseCase>(relaxed = true)
    private val util = BookMenuActionUtil(repository, deleteLocalBookFilesUseCase)

    @Test
    fun testToggleFinished_whenUnfinished_marksFinished() = runTest {
        coEvery { repository.updatePlaybackFinished("book-123", true) } returns Result.success(Unit)

        val result = util.toggleFinished("book-123", currentFinishedState = false)

        assertEquals(Result.success(Unit), result)
        coVerify { repository.updatePlaybackFinished("book-123", true) }
    }

    @Test
    fun testToggleFinished_whenFinished_marksUnfinished() = runTest {
        coEvery { repository.updatePlaybackFinished("book-123", false) } returns Result.success(Unit)

        val result = util.toggleFinished("book-123", currentFinishedState = true)

        assertEquals(Result.success(Unit), result)
        coVerify { repository.updatePlaybackFinished("book-123", false) }
    }

    @Test
    fun testDiscardProgress() = runTest {
        coEvery { repository.discardProgress("book-123") } returns Result.success(Unit)

        val result = util.discardProgress("book-123")

        assertEquals(Result.success(Unit), result)
        coVerify { repository.discardProgress("book-123") }
    }

    @Test
    fun testDeleteDownload() = runTest {
        coEvery { deleteLocalBookFilesUseCase("book-123") } returns Result.success(Unit)

        val result = util.deleteDownload("book-123")

        assertEquals(Result.success(Unit), result)
        coVerify { deleteLocalBookFilesUseCase("book-123") }
    }

    @Test
    fun testAddToPlaylist() = runTest {
        coEvery { repository.addBookToPlaylist("playlist-789", "book-123") } returns Result.success(Unit)

        val result = util.addToPlaylist("playlist-789", "book-123")

        assertEquals(Result.success(Unit), result)
        coVerify { repository.addBookToPlaylist("playlist-789", "book-123") }
    }

    @Test
    fun testCreatePlaylistWithBook() = runTest {
        coEvery { repository.createPlaylistWithBook("New Playlist", "lib-456", "book-123") } returns Result.success(Unit)

        val result = util.createPlaylistWithBook("New Playlist", "lib-456", "book-123")

        assertEquals(Result.success(Unit), result)
        coVerify { repository.createPlaylistWithBook("New Playlist", "lib-456", "book-123") }
    }
}
