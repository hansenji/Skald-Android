package dev.vikingsen.absclientapp.feature.home

import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val featureHomeModule = module {
    viewModel<HomeViewModel>()
}
