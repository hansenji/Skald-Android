package dev.vikingsen.absclientapp.feature.player

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featurePlayerModule = module {
    viewModel { PlayerViewModel(get(), get()) }
}
