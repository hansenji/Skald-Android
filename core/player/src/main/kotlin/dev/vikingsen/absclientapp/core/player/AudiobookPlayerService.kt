package dev.vikingsen.absclientapp.core.player

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import org.koin.android.ext.android.inject

class AudiobookPlayerService : MediaLibraryService() {
    private val exoPlayer: ExoPlayer by inject()
    private val sessionCallback: MediaLibrarySession.Callback by inject()
    private var mediaLibrarySession: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        
        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            exoPlayer,
            sessionCallback
        ).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }
}
