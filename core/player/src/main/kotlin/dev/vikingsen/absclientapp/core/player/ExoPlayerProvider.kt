package dev.vikingsen.absclientapp.core.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import dev.vikingsen.absclientapp.core.preferences.PreferencesManager

class ExoPlayerProvider(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    val exoPlayer: ExoPlayer by lazy {
        val httpDataSourceFactory = AuthenticatedHttpDataSourceFactory(preferencesManager)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setAudioAttributes(audioAttributes, true)
            .build()
    }
}
