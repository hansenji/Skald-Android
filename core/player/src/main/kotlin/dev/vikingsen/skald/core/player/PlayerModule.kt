package dev.vikingsen.skald.core.player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dev.vikingsen.skald.core.preferences.PreferencesManager
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import dev.vikingsen.skald.domain.repository.PlaybackStateProvider
import dev.vikingsen.skald.domain.repository.PlaylistPlayer
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single


class AuthenticatedHttpDataSourceFactory(
    private val preferencesManager: PreferencesManager
) : DataSource.Factory {
    private val baseFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("ABS-Client-Android")

    override fun createDataSource(): DataSource {
        val token = preferencesManager.getToken()
        if (!token.isNullOrEmpty()) {
            baseFactory.setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
        } else {
            baseFactory.setDefaultRequestProperties(emptyMap())
        }
        return baseFactory.createDataSource()
    }
}

val corePlayerModule = module {
    single<ExoPlayerProvider>()

    single<PlayerManager>()
    single<PlaybackStateProvider> { get<PlayerManager>() }
    single<PlaylistPlayer> { get<PlayerManager>() }
    
    single<AudiobookForwardingPlayer>() bind Player::class

    single<AudiobookSessionCallback>() bind MediaLibrarySession.Callback::class
}

