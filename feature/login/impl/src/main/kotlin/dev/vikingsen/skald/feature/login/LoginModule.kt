package dev.vikingsen.skald.feature.login

import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val featureLoginModule = module {
    viewModel<LoginViewModel>()
}

