package dev.vikingsen.skald.feature.settings

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dev.vikingsen.skald.core.model.Library
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.LogoutUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val logoutUseCase = mockk<LogoutUseCase>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val audiobookshelfRepository = mockk<AudiobookshelfRepository>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private val connectivityManager = mockk<ConnectivityManager>()
    private val networkCapabilities = mockk<NetworkCapabilities>()

    private lateinit var viewModel: SettingsViewModel
    private lateinit var tempDir: File
    private lateinit var tempDbFile: File

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Setup mock connectivity
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns mockk()
        every { connectivityManager.getNetworkCapabilities(any()) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        // Setup mock temporary storage paths
        tempDir = Files.createTempDirectory("skald_test").toFile()
        tempDbFile = File(tempDir, "skald_db")
        every { context.getExternalFilesDir(null) } returns tempDir
        every { context.getDatabasePath("skald_db") } returns tempDbFile

        // Setup mock settings returns
        every { settingsRepository.getServerUrl() } returns "https://abs.example.com"
        every { settingsRepository.getUsername() } returns "test_user"
        every { settingsRepository.getSkipForwardDuration() } returns 30
        every { settingsRepository.getSkipBackwardDuration() } returns 10
        every { settingsRepository.getPlaybackSpeed() } returns 1.25f
        every { settingsRepository.getGoBackOnInterrupt() } returns true
        every { settingsRepository.getLibrarySyncIntervalHours() } returns 12
        every { settingsRepository.getLibraryLastSyncTimestamp() } returns 5000L
        every { settingsRepository.getLibraryId() } returns "lib_123"
        coEvery { settingsRepository.getCachedLibraries() } returns listOf(
            Library("lib_123", "Audiobooks", "book")
        )

        viewModel = SettingsViewModel(
            logoutUseCase = logoutUseCase,
            settingsRepository = settingsRepository,
            audiobookshelfRepository = audiobookshelfRepository,
            context = context,
            ioDispatcher = testDispatcher
        )

        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
        clearAllMocks()
    }

    @Test
    fun testInitialLoad() = runTest(testDispatcher) {
        assertEquals("https://abs.example.com", viewModel.serverUrl.value)
        assertEquals("test_user", viewModel.username.value)
        assertEquals("Audiobooks", viewModel.activeLibraryName.value)
        assertEquals(30, viewModel.skipForwardDuration.value)
        assertEquals(10, viewModel.skipBackwardDuration.value)
        assertEquals(1.25f, viewModel.playbackSpeed.value)
        assertEquals(true, viewModel.goBackOnInterrupt.value)
        assertEquals(12, viewModel.syncIntervalHours.value)
        assertEquals(5000L, viewModel.lastSyncTimestamp.value)
    }

    @Test
    fun testUpdateSkipForwardDuration() = runTest(testDispatcher) {
        viewModel.updateSkipForwardDuration(45)
        assertEquals(45, viewModel.skipForwardDuration.value)
        verify { settingsRepository.saveSkipForwardDuration(45) }
    }

    @Test
    fun testUpdateSkipBackwardDuration() = runTest(testDispatcher) {
        viewModel.updateSkipBackwardDuration(15)
        assertEquals(15, viewModel.skipBackwardDuration.value)
        verify { settingsRepository.saveSkipBackwardDuration(15) }
    }

    @Test
    fun testUpdatePlaybackSpeed() = runTest(testDispatcher) {
        viewModel.updatePlaybackSpeed(2.0f)
        assertEquals(2.0f, viewModel.playbackSpeed.value)
        verify { settingsRepository.savePlaybackSpeed(2.0f) }
    }

    @Test
    fun testUpdateGoBackOnInterrupt() = runTest(testDispatcher) {
        viewModel.updateGoBackOnInterrupt(false)
        assertEquals(false, viewModel.goBackOnInterrupt.value)
        verify { settingsRepository.saveGoBackOnInterrupt(false) }
    }

    @Test
    fun testUpdateSyncInterval() = runTest(testDispatcher) {
        viewModel.updateSyncInterval(6)
        assertEquals(6, viewModel.syncIntervalHours.value)
        verify { settingsRepository.saveLibrarySyncIntervalHours(6) }
    }

    @Test
    fun testSyncNow_success() = runTest(testDispatcher) {
        viewModel.syncNow()
        
        // Advance time to run coroutine
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { audiobookshelfRepository.syncLibraryBooks("lib_123", forceRefresh = true) }
        coVerify { audiobookshelfRepository.syncGlobalProgress(forceRefresh = true) }
    }

    @Test
    fun testClearCache() = runTest(testDispatcher) {
        // Create mock downloaded file
        val downloadsFolder = File(tempDir, "downloads")
        downloadsFolder.mkdirs()
        val dummyFile = File(downloadsFolder, "test_track.mp3")
        dummyFile.writeText("audio data")

        assertTrue(dummyFile.exists())

        viewModel.clearCache()
        
        // Advance time to run IO operations
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify downloads were deleted
        assertTrue(!dummyFile.exists())
        assertTrue(!downloadsFolder.exists())

        // Verify database was cleared
        coVerify { audiobookshelfRepository.clearLocalData() }
    }

    @Test
    fun testLogout() = runTest(testDispatcher) {
        var completed = false
        viewModel.logout { completed = true }

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(completed)
        coVerify { logoutUseCase.invoke() }
    }
}
