package dev.vikingsen.absclientapp.core.database

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreDatabaseModule = module {
    single { AppDatabase.create(androidContext()) }
}
