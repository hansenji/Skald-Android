package dev.vikingsen.absclientapp.data

import dev.vikingsen.absclientapp.data.repository.AudiobookshelfRepositoryImpl
import dev.vikingsen.absclientapp.data.repository.SettingsRepositoryImpl
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<AudiobookshelfRepository> { AudiobookshelfRepositoryImpl(androidContext(), get(), get(), get()) }
}
