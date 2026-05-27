package dev.vikingsen.absclientapp.core.player

import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.flow.flowOf
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.model.Chapter
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository
import dev.vikingsen.absclientapp.domain.usecase.GetBooksUseCase
import dev.vikingsen.absclientapp.domain.usecase.GetPlaybackProgressUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudiobookSessionCallbackTest {

    private val context = mockk<Context>(relaxed = true)

    private val playerManager = mockk<PlayerManager>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val getBooksUseCase = mockk<GetBooksUseCase>(relaxed = true)
    private val getPlaybackProgressUseCase = mockk<GetPlaybackProgressUseCase>(relaxed = true)

    private lateinit var callback: AudiobookSessionCallback
    private val mockSession = mockk<MediaSession>(relaxed = true)
    private val mockController = mockk<MediaSession.ControllerInfo>(relaxed = true)
    private val mockBundle = mockk<Bundle>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        callback = AudiobookSessionCallback(
            context = context,
            playerManager = playerManager,
            settingsRepository = settingsRepository,
            getBooksUseCase = getBooksUseCase,
            getPlaybackProgressUseCase = getPlaybackProgressUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun testSkipForward_normal() {
        every { settingsRepository.getSkipForwardDuration() } returns 30
        every { playerManager.currentPosition } returns MutableStateFlow(100.0)
        every { playerManager.duration } returns MutableStateFlow(200.0)

        val cmd = SessionCommand(AudiobookSessionCallback.COMMAND_SKIP_FORWARD, mockBundle)
        val result = callback.onCustomCommand(mockSession, mockController, cmd, mockBundle).get()

        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
        verify { playerManager.seekTo(130.0) }
        verify(exactly = 0) { playerManager.pause() }
    }

    @Test
    fun testSkipForward_exceedsDuration() {
        every { settingsRepository.getSkipForwardDuration() } returns 30
        every { playerManager.currentPosition } returns MutableStateFlow(180.0)
        every { playerManager.duration } returns MutableStateFlow(200.0)

        val cmd = SessionCommand(AudiobookSessionCallback.COMMAND_SKIP_FORWARD, mockBundle)
        val result = callback.onCustomCommand(mockSession, mockController, cmd, mockBundle).get()

        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
        verify { playerManager.seekTo(200.0) }
        verify { playerManager.pause() }
    }

    @Test
    fun testSkipBackward() {
        every { settingsRepository.getSkipBackwardDuration() } returns 10
        every { playerManager.currentPosition } returns MutableStateFlow(25.0)

        val cmd = SessionCommand(AudiobookSessionCallback.COMMAND_SKIP_BACKWARD, mockBundle)
        val result = callback.onCustomCommand(mockSession, mockController, cmd, mockBundle).get()

        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
        verify { playerManager.seekTo(15.0) }
    }

    @Test
    fun testCycleSpeed() {
        every { playerManager.playbackSpeed } returns MutableStateFlow(1.0f)

        val cmd = SessionCommand(AudiobookSessionCallback.COMMAND_CYCLE_SPEED, mockBundle)
        val result = callback.onCustomCommand(mockSession, mockController, cmd, mockBundle).get()

        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
        verify { playerManager.setPlaybackSpeed(1.25f) }
        verify { settingsRepository.savePlaybackSpeed(1.25f) }
    }

    @Test
    fun testSkipToNextChapter() {
        val chapters = listOf(
            Chapter(0.0, 50.0, "Chapter 1"),
            Chapter(50.0, 100.0, "Chapter 2")
        )
        val mockBook = mockk<Book>()
        every { mockBook.chapters } returns chapters
        every { playerManager.currentBook } returns MutableStateFlow(mockBook)
        every { playerManager.currentPosition } returns MutableStateFlow(10.0)
        every { playerManager.duration } returns MutableStateFlow(100.0)

        val cmd = SessionCommand(AudiobookSessionCallback.COMMAND_SKIP_TO_NEXT_CHAPTER, mockBundle)
        val result = callback.onCustomCommand(mockSession, mockController, cmd, mockBundle).get()

        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
        verify { playerManager.seekTo(50.0) }
    }

    @Test
    fun testSkipToPreviousChapter_restartChapter() {
        val chapters = listOf(
            Chapter(0.0, 50.0, "Chapter 1"),
            Chapter(50.0, 100.0, "Chapter 2")
        )
        val mockBook = mockk<Book>()
        every { mockBook.chapters } returns chapters
        every { playerManager.currentBook } returns MutableStateFlow(mockBook)
        every { playerManager.currentPosition } returns MutableStateFlow(58.0) // Played 8s

        val cmd = SessionCommand(AudiobookSessionCallback.COMMAND_SKIP_TO_PREVIOUS_CHAPTER, mockBundle)
        val result = callback.onCustomCommand(mockSession, mockController, cmd, mockBundle).get()

        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
        verify { playerManager.seekTo(50.0) }
    }

    @Test
    fun testSkipToPreviousChapter_jumpToPrior() {
        val chapters = listOf(
            Chapter(0.0, 50.0, "Chapter 1"),
            Chapter(50.0, 100.0, "Chapter 2")
        )
        val mockBook = mockk<Book>()
        every { mockBook.chapters } returns chapters
        every { playerManager.currentBook } returns MutableStateFlow(mockBook)
        every { playerManager.currentPosition } returns MutableStateFlow(52.0) // Played 2s

        val cmd = SessionCommand(AudiobookSessionCallback.COMMAND_SKIP_TO_PREVIOUS_CHAPTER, mockBundle)
        val result = callback.onCustomCommand(mockSession, mockController, cmd, mockBundle).get()

        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
        verify { playerManager.seekTo(0.0) }
    }

    @Test
    fun testOnAddMediaItems_playDownloadedBook_success() {
        val book = Book(
            id = "1",
            title = "Alpha Book",
            author = "Author A",
            narrator = "Narrator X",
            description = "Desc",
            duration = 100.0,
            coverPath = "/path/to/1",
            isDownloaded = true,
            audioFiles = emptyList(),
            chapters = emptyList()
        )
        every { getBooksUseCase.invoke() } returns flowOf(listOf(book))
        every { getPlaybackProgressUseCase.invoke() } returns flowOf(emptyList())
        val mediaItem = androidx.media3.common.MediaItem.Builder()
            .setMediaId("1")
            .build()

        callback.onAddMediaItems(mockSession, mockController, listOf(mediaItem)).get()

        verify { playerManager.playBook(book, 0.0) }
    }

    @Test
    fun testOnAddMediaItems_playNonDownloadedBook_fails() {
        val book = Book(
            id = "2",
            title = "Beta Book",
            author = "Author B",
            narrator = "Narrator Y",
            description = "Desc",
            duration = 200.0,
            coverPath = null,
            isDownloaded = false,
            audioFiles = emptyList(),
            chapters = emptyList()
        )
        every { getBooksUseCase.invoke() } returns flowOf(listOf(book))
        every { getPlaybackProgressUseCase.invoke() } returns flowOf(emptyList())
        val mediaItem = androidx.media3.common.MediaItem.Builder()
            .setMediaId("2")
            .build()

        val result = callback.onAddMediaItems(mockSession, mockController, listOf(mediaItem)).get()

        assertEquals(0, result.size)
        verify(exactly = 0) { playerManager.playBook(any(), any()) }
    }

    @Test
    fun testOnAddMediaItems_voiceSearch_resolvesDownloadedOnly() {
        val downloadedBook = Book(
            id = "1",
            title = "Alpha Book",
            author = "Author A",
            narrator = "Narrator X",
            description = "Desc",
            duration = 100.0,
            coverPath = "/path/to/1",
            isDownloaded = true,
            audioFiles = emptyList(),
            chapters = emptyList()
        )
        val nonDownloadedBook = Book(
            id = "2",
            title = "Beta Book",
            author = "Author B",
            narrator = "Narrator Y",
            description = "Desc",
            duration = 200.0,
            coverPath = null,
            isDownloaded = false,
            audioFiles = emptyList(),
            chapters = emptyList()
        )
        every { getBooksUseCase.invoke() } returns flowOf(listOf(downloadedBook, nonDownloadedBook))
        every { getPlaybackProgressUseCase.invoke() } returns flowOf(emptyList())

        val requestMetadata = androidx.media3.common.MediaItem.RequestMetadata.Builder()
            .setSearchQuery("Book")
            .build()
        val mediaItem = androidx.media3.common.MediaItem.Builder()
            .setMediaId("play_from_search")
            .setRequestMetadata(requestMetadata)
            .build()

        callback.onAddMediaItems(mockSession, mockController, listOf(mediaItem)).get()

        verify { playerManager.playBook(downloadedBook, 0.0) }
        verify(exactly = 0) { playerManager.playBook(nonDownloadedBook, any()) }
    }
}
