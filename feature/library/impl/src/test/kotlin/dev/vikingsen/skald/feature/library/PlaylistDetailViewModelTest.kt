package dev.vikingsen.skald.feature.library

import dev.vikingsen.skald.core.model.Book
import dev.vikingsen.skald.core.model.Playlist
import dev.vikingsen.skald.core.model.PlaylistItem
import dev.vikingsen.skald.core.model.PlaybackProgress
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.GetMiniPlayerStateUseCase
import dev.vikingsen.skald.domain.usecase.PlayPlaylistUseCase
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
class PlaylistDetailViewModelTest {

    private val repository = mockk<AudiobookshelfRepository>(relaxed = true)
    private val playPlaylistUseCase = mockk<PlayPlaylistUseCase>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val getMiniPlayerStateUseCase = mockk<GetMiniPlayerStateUseCase>(relaxed = true)
    private val bookMenuActionUtil = mockk<BookMenuActionUtil>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private val testPlaylistItem = PlaylistItem(
        id = "item-123",
        playlistId = "play-123",
        libraryItemId = "book-123",
        sequence = 0,
        title = "Test Track",
        duration = 1800.0,
        coverPath = "/cover.jpg"
    )

    private val testPlaylist = Playlist(
        id = "play-123",
        name = "Test Playlist",
        description = "Test Description",
        duration = 1800.0,
        itemCount = 1,
        items = listOf(testPlaylistItem),
        lastUpdated = 123456789L
    )

    private val testBook = Book(
        id = "book-123",
        libraryId = "lib-456",
        title = "Test Book",
        author = "Test Author",
        narrator = "Test Narrator",
        description = "Test Description",
        duration = 1800.0,
        coverPath = "/cover.jpg",
        isDownloaded = false,
        audioFiles = emptyList(),
        chapters = emptyList()
    )

    private val testProgress = PlaybackProgress(
        bookId = "book-123",
        currentTime = 900.0,
        progress = 0.5f,
        isFinished = false,
        lastUpdated = 123456789L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { settingsRepository.getServerUrl() } returns "https://abs.example.com"
        every { settingsRepository.getToken() } returns "jwt-token-123"
        every { settingsRepository.getLibraryId() } returns "lib-456"
        coEvery { repository.getPlaylistDetails("play-123", any()) } returns Result.success(testPlaylist)
        every { repository.getPlaylistsFlow() } returns flowOf(listOf(testPlaylist))
        every { repository.getBookWithProgressFlow("book-123") } returns flowOf(testBook to testProgress)
        every { getMiniPlayerStateUseCase() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun testSetPlaylistId_loadsPlaylistDetails() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val playlistJob = launch { viewModel.playlist.collect {} }
        val itemsJob = launch { viewModel.playlistItems.collect {} }

        viewModel.setPlaylistId("play-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.getPlaylistDetails("play-123", false) }
        assertEquals(testPlaylist, viewModel.playlist.value)
        assertEquals(1, viewModel.playlistItems.value.size)
        assertEquals("item-123", viewModel.playlistItems.value[0].id)

        playlistJob.cancel()
        itemsJob.cancel()
    }

    @Test
    fun testActiveBookDetail_loadsWhenSelected() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val activeBookJob = launch { viewModel.activeBookDetail.collect {} }

        viewModel.selectBookForMenu("book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        verify { repository.getBookWithProgressFlow("book-123") }
        val activeBook = viewModel.activeBookDetail.value
        assertEquals("book-123", activeBook?.id)
        assertEquals("Test Book", activeBook?.title)
        assertEquals("Test Author", activeBook?.author)
        assertEquals(0.5f, activeBook?.progress?.progress)

        activeBookJob.cancel()
    }

    @Test
    fun testToggleFinished() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.toggleFinished("book-123", false)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.toggleFinished("book-123", false) }
    }

    @Test
    fun testDiscardProgress() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.discardProgress("book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.discardProgress("book-123") }
    }

    @Test
    fun testDeleteDownloadedBook() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.deleteDownloadedBook("book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.deleteDownload("book-123") }
    }

    @Test
    fun testAddToPlaylist() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.addToPlaylist("another-play", "book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.addToPlaylist("another-play", "book-123") }
    }

    @Test
    fun testCreatePlaylistAndAdd() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.createPlaylistAndAdd("New Playlist", "book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.createPlaylistWithBook("New Playlist", "lib-456", "book-123") }
    }

    @Test
    fun testRemoveFromPlaylist() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val playlistJob = launch { viewModel.playlist.collect {} }
        val itemsJob = launch { viewModel.playlistItems.collect {} }

        viewModel.setPlaylistId("play-123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify loaded first
        assertEquals(1, viewModel.playlistItems.value.size)

        viewModel.removeFromPlaylist("book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Optimistically cleared from local list
        assertEquals(0, viewModel.playlistItems.value.size)

        coVerify { repository.removePlaylistItem("play-123", "book-123") }

        playlistJob.cancel()
        itemsJob.cancel()
    }

    private fun createViewModel() = PlaylistDetailViewModel(
        repository = repository,
        playPlaylistUseCase = playPlaylistUseCase,
        settingsRepository = settingsRepository,
        getMiniPlayerStateUseCase = getMiniPlayerStateUseCase,
        bookMenuActionUtil = bookMenuActionUtil
    )
}
