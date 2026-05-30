package dev.vikingsen.absclientapp.feature.androidauto

import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val featureAndroidAutoModule = module {
    single<AndroidAutoBrowseCallback>() bind MediaLibrarySession.Callback::class
}

