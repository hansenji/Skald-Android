package dev.vikingsen.absclientapp.core.network

import org.koin.dsl.module

val coreNetworkModule = module {
    single<AudiobookshelfRemoteDataSource> { AudiobookshelfRemoteDataSourceImpl(get()) }
}
