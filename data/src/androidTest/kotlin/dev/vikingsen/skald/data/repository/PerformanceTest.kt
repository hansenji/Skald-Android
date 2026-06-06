package dev.vikingsen.skald.data.repository

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vikingsen.skald.core.database.AppDatabase
import dev.vikingsen.skald.core.database.AppDatabaseProvider
import dev.vikingsen.skald.core.database.LibraryEntity
import dev.vikingsen.skald.core.database.PlaybackProgressEntity
import dev.vikingsen.skald.core.network.AudiobookshelfRemoteDataSource
import dev.vikingsen.skald.core.network.ProgressSyncRequest
import dev.vikingsen.skald.core.preferences.PreferencesManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class PerformanceTest {

    private lateinit var context: Context
    private lateinit var inMemoryDb: AppDatabase
    private lateinit var dbProvider: AppDatabaseProvider
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var remoteDataSource: AudiobookshelfRemoteDataSource
    private lateinit var repository: AudiobookshelfRepositoryImpl

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        context = InstrumentationRegistry.getInstrumentation().targetContext
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        dbProvider = mockk(relaxed = true)
        every { dbProvider.database } returns inMemoryDb

        preferencesManager = mockk(relaxed = true)
        remoteDataSource = mockk(relaxed = true)

        repository = AudiobookshelfRepositoryImpl(
            context = context,
            dbProvider = dbProvider,
            preferencesManager = preferencesManager,
            remoteDataSource = remoteDataSource
        )
    }

    @After
    fun tearDown() {
        inMemoryDb.close()
    }

    @Test
    fun testDataMinimization_syncPayloadUnder1KB() {
        val request = ProgressSyncRequest(
            timeListened = 120.5,
            currentTime = 345.2
        )
        val serialized = Json.encodeToString(request)
        val sizeInBytes = serialized.toByteArray(Charsets.UTF_8).size
        Log.i("PerformanceTest", "ProgressSyncRequest serialized payload size: $sizeInBytes bytes")
        assertTrue("Payload should be under 1KB (1024 bytes), but was $sizeInBytes bytes", sizeInBytes < 1024)
    }

    @Test
    fun testOfflineTransitionTime_under300ms() = runBlocking {
        val count = 100
        val mockLibraryEntities = List(count) { index ->
            LibraryEntity(
                id = "library-$index",
                name = "Library $index",
                type = "book"
            )
        }
        
        // Write the libraries to the database
        inMemoryDb.libraryDao().insertAll(mockLibraryEntities)

        val duration = measureTimeMillis {
            val cached = repository.getCachedLibraries()
            assertEquals(count, cached.size)
        }
        Log.i("PerformanceTest", "Offline transition (cached library fetch) took: $duration ms")
        assertTrue("Offline transition should be under 300ms, but took $duration ms", duration < 300)
    }

    @Test
    fun testPlaybackStartDelay_repositoryPrepUnder100ms() = runBlocking {
        // Measure database save speed
        val duration = measureTimeMillis {
            repository.saveLocalProgress("test-book", 15.0, 1200.0)
        }
        
        // Read progress back to verify
        val savedProgress = inMemoryDb.playbackProgressDao().getProgressForBook("test-book")
        assertTrue(savedProgress != null)
        assertEquals(15.0, savedProgress!!.currentTime, 0.001)

        Log.i("PerformanceTest", "Playback local progress preparation took: $duration ms")
        assertTrue("Playback preparation should be under 100ms, but took $duration ms", duration < 100)
    }
}
