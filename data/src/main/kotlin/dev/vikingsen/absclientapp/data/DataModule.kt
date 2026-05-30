package dev.vikingsen.absclientapp.data

import dev.vikingsen.absclientapp.data.repository.AudiobookshelfRepositoryImpl
import dev.vikingsen.absclientapp.data.repository.SettingsRepositoryImpl
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val dataModule = module {
    single<SettingsRepositoryImpl>() bind SettingsRepository::class
    single<AudiobookshelfRepositoryImpl>() bind AudiobookshelfRepository::class
}

