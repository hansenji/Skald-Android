package dev.vikingsen.skald.feature.miniplayer

import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val featureMiniPlayerModule = module {
    viewModel<MiniPlayerViewModel>()
}

