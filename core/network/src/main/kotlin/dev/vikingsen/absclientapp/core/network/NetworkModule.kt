package dev.vikingsen.absclientapp.core.network

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val coreNetworkModule = module {
    single<AudiobookshelfRemoteDataSourceImpl>() bind AudiobookshelfRemoteDataSource::class
}

