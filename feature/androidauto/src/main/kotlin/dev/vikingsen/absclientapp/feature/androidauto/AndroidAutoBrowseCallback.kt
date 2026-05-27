package dev.vikingsen.absclientapp.feature.androidauto

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dev.vikingsen.absclientapp.core.model.Book
import dev.vikingsen.absclientapp.core.player.AudiobookSessionCallback
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository
import dev.vikingsen.absclientapp.domain.usecase.GetBooksUseCase
import dev.vikingsen.absclientapp.domain.usecase.GetPlaybackProgressUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dev.vikingsen.absclientapp.feature.androidauto.R
import java.io.File

@OptIn(UnstableApi::class)
class AndroidAutoBrowseCallback(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val getBooksUseCase: GetBooksUseCase,
    private val getPlaybackProgressUseCase: GetPlaybackProgressUseCase,
    private val coreCallback: AudiobookSessionCallback
) : MediaLibrarySession.Callback {

    private val scope = CoroutineScope(Dispatchers.Main)

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val cap = cm.getNetworkCapabilities(net) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun buildBrowsableItem(id: String, title: String, subtitle: String? = null): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setFolderType(MediaMetadata.FOLDER_TYPE_MIXED)
            .setIsPlayable(false)
            .setIsBrowsable(true)
            .build()
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(meta)
            .build()
    }

    private fun buildPlayableBookItem(book: Book): MediaItem {
        val isNetworkOnline = isOnline()
        val coverUri = if (book.coverPath != null && File(book.coverPath).exists()) {
            Uri.fromFile(File(book.coverPath))
        } else if (isNetworkOnline) {
            val serverUrl = settingsRepository.getServerUrl() ?: ""
            val sanitizedBase = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
            Uri.parse("${sanitizedBase}api/items/${book.id}/cover")
        } else {
            Uri.parse("android.resource://${context.packageName}/drawable/ic_book_placeholder")
        }

        val meta = MediaMetadata.Builder()
            .setTitle(book.title)
            .setArtist(book.author)
            .setAlbumArtist(book.narrator)
            .setArtworkUri(coverUri)
            .setFolderType(MediaMetadata.FOLDER_TYPE_NONE)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
            .build()

        return MediaItem.Builder()
            .setMediaId(book.id)
            .setMediaMetadata(meta)
            .build()
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        return coreCallback.onConnect(session, controller)
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        return coreCallback.onCustomCommand(session, controller, customCommand, args)
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        return coreCallback.onAddMediaItems(mediaSession, controller, mediaItems)
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        if (!settingsRepository.isLoggedIn()) {
            val meta = MediaMetadata.Builder()
                .setTitle(context.getString(R.string.auto_login_required))
                .setFolderType(MediaMetadata.FOLDER_TYPE_NONE)
                .setIsPlayable(false)
                .setIsBrowsable(false)
                .build()
            val rootItem = MediaItem.Builder()
                .setMediaId("root")
                .setMediaMetadata(meta)
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        val rootItem = MediaItem.Builder()
            .setMediaId("root")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(context.getString(R.string.auto_root))
                    .setFolderType(MediaMetadata.FOLDER_TYPE_MIXED)
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .build()
            ).build()
        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        scope.launch {
            try {
                if (!settingsRepository.isLoggedIn()) {
                    future.set(LibraryResult.ofItemList(ImmutableList.of(), params))
                    return@launch
                }

                val allBooks = getBooksUseCase().first()
                val progressList = getPlaybackProgressUseCase().first()
                val isOffline = !isOnline()

                // If offline, filter all queries to show only downloaded books
                val books = if (isOffline) {
                    allBooks.filter { it.isDownloaded }
                } else {
                    allBooks
                }

                when (parentId) {
                    "root" -> {
                        val children = listOf(
                            buildBrowsableItem("continue_listening", context.getString(R.string.auto_continue_listening)),
                            buildBrowsableItem("downloads", context.getString(R.string.auto_downloads)),
                            buildBrowsableItem("all_audiobooks", context.getString(R.string.auto_all_audiobooks))
                        )
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(children), params))
                    }
                    "continue_listening" -> {
                        val filtered = progressList
                            .filter { it.progress > 0f && it.progress < 0.99f && !it.isFinished }
                            .sortedByDescending { it.lastUpdated }
                            .mapNotNull { progress -> books.find { it.id == progress.bookId } }
                            .map { buildPlayableBookItem(it) }
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(filtered), params))
                    }
                    "downloads" -> {
                        // Downloads folder always filters to isDownloaded == true, regardless of offline/online state
                        val downloaded = allBooks.filter { it.isDownloaded }.map { buildPlayableBookItem(it) }
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(downloaded), params))
                    }
                    "all_audiobooks" -> {
                        val categories = listOf(
                            buildBrowsableItem("by_author", context.getString(R.string.auto_by_author)),
                            buildBrowsableItem("a_z", context.getString(R.string.auto_a_z))
                        )
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(categories), params))
                    }
                    "by_author" -> {
                        val authors = books.map { it.author }.distinct().sorted()
                        val authorItems = authors.map { buildBrowsableItem("author_$it", it) }
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(authorItems), params))
                    }
                    "a_z" -> {
                        val letters = books.map { it.title.firstOrNull()?.uppercaseChar() }
                            .filterNotNull()
                            .distinct()
                            .sorted()
                        val letterItems = letters.map { buildBrowsableItem("letter_$it", it.toString()) }
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(letterItems), params))
                    }
                    else -> {
                        if (parentId.startsWith("author_")) {
                            val authorName = parentId.substringAfter("author_")
                            val authorBooks = books.filter { it.author == authorName }
                                .sortedBy { it.title }
                                .map { buildPlayableBookItem(it) }
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(authorBooks), params))
                        } else if (parentId.startsWith("letter_")) {
                            val letter = parentId.substringAfter("letter_").firstOrNull()
                            val letterBooks = books.filter { it.title.firstOrNull()?.uppercaseChar() == letter }
                                .sortedBy { it.title }
                                .map { buildPlayableBookItem(it) }
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(letterBooks), params))
                        } else {
                            future.set(LibraryResult.ofItemList(ImmutableList.of(), params))
                        }
                    }
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }

        return future
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val future = SettableFuture.create<LibraryResult<MediaItem>>()

        scope.launch {
            try {
                if (!settingsRepository.isLoggedIn()) {
                    future.set(LibraryResult.ofError(SessionResult.RESULT_ERROR_PERMISSION_DENIED))
                    return@launch
                }

                val allBooks = getBooksUseCase().first()
                val book = allBooks.find { it.id == mediaId }
                if (book != null) {
                    future.set(LibraryResult.ofItem(buildPlayableBookItem(book), null))
                } else {
                    future.set(LibraryResult.ofError(SessionResult.RESULT_ERROR_BAD_VALUE))
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }

        return future
    }
}
