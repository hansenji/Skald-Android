package dev.vikingsen.absclientapp.core.database

import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val coreDatabaseModule = module {
    single<AppDatabaseProvider>()
}

