package dev.vikingsen.skald.feature.assistant.mcp

import dev.vikingsen.skald.core.model.Book
import dev.vikingsen.skald.core.player.PlayerManager
import dev.vikingsen.skald.domain.fakes.FakeAudiobookshelfRepository
import dev.vikingsen.skald.domain.fakes.FakeSettingsRepository
import dev.vikingsen.skald.domain.usecase.DownloadAudioFileUseCase
import dev.vikingsen.skald.domain.usecase.FetchBookDetailsUseCase
import dev.vikingsen.skald.domain.usecase.GetPlaylistsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SkaldMcpToolRegistryTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var playerManager: PlayerManager
    private lateinit var repository: FakeAudiobookshelfRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var fetchBookDetailsUseCase: FetchBookDetailsUseCase
    private lateinit var downloadAudioFileUseCase: DownloadAudioFileUseCase
    private lateinit var getPlaylistsUseCase: GetPlaylistsUseCase

    private lateinit var registry: SkaldMcpToolRegistry

    private val testBook = Book(
        id = "book-42",
        libraryId = "lib-1",
        title = "Dune",
        author = "Frank Herbert",
        narrator = "George Guidall",
        description = "Desert planet",
        duration = 7200.0,
        coverPath = null,
        isDownloaded = false,
        audioFiles = emptyList(),
        chapters = emptyList()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        playerManager = mockk(relaxed = true)
        repository = FakeAudiobookshelfRepository()
        settingsRepository = FakeSettingsRepository()
        fetchBookDetailsUseCase = mockk(relaxed = true)
        downloadAudioFileUseCase = mockk(relaxed = true)
        getPlaylistsUseCase = mockk(relaxed = true)

        settingsRepository.fakeUrl = "http://localhost:8080"
        settingsRepository.fakeToken = "test-token"
        settingsRepository.fakeLibraryId = "lib-1"

        every { playerManager.currentBook } returns MutableStateFlow(testBook)
        every { playerManager.isPlaying } returns MutableStateFlow(true)
        every { playerManager.currentPosition } returns MutableStateFlow(120.0)
        every { playerManager.playbackSpeed } returns MutableStateFlow(1.0f)

        coEvery { fetchBookDetailsUseCase(any()) } returns Result.success(testBook)
        coEvery { downloadAudioFileUseCase(any()) } returns Result.success(Unit)
        coEvery { getPlaylistsUseCase() } returns flowOf(emptyList())

        registry = SkaldMcpToolRegistry(
            playerManager = playerManager,
            repository = repository,
            settingsRepository = settingsRepository,
            fetchBookDetailsUseCase = fetchBookDetailsUseCase,
            downloadAudioFileUseCase = downloadAudioFileUseCase,
            getPlaylistsUseCase = getPlaylistsUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `playback tools plan immediately without confirmation`() = runTest {
        val resumePlan = registry.planExecution(SkaldMcpToolRegistry.TOOL_RESUME_PLAYBACK)
        assertTrue(resumePlan is ToolExecutionPlan.Immediate)

        val pausePlan = registry.planExecution(SkaldMcpToolRegistry.TOOL_PAUSE_PLAYBACK)
        assertTrue(pausePlan is ToolExecutionPlan.Immediate)

        val playPlan = registry.planExecution(
            SkaldMcpToolRegistry.TOOL_PLAY_AUDIOBOOK,
            mapOf("bookId" to "book-42")
        )
        assertTrue(playPlan is ToolExecutionPlan.Immediate)
    }

    @Test
    fun `download tools require confirmation`() = runTest {
        val downloadPlan = registry.planExecution(
            SkaldMcpToolRegistry.TOOL_DOWNLOAD_AUDIOBOOK,
            mapOf("bookId" to "book-42")
        )
        assertTrue(downloadPlan is ToolExecutionPlan.RequiresConfirmation)
        val confirmation = downloadPlan as ToolExecutionPlan.RequiresConfirmation
        assertEquals("Download Audiobook", confirmation.promptTitle)
        assertEquals("book-42", confirmation.targetId)
    }

    @Test
    fun `immediate execution of pause playback calls playerManager pause`() = runTest {
        val result = registry.executeTool(SkaldMcpToolRegistry.TOOL_PAUSE_PLAYBACK)
        assertTrue(result is McpToolResult.Success)
        verify { playerManager.pause() }
    }

    @Test
    fun `confirmed download execution invokes downloadAudioFileUseCase`() = runTest {
        val result = registry.executeTool(
            SkaldMcpToolRegistry.TOOL_DOWNLOAD_AUDIOBOOK,
            mapOf("bookId" to "book-42")
        )
        assertTrue(result is McpToolResult.Success)
        coVerify { downloadAudioFileUseCase("book-42") }
    }

    @Test
    fun `set sleep timer executes immediately`() = runTest {
        val plan = registry.planExecution(
            SkaldMcpToolRegistry.TOOL_SET_SLEEP_TIMER,
            mapOf("minutes" to 30)
        )
        assertTrue(plan is ToolExecutionPlan.Immediate)

        val result = registry.executeTool(
            SkaldMcpToolRegistry.TOOL_SET_SLEEP_TIMER,
            mapOf("minutes" to 30)
        )
        assertTrue(result is McpToolResult.Success)
        verify { playerManager.setSleepTimer(30) }
    }

    @Test
    fun `search and play matches book and invokes playerManager playBook`() = runTest {
        val bwp = dev.vikingsen.skald.core.model.BookWithProgress(
            book = testBook,
            progress = null
        )
        repository.booksWithProgressFlow = kotlinx.coroutines.flow.flowOf(listOf(bwp))

        val result = registry.executeTool(
            SkaldMcpToolRegistry.TOOL_SEARCH_AND_PLAY,
            mapOf("query" to "Dune")
        )

        assertTrue(result is McpToolResult.Success)
        assertEquals("Found and playing \"Dune\" by Frank Herbert", (result as McpToolResult.Success).message)
        verify { playerManager.playBook(testBook, 0.0) }
    }

    @Test
    fun `get library summary returns count of books`() = runTest {
        val bwp = dev.vikingsen.skald.core.model.BookWithProgress(
            book = testBook,
            progress = null
        )
        repository.booksWithProgressFlow = kotlinx.coroutines.flow.flowOf(listOf(bwp))

        val result = registry.executeTool(SkaldMcpToolRegistry.TOOL_GET_LIBRARY_SUMMARY)
        assertTrue(result is McpToolResult.Success)
        assertTrue((result as McpToolResult.Success).message.contains("You have 1 books in your library"))
    }
}
