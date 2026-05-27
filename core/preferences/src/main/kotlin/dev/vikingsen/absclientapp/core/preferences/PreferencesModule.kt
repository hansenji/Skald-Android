package dev.vikingsen.absclientapp.core.preferences

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val corePreferencesModule = module {
    single { PreferencesManager(androidContext()) }
}
