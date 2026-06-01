package dev.vikingsen.skald.data

import dev.vikingsen.skald.data.repository.AudiobookshelfRepositoryImpl
import dev.vikingsen.skald.data.repository.SettingsRepositoryImpl
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository
import dev.vikingsen.skald.domain.repository.SettingsRepository
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val dataModule = module {
    single<SettingsRepositoryImpl>() bind SettingsRepository::class
    single<AudiobookshelfRepositoryImpl>() bind AudiobookshelfRepository::class
}

