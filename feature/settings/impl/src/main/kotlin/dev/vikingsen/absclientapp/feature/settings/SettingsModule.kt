package dev.vikingsen.absclientapp.feature.settings

import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val featureSettingsModule = module {
    viewModel<SettingsViewModel>()
}
