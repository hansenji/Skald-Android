package dev.vikingsen.skald.feature.library

import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val featureLibraryModule = module {
    single<BookMenuActionUtil>()
    viewModel<LibraryViewModel>()
    viewModel<DetailViewModel>()
    viewModel<SeriesDetailViewModel>()
    viewModel<AuthorDetailViewModel>()
    viewModel<CollectionDetailViewModel>()
    viewModel<PlaylistDetailViewModel>()
}


