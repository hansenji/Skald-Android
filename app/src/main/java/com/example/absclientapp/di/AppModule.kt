package com.example.absclientapp.di

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.absclientapp.data.PreferencesManager
import com.example.absclientapp.data.database.AppDatabase
import com.example.absclientapp.data.repository.AudiobookshelfRepositoryImpl
import com.example.absclientapp.data.repository.SettingsRepositoryImpl
import com.example.absclientapp.data.remote.AudiobookshelfRemoteDataSource
import com.example.absclientapp.data.remote.AudiobookshelfRemoteDataSourceImpl
import com.example.absclientapp.domain.repository.AudiobookshelfRepository
import com.example.absclientapp.domain.repository.SettingsRepository
import com.example.absclientapp.domain.usecase.*
import com.example.absclientapp.player.PlayerManager
import com.example.absclientapp.ui.login.LoginViewModel
import com.example.absclientapp.ui.library.LibraryViewModel
import com.example.absclientapp.ui.detail.DetailViewModel
import com.example.absclientapp.ui.player.PlayerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
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

val appModule = module {
    single { PreferencesManager(androidContext()) }
    single { AppDatabase.create(androidContext()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<AudiobookshelfRemoteDataSource> { AudiobookshelfRemoteDataSourceImpl(get()) }
    single<AudiobookshelfRepository> { AudiobookshelfRepositoryImpl(androidContext(), get(), get(), get()) }

    // Use Cases
    single { LoginUseCase(get()) }
    single { FetchLibrariesUseCase(get()) }
    single { SyncLibraryBooksUseCase(get()) }
    single { GetBooksUseCase(get()) }
    single { GetBookWithProgressUseCase(get()) }
    single { FetchBookDetailsUseCase(get()) }
    single { DownloadAudioFileUseCase(get()) }
    single { SaveProgressUseCase(get()) }
    single { StartPlaybackSessionUseCase(get()) }
    single { LogoutUseCase(get(), get()) }

    // Playback Components
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

    // ViewModels
    viewModel { LoginViewModel(get(), get(), get(), get()) }
    viewModel { LibraryViewModel(get(), get(), get(), get()) }
    viewModel { DetailViewModel(get(), get(), get(), get()) }
    viewModel { PlayerViewModel(get()) }
}
