package dev.vikingsen.skald.feature.assistant

import dev.vikingsen.skald.feature.assistant.engine.LocalAgentCoordinator
import dev.vikingsen.skald.feature.assistant.mcp.SkaldMcpToolRegistry
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val featureAssistantModule = module {
    single {
        SkaldMcpToolRegistry(
            playerManager = get(),
            repository = get(),
            settingsRepository = get(),
            fetchBookDetailsUseCase = get(),
            downloadAudioFileUseCase = get(),
            getPlaylistsUseCase = get()
        )
    }
    single {
        LocalAgentCoordinator(
            toolRegistry = get()
        )
    }
    viewModel<AssistantViewModel>()
}
