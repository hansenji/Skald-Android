package dev.vikingsen.absclientapp.feature.androidauto

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.model.PlaybackProgress
import dev.vikingsen.absclientapp.core.player.AudiobookSessionCallback
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository
import dev.vikingsen.absclientapp.domain.usecase.GetBooksUseCase
import dev.vikingsen.absclientapp.domain.usecase.GetPlaybackProgressUseCase
import dev.vikingsen.absclientapp.feature.androidauto.R
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidAutoBrowseCallbackTest {

    private val context = mockk<Context>(relaxed = true)
    private val cm = mockk<ConnectivityManager>(relaxed = true)
    private val netCapabilities = mockk<NetworkCapabilities>(relaxed = true)
    
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val getBooksUseCase = mockk<GetBooksUseCase>(relaxed = true)
    private val getPlaybackProgressUseCase = mockk<GetPlaybackProgressUseCase>(relaxed = true)
    private val coreCallback = mockk<AudiobookSessionCallback>(relaxed = true)

    private lateinit var callback: AndroidAutoBrowseCallback

    private val mockBooks = listOf(
        Book(
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
        ),
        Book(
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
    )

    private val mockProgressList = listOf(
        PlaybackProgress("1", 10.0, 0.1f, false, 1000L),
        PlaybackProgress("2", 20.0, 0.1f, false, 2000L)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        mockkStatic(Uri::class)
        val mockUri = mockk<Uri>(relaxed = true)
        every { Uri.parse(any()) } returns mockUri
        every { Uri.fromFile(any()) } returns mockUri

        every { context.packageName } returns "dev.vikingsen.absclientapp"
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns cm
        every { context.getString(any()) } answers {
            val resId = firstArg<Int>()
            when (resId) {
                R.string.auto_login_required -> "Please log in on your phone"
                R.string.auto_continue_listening -> "Continue Listening"
                R.string.auto_downloads -> "Downloads"
                R.string.auto_all_audiobooks -> "All Audiobooks"
                R.string.auto_by_author -> "By Author"
                R.string.auto_a_z -> "A-Z"
                R.string.auto_root -> "Root"
                else -> ""
            }
        }
        val activeNetwork = mockk<android.net.Network>()
        every { cm.allNetworks } returns arrayOf(activeNetwork)
        every { cm.getNetworkCapabilities(activeNetwork) } returns netCapabilities
        every { netCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        every { settingsRepository.isLoggedIn() } returns true
        every { getBooksUseCase.invoke() } returns flowOf(mockBooks)
        every { getPlaybackProgressUseCase.invoke() } returns flowOf(mockProgressList)

        callback = AndroidAutoBrowseCallback(
            context = context,
            settingsRepository = settingsRepository,
            getBooksUseCase = getBooksUseCase,
            getPlaybackProgressUseCase = getPlaybackProgressUseCase,
            coreCallback = coreCallback
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun testGetLibraryRoot_loggedOut() {
        every { settingsRepository.isLoggedIn() } returns false

        val result = callback.onGetLibraryRoot(mockk(), mockk(), null).get()
        assertEquals("root", result.value?.mediaId)
        assertEquals("Please log in on your phone", result.value?.mediaMetadata?.title?.toString())
    }

    @Test
    fun testGetLibraryRoot_loggedIn() {
        every { settingsRepository.isLoggedIn() } returns true

        val result = callback.onGetLibraryRoot(mockk(), mockk(), null).get()
        assertEquals("root", result.value?.mediaId)
    }

    @Test
    fun testGetChildren_root() {
        val result = callback.onGetChildren(mockk(), mockk(), "root", 0, 10, null).get()
        val list = result.value ?: emptyList()
        assertEquals(3, list.size)
        assertEquals("continue_listening", list[0].mediaId)
        assertEquals("downloads", list[1].mediaId)
        assertEquals("all_audiobooks", list[2].mediaId)
    }

    @Test
    fun testGetChildren_downloads() {
        val result = callback.onGetChildren(mockk(), mockk(), "downloads", 0, 10, null).get()
        val list = result.value ?: emptyList()
        // Only Book 1 isDownloaded == true
        assertEquals(1, list.size)
        assertEquals("1", list[0].mediaId)
        assertEquals("Alpha Book", list[0].mediaMetadata.title?.toString())
    }

    @Test
    fun testGetChildren_continueListening_online() {
        val result = callback.onGetChildren(mockk(), mockk(), "continue_listening", 0, 10, null).get()
        val list = result.value ?: emptyList()
        // Only downloaded books are returned, so only Book 1 is present
        assertEquals(1, list.size)
        assertEquals("1", list[0].mediaId)
    }

    @Test
    fun testGetChildren_continueListening_offline() {
        // Mock offline
        every { cm.allNetworks } returns emptyArray()

        val result = callback.onGetChildren(mockk(), mockk(), "continue_listening", 0, 10, null).get()
        val list = result.value ?: emptyList()
        // Only downloaded book (Book 1) is returned
        assertEquals(1, list.size)
        assertEquals("1", list[0].mediaId)
    }

    @Test
    fun testGetChildren_byAuthor() {
        val result = callback.onGetChildren(mockk(), mockk(), "by_author", 0, 10, null).get()
        val list = result.value ?: emptyList()
        // Only downloaded books are considered, so only Author A is present
        assertEquals(1, list.size)
        assertEquals("author_Author A", list[0].mediaId)
    }

    @Test
    fun testGetChildren_a_z() {
        val result = callback.onGetChildren(mockk(), mockk(), "a_z", 0, 10, null).get()
        val list = result.value ?: emptyList()
        // Only downloaded books are considered, so only letter A is present
        assertEquals(1, list.size)
        assertEquals("letter_A", list[0].mediaId)
    }

    @Test
    fun testGetChildren_authorName() {
        val result = callback.onGetChildren(mockk(), mockk(), "author_Author A", 0, 10, null).get()
        val list = result.value ?: emptyList()
        assertEquals(1, list.size)
        assertEquals("1", list[0].mediaId)
    }

    @Test
    fun testGetChildren_letter() {
        val result = callback.onGetChildren(mockk(), mockk(), "letter_B", 0, 10, null).get()
        val list = result.value ?: emptyList()
        // Book 2 (starting with B) is not downloaded, so the list should be empty
        assertEquals(0, list.size)

        val resultA = callback.onGetChildren(mockk(), mockk(), "letter_A", 0, 10, null).get()
        val listA = resultA.value ?: emptyList()
        assertEquals(1, listA.size)
        assertEquals("1", listA[0].mediaId)
    }

    @Test
    fun testGetChildren_loggedOut() {
        every { settingsRepository.isLoggedIn() } returns false

        val result = callback.onGetChildren(mockk(), mockk(), "root", 0, 10, null).get()
        val list = result.value ?: emptyList()
        assertEquals(1, list.size)
        assertEquals("login_required", list[0].mediaId)
        assertEquals("Please log in on your phone", list[0].mediaMetadata.title?.toString())
    }

    @Test
    fun testOnGetItem_downloaded() {
        val result = callback.onGetItem(mockk(), mockk(), "1").get()
        assertEquals("1", result.value?.mediaId)
        assertEquals("Alpha Book", result.value?.mediaMetadata?.title?.toString())
    }

    @Test
    fun testOnGetItem_notDownloaded() {
        val result = callback.onGetItem(mockk(), mockk(), "2").get()
        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, result.resultCode)
    }

    @Test
    fun testOnGetItem_notFound() {
        val result = callback.onGetItem(mockk(), mockk(), "non_existent").get()
        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, result.resultCode)
    }
}
