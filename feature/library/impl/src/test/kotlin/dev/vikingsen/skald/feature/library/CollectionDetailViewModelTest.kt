package dev.vikingsen.skald.feature.library

import dev.vikingsen.skald.core.model.Book
import dev.vikingsen.skald.core.model.BookCollection
import dev.vikingsen.skald.core.model.BookWithProgress
import dev.vikingsen.skald.core.model.PlaybackProgress
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.GetMiniPlayerStateUseCase
import dev.vikingsen.skald.domain.usecase.GetCollectionDetailsUseCase
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
class CollectionDetailViewModelTest {

    private val getCollectionDetailsUseCase = mockk<GetCollectionDetailsUseCase>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val getMiniPlayerStateUseCase = mockk<GetMiniPlayerStateUseCase>(relaxed = true)
    private val repository = mockk<AudiobookshelfRepository>(relaxed = true)
    private val bookMenuActionUtil = mockk<BookMenuActionUtil>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private val testCollection = BookCollection(
        id = "col-123",
        libraryId = "lib-456",
        name = "Test Collection",
        description = "Test Description",
        bookIds = listOf("book-123"),
        bookCovers = listOf("/cover.jpg"),
        lastUpdated = 123456789L
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
        coEvery { getCollectionDetailsUseCase.getCollection("col-123", any()) } returns Result.success(testCollection)
        every { getCollectionDetailsUseCase.getBooks("col-123") } returns flowOf(listOf(testBookWithProgress))
        every { repository.getPlaylistsFlow() } returns flowOf(emptyList())
        every { getMiniPlayerStateUseCase() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun testSetCollectionId_loadsCollectionAndBooks() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectionJob = launch { viewModel.collection.collect {} }
        val booksJob = launch { viewModel.books.collect {} }

        viewModel.setCollectionId("col-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { getCollectionDetailsUseCase.getCollection("col-123", true) }
        coVerify { getCollectionDetailsUseCase.getBooks("col-123") }
        assertEquals(testCollection, viewModel.collection.value)
        assertEquals(1, viewModel.books.value.size)
        assertEquals("book-123", viewModel.books.value[0].id)

        collectionJob.cancel()
        booksJob.cancel()
    }

    @Test
    fun testToggleFinished_whenUnfinished_marksFinished() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val booksJob = launch { viewModel.books.collect {} }
        viewModel.setCollectionId("col-123")
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
        every { getCollectionDetailsUseCase.getBooks("col-123") } returns flowOf(listOf(finishedBookWithProgress))

        val viewModel = createViewModel()
        val booksJob = launch { viewModel.books.collect {} }
        viewModel.setCollectionId("col-123")
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
        val collectionJob = launch { viewModel.collection.collect {} }
        viewModel.setCollectionId("col-123")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createPlaylistAndAdd("My Premium Playlist", "book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bookMenuActionUtil.createPlaylistWithBook("My Premium Playlist", "lib-456", "book-123") }
        collectionJob.cancel()
    }

    private fun createViewModel() = CollectionDetailViewModel(
        getCollectionDetailsUseCase = getCollectionDetailsUseCase,
        settingsRepository = settingsRepository,
        getMiniPlayerStateUseCase = getMiniPlayerStateUseCase,
        repository = repository,
        bookMenuActionUtil = bookMenuActionUtil
    )
}
