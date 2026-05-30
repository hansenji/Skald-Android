package dev.vikingsen.absclientapp.core.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromAudioFileList(value: List<LocalAudioFile>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toAudioFileList(value: String): List<LocalAudioFile> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromChapterList(value: List<LocalChapter>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toChapterList(value: String): List<LocalChapter> {
        return Json.decodeFromString(value)
    }
}

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<BookEntity>)

    @Query("SELECT * FROM books")
    fun getAllBooksFlow(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookByIdFlow(id: String): Flow<BookEntity?>

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: String)

    @RawQuery(observedEntities = [BookEntity::class, PlaybackProgressEntity::class])
    fun getBooksPaged(query: SupportSQLiteQuery): PagingSource<Int, BookWithProgressEntity>
}

@Dao
interface PlaybackProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: PlaybackProgressEntity)

    @Query("SELECT * FROM playback_progress WHERE bookId = :bookId")
    suspend fun getProgressForBook(bookId: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress WHERE bookId = :bookId")
    fun getProgressForBookFlow(bookId: String): Flow<PlaybackProgressEntity?>

    @Query("SELECT * FROM playback_progress")
    fun getAllProgressFlow(): Flow<List<PlaybackProgressEntity>>
}

@Dao
interface LibraryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(libraries: List<LibraryEntity>)

    @Query("SELECT * FROM libraries")
    suspend fun getAllLibraries(): List<LibraryEntity>

    @Query("DELETE FROM libraries")
    suspend fun deleteAll()
}

@Database(entities = [BookEntity::class, PlaybackProgressEntity::class, LibraryEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun libraryDao(): LibraryDao

    companion object {
        private const val DB_NAME = "abs_client_db"

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            ).fallbackToDestructiveMigration()
                .build()
        }
    }
}

