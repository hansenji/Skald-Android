package dev.vikingsen.skald.feature.library

import dev.vikingsen.skald.core.model.Book
import dev.vikingsen.skald.core.model.PlaybackProgress
import dev.vikingsen.skald.core.model.Playlist
import dev.vikingsen.skald.core.player.PlayerManager
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val getBookWithProgressUseCase = mockk<GetBookWithProgressUseCase>(relaxed = true)
    private val fetchBookDetailsUseCase = mockk<FetchBookDetailsUseCase>(relaxed = true)
    private val downloadAudioFileUseCase = mockk<DownloadAudioFileUseCase>(relaxed = true)
    private val deleteLocalBookFilesUseCase = mockk<DeleteLocalBookFilesUseCase>(relaxed = true)
    private val repository = mockk<AudiobookshelfRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val playerManager = mockk<PlayerManager>(relaxed = true)
    private val getMiniPlayerStateUseCase = mockk<GetMiniPlayerStateUseCase>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { settingsRepository.getServerUrl() } returns "https://abs.example.com"
        every { settingsRepository.getToken() } returns "jwt-token-123"
        every { getBookWithProgressUseCase("book-123") } returns flowOf(testBook to testProgress)
        every { repository.getPlaylistsFlow() } returns flowOf(emptyList())
        every { getMiniPlayerStateUseCase() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun testSetBookId_fetchesDetails() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = launch { viewModel.bookAndProgress.collect {} }
        viewModel.setBookId("book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { fetchBookDetailsUseCase("book-123") }
        assertEquals("book-123", viewModel.bookId.value)
        collectJob.cancel()
    }

    @Test
    fun testToggleFinished_whenUnfinished_marksFinished() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = launch { viewModel.bookAndProgress.collect {} }
        viewModel.setBookId("book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleFinished()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updatePlaybackFinished("book-123", true) }
        collectJob.cancel()
    }

    @Test
    fun testToggleFinished_whenFinished_marksUnfinished() = runTest(testDispatcher) {
        val finishedProgress = testProgress.copy(isFinished = true)
        every { getBookWithProgressUseCase("book-123") } returns flowOf(testBook to finishedProgress)

        val viewModel = createViewModel()
        val collectJob = launch { viewModel.bookAndProgress.collect {} }
        viewModel.setBookId("book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleFinished()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updatePlaybackFinished("book-123", false) }
        collectJob.cancel()
    }

    @Test
    fun testDiscardProgress() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = launch { viewModel.bookAndProgress.collect {} }
        viewModel.setBookId("book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.discardProgress()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.discardProgress("book-123") }
        collectJob.cancel()
    }

    @Test
    fun testAddToPlaylist() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = launch { viewModel.bookAndProgress.collect {} }
        viewModel.setBookId("book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addToPlaylist("playlist-789")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.addBookToPlaylist("playlist-789", "book-123") }
        collectJob.cancel()
    }

    @Test
    fun testCreatePlaylistAndAdd() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = launch { viewModel.bookAndProgress.collect {} }
        viewModel.setBookId("book-123")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createPlaylistAndAdd("My Premium Playlist")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.createPlaylistWithBook("My Premium Playlist", "lib-456", "book-123") }
        collectJob.cancel()
    }


    private fun createViewModel() = DetailViewModel(
        getBookWithProgressUseCase = getBookWithProgressUseCase,
        fetchBookDetailsUseCase = fetchBookDetailsUseCase,
        downloadAudioFileUseCase = downloadAudioFileUseCase,
        deleteLocalBookFilesUseCase = deleteLocalBookFilesUseCase,
        repository = repository,
        settingsRepository = settingsRepository,
        playerManager = playerManager,
        getMiniPlayerStateUseCase = getMiniPlayerStateUseCase
    )
}
