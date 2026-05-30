package dev.vikingsen.absclientapp.feature.miniplayer

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureMiniPlayerModule = module {
    viewModel { MiniPlayerViewModel(get(), get()) }
}
