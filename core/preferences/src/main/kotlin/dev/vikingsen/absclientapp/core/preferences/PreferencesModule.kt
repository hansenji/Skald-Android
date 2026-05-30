package dev.vikingsen.absclientapp.core.preferences

import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val corePreferencesModule = module {
    single<PreferencesManager>()
}

