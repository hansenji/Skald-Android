package dev.vikingsen.absclientapp.feature.library

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureLibraryModule = module {
    viewModel { LibraryViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { DetailViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
}
