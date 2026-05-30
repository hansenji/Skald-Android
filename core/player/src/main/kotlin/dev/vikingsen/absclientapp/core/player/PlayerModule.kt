package dev.vikingsen.absclientapp.core.player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dev.vikingsen.absclientapp.core.preferences.PreferencesManager
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import dev.vikingsen.absclientapp.domain.repository.PlaybackStateProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

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
    single {
        val preferencesManager: PreferencesManager = get()
        val httpDataSourceFactory = AuthenticatedHttpDataSourceFactory(preferencesManager)
        val dataSourceFactory = DefaultDataSource.Factory(androidContext(), httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        ExoPlayer.Builder(androidContext())
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setAudioAttributes(audioAttributes, true)
            .build()
    }

    single { PlayerManager(androidContext(), get(), get(), get(), get()) }
    single<PlaybackStateProvider> { get<PlayerManager>() }
    
    single<Player> {
        AudiobookForwardingPlayer(
            player = get<ExoPlayer>(),
            playerManager = get(),
            settingsRepository = get()
        )
    }

    single {
        AudiobookSessionCallback(
            context = androidContext(),
            playerManager = get(),
            settingsRepository = get(),
            getBooksUseCase = get(),
            getPlaybackProgressUseCase = get()
        )
    }

    single<MediaLibrarySession.Callback> {
        get<AudiobookSessionCallback>()
    }
}
