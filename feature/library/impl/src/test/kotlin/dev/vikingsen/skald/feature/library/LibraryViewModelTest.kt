package dev.vikingsen.skald.feature.library

import dev.vikingsen.skald.core.model.Playlist
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.*
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
class LibraryViewModelTest {

    private val getBooksUseCase = mockk<GetBooksUseCase>(relaxed = true)
    private val syncLibraryBooksUseCase = mockk<SyncLibraryBooksUseCase>(relaxed = true)
    private val getSeriesUseCase = mockk<GetSeriesUseCase>(relaxed = true)
    private val syncLibrarySeriesUseCase = mockk<SyncLibrarySeriesUseCase>(relaxed = true)
    private val getAuthorsUseCase = mockk<GetAuthorsUseCase>(relaxed = true)
    private val syncLibraryAuthorsUseCase = mockk<SyncLibraryAuthorsUseCase>(relaxed = true)
    private val getCollectionsUseCase = mockk<GetCollectionsUseCase>(relaxed = true)
    private val syncLibraryCollectionsUseCase = mockk<SyncLibraryCollectionsUseCase>(relaxed = true)
    private val repository = mockk<AudiobookshelfRepository>(relaxed = true)
    private val fetchLibrariesUseCase = mockk<FetchLibrariesUseCase>(relaxed = true)
    private val logoutUseCase = mockk<LogoutUseCase>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val getMiniPlayerStateUseCase = mockk<GetMiniPlayerStateUseCase>(relaxed = true)
    private val syncGlobalProgressUseCase = mockk<SyncGlobalProgressUseCase>(relaxed = true)
    private val getPlaylistsUseCase = mockk<GetPlaylistsUseCase>(relaxed = true)
    private val syncPlaylistsUseCase = mockk<SyncPlaylistsUseCase>(relaxed = true)
    private val playPlaylistUseCase = mockk<PlayPlaylistUseCase>(relaxed = true)
    private val bookMenuActionUtil = mockk<BookMenuActionUtil>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private val testBookCard = BookCardUiModel(
        id = "book-123",
        title = "Test Book",
        author = "Test Author",
        narrator = "Test Narrator",
        coverUrl = "/cover.jpg",
        authorizationHeader = "Bearer jwt",
        isDownloaded = true,
        duration = 3600.0,
        progress = PlaybackProgressUiModel(
            progress = 0.5f,
            isFinished = false,
            currentTime = 1800.0,
            lastUpdated = 123456789L
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { settingsRepository.getReadStatusFilter() } returns null
        every { settingsRepository.getSortOption() } returns null
        every { settingsRepository.getDownloadedOnlyFilter() } returns false
        every { settingsRepository.getLibraryId() } returns ""
        every { settingsRepository.getLibrarySyncIntervalHours() } returns 24
        every { settingsRepository.getLibraryLastSyncTimestamp() } returns 0L
        every { settingsRepository.getHideEmptyLibraryTabs() } returns false
        every { settingsRepository.observeHideEmptyLibraryTabs() } returns flowOf(false)
        coEvery { settingsRepository.getCachedLibraries() } returns emptyList()

        every { repository.getSeriesFlow(any()) } returns flowOf(emptyList())
        every { repository.getAuthorsFlow(any()) } returns flowOf(emptyList())
        every { getCollectionsUseCase(any()) } returns flowOf(emptyList())
        every { getPlaylistsUseCase() } returns flowOf(emptyList())
        every { repository.getPlaylistsFlow() } returns flowOf(emptyList())
        every { getMiniPlayerStateUseCase() } returns flowOf(null)

        coEvery { fetchLibrariesUseCase() } returns Result.success(emptyList())
        coEvery { syncLibraryBooksUseCase(any(), any()) } returns Result.success(Unit)
        coEvery { syncLibrarySeriesUseCase(any(), any()) } returns Result.success(Unit)
        coEvery { syncLibraryAuthorsUseCase(any(), any()) } returns Result.success(Unit)
        coEvery { syncLibraryCollectionsUseCase(any(), any()) } returns Result.success(Unit)
        coEvery { syncPlaylistsUseCase(any()) } returns Result.success(Unit)
        coEvery { syncGlobalProgressUseCase(any()) } returns Result.success(Unit)
        coEvery { bookMenuActionUtil.deleteDownload(any()) } returns Result.success(Unit)
        coEvery { bookMenuActionUtil.toggleFinished(any(), any()) } returns Result.success(Unit)
        coEvery { bookMenuActionUtil.discardProgress(any()) } returns Result.success(Unit)
        coEvery { bookMenuActionUtil.addToPlaylist(any(), any()) } returns Result.success(Unit)
        coEvery { bookMenuActionUtil.createPlaylistWithBook(any(), any(), any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun testToggleFinished_whenUnfinished_marksFinished() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleFinished(testBookCard)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.toggleFinished("book-123", false) }
    }

    @Test
    fun testToggleFinished_whenFinished_marksUnfinished() = runTest(testDispatcher) {
        val finishedBook = testBookCard.copy(
            progress = testBookCard.progress?.copy(isFinished = true)
        )
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleFinished(finishedBook)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.toggleFinished("book-123", true) }
    }

    @Test
    fun testDiscardProgress() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.discardProgress("book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.discardProgress("book-123") }
    }

    @Test
    fun testDeleteDownloadedBook() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteDownloadedBook("book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.deleteDownload("book-123") }
    }

    @Test
    fun testAddToPlaylist() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addToPlaylist("playlist-789", "book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.addToPlaylist("playlist-789", "book-123") }
    }

    @Test
    fun testCreatePlaylistAndAdd() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createPlaylistAndAdd("New Playlist", "lib-456", "book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.createPlaylistWithBook("New Playlist", "lib-456", "book-123") }
    }

    private fun createViewModel() = LibraryViewModel(
        getBooksUseCase = getBooksUseCase,
        syncLibraryBooksUseCase = syncLibraryBooksUseCase,
        getSeriesUseCase = getSeriesUseCase,
        syncLibrarySeriesUseCase = syncLibrarySeriesUseCase,
        getAuthorsUseCase = getAuthorsUseCase,
        syncLibraryAuthorsUseCase = syncLibraryAuthorsUseCase,
        getCollectionsUseCase = getCollectionsUseCase,
        syncLibraryCollectionsUseCase = syncLibraryCollectionsUseCase,
        repository = repository,
        fetchLibrariesUseCase = fetchLibrariesUseCase,
        logoutUseCase = logoutUseCase,
        settingsRepository = settingsRepository,
        getMiniPlayerStateUseCase = getMiniPlayerStateUseCase,
        syncGlobalProgressUseCase = syncGlobalProgressUseCase,
        getPlaylistsUseCase = getPlaylistsUseCase,
        syncPlaylistsUseCase = syncPlaylistsUseCase,
        playPlaylistUseCase = playPlaylistUseCase,
        bookMenuActionUtil = bookMenuActionUtil
    )
}
