package dev.vikingsen.skald.feature.library

import dev.vikingsen.skald.core.model.Playlist
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddToPlaylistViewModelTest {

    private val repository = mockk<AudiobookshelfRepository>(relaxed = true)
    private val bookMenuActionUtil = mockk<BookMenuActionUtil>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private val testPlaylists = listOf(
        Playlist(
            id = "playlist-1",
            name = "My Playlist 1",
            description = "Desc 1",
            duration = 100.0,
            itemCount = 0,
            items = emptyList(),
            lastUpdated = 0L
        ),
        Playlist(
            id = "playlist-2",
            name = "My Playlist 2",
            description = "Desc 2",
            duration = 200.0,
            itemCount = 0,
            items = emptyList(),
            lastUpdated = 0L
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getPlaylistsFlow() } returns flowOf(testPlaylists)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun testInit_syncsPlaylistsAndLoadsPlaylists() = runTest(testDispatcher) {
        val viewModel = AddToPlaylistViewModel(repository, bookMenuActionUtil)
        
        // Let init job run
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.syncPlaylists() }
        
        val playlistsJob = launch { viewModel.playlists.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(testPlaylists, viewModel.playlists.value)
        playlistsJob.cancel()
    }

    @Test
    fun testAddToPlaylist_success() = runTest(testDispatcher) {
        val viewModel = AddToPlaylistViewModel(repository, bookMenuActionUtil)
        coEvery { bookMenuActionUtil.addToPlaylist("playlist-1", "book-123") } returns Result.success(Unit)

        var result: Result<Unit>? = null
        viewModel.addToPlaylist("playlist-1", "book-123") {
            result = it
        }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.addToPlaylist("playlist-1", "book-123") }
        assertEquals(Result.success(Unit), result)
    }

    @Test
    fun testCreatePlaylistAndAdd_success() = runTest(testDispatcher) {
        val viewModel = AddToPlaylistViewModel(repository, bookMenuActionUtil)
        coEvery {
            bookMenuActionUtil.createPlaylistWithBook("New Playlist", "lib-1", "book-123")
        } returns Result.success(Unit)

        var result: Result<Unit>? = null
        viewModel.createPlaylistAndAdd("New Playlist", "lib-1", "book-123") {
            result = it
        }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.createPlaylistWithBook("New Playlist", "lib-1", "book-123") }
        assertEquals(Result.success(Unit), result)
    }
}
