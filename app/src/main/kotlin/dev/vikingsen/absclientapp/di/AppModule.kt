package dev.vikingsen.absclientapp.di

import dev.vikingsen.absclientapp.core.database.coreDatabaseModule
import dev.vikingsen.absclientapp.core.network.coreNetworkModule
import dev.vikingsen.absclientapp.core.player.corePlayerModule
import dev.vikingsen.absclientapp.core.preferences.corePreferencesModule
import dev.vikingsen.absclientapp.data.dataModule
import dev.vikingsen.absclientapp.domain.domainModule
import dev.vikingsen.absclientapp.feature.library.featureLibraryModule
import dev.vikingsen.absclientapp.feature.login.featureLoginModule
import dev.vikingsen.absclientapp.feature.player.featurePlayerModule
import dev.vikingsen.absclientapp.feature.androidauto.featureAndroidAutoModule
import dev.vikingsen.absclientapp.feature.miniplayer.featureMiniPlayerModule
import dev.vikingsen.absclientapp.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    includes(
        corePreferencesModule,
        coreDatabaseModule,
        coreNetworkModule,
        corePlayerModule,
        domainModule,
        dataModule,
        featureLoginModule,
        featureLibraryModule,
        featurePlayerModule,
        featureAndroidAutoModule,
        featureMiniPlayerModule
    )

    viewModel { MainViewModel(get()) }
}
