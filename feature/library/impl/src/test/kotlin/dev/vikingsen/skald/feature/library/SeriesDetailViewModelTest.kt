package dev.vikingsen.skald.feature.library

import dev.vikingsen.skald.core.model.Book
import dev.vikingsen.skald.core.model.BookWithProgress
import dev.vikingsen.skald.core.model.PlaybackProgress
import dev.vikingsen.skald.core.model.Series
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.GetMiniPlayerStateUseCase
import dev.vikingsen.skald.domain.usecase.GetSeriesDetailsUseCase
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
class SeriesDetailViewModelTest {

    private val getSeriesDetailsUseCase = mockk<GetSeriesDetailsUseCase>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val getMiniPlayerStateUseCase = mockk<GetMiniPlayerStateUseCase>(relaxed = true)
    private val repository = mockk<AudiobookshelfRepository>(relaxed = true)
    private val bookMenuActionUtil = mockk<BookMenuActionUtil>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private val testSeries = Series(
        id = "series-123",
        libraryId = "lib-456",
        name = "Test Series",
        description = "Test Description",
        bookCount = 5
    )

    private val testBook = Book(
        id = "book-123",
        libraryId = "lib-456",
        title = "Test Book",
        author = "Test Author",
        narrator = "Test Narrator",
        description = "Test Description",
        duration = 3600.0,
        coverPath = "/cover.jpg",
        isDownloaded = false,
        audioFiles = emptyList(),
        chapters = emptyList()
    )

    private val testProgress = PlaybackProgress(
        bookId = "book-123",
        currentTime = 1800.0,
        progress = 0.5f,
        isFinished = false,
        lastUpdated = 123456789L
    )

    private val testBookWithProgress = BookWithProgress(
        book = testBook,
        progress = testProgress
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { settingsRepository.getServerUrl() } returns "https://abs.example.com"
        every { settingsRepository.getToken() } returns "jwt-token-123"
        every { settingsRepository.getLibraryId() } returns "lib-456"
        coEvery { getSeriesDetailsUseCase.getSeries("series-123") } returns testSeries
        every { getSeriesDetailsUseCase.getBooks("series-123") } returns flowOf(listOf(testBookWithProgress))
        every { repository.getPlaylistsFlow() } returns flowOf(emptyList())
        every { getMiniPlayerStateUseCase() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun testSetSeriesId_loadsSeriesAndBooks() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val seriesJob = launch { viewModel.series.collect {} }
        val booksJob = launch { viewModel.books.collect {} }

        viewModel.setSeriesId("series-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { getSeriesDetailsUseCase.getSeries("series-123") }
        coVerify { getSeriesDetailsUseCase.getBooks("series-123") }
        assertEquals(testSeries, viewModel.series.value)
        assertEquals(1, viewModel.books.value.size)
        assertEquals("book-123", viewModel.books.value[0].id)

        seriesJob.cancel()
        booksJob.cancel()
    }

    @Test
    fun testToggleFinished_whenUnfinished_marksFinished() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val booksJob = launch { viewModel.books.collect {} }
        viewModel.setSeriesId("series-123")
        testDispatcher.scheduler.advanceUntilIdle()

        val bookCard = viewModel.books.value[0]
        viewModel.toggleFinished(bookCard)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.toggleFinished("book-123", false) }
        booksJob.cancel()
    }

    @Test
    fun testToggleFinished_whenFinished_marksUnfinished() = runTest(testDispatcher) {
        val finishedBookWithProgress = testBookWithProgress.copy(
            progress = testProgress.copy(isFinished = true)
        )
        every { getSeriesDetailsUseCase.getBooks("series-123") } returns flowOf(listOf(finishedBookWithProgress))

        val viewModel = createViewModel()
        val booksJob = launch { viewModel.books.collect {} }
        viewModel.setSeriesId("series-123")
        testDispatcher.scheduler.advanceUntilIdle()

        val bookCard = viewModel.books.value[0]
        viewModel.toggleFinished(bookCard)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.toggleFinished("book-123", true) }
        booksJob.cancel()
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
        viewModel.addToPlaylist("playlist-789", "book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.addToPlaylist("playlist-789", "book-123") }
    }

    @Test
    fun testCreatePlaylistAndAdd() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val seriesJob = launch { viewModel.series.collect {} }
        viewModel.setSeriesId("series-123")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createPlaylistAndAdd("My Premium Playlist", "book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.createPlaylistWithBook("My Premium Playlist", "lib-456", "book-123") }
        seriesJob.cancel()
    }

    private fun createViewModel() = SeriesDetailViewModel(
        getSeriesDetailsUseCase = getSeriesDetailsUseCase,
        settingsRepository = settingsRepository,
        getMiniPlayerStateUseCase = getMiniPlayerStateUseCase,
        repository = repository,
        bookMenuActionUtil = bookMenuActionUtil
    )
}
