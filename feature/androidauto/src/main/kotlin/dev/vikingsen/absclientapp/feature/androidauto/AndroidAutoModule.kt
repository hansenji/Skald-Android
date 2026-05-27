package dev.vikingsen.absclientapp.feature.androidauto

import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val featureAndroidAutoModule = module {
    single<MediaLibrarySession.Callback> {
        AndroidAutoBrowseCallback(
            context = androidContext(),
            settingsRepository = get(),
            getBooksUseCase = get(),
            getPlaybackProgressUseCase = get(),
            coreCallback = get()
        )
    }
}
