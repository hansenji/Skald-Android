package dev.vikingsen.skald.core.preferences

import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val corePreferencesModule = module {
    single<PreferencesManager>()
}

