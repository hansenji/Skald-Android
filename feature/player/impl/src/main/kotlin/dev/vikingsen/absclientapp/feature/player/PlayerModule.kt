package dev.vikingsen.absclientapp.feature.player

import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val featurePlayerModule = module {
    viewModel<PlayerViewModel>()
}

