package dev.vikingsen.skald.core.database

import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val coreDatabaseModule = module {
    single<AppDatabaseProvider>()
}

