package dev.vikingsen.absclientapp.domain

import dev.vikingsen.absclientapp.domain.usecase.*
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val domainModule = module {
    single<LoginUseCase>()
    single<FetchLibrariesUseCase>()
    single<SyncLibraryBooksUseCase>()
    single<GetBooksUseCase>()
    single<GetPlaybackProgressUseCase>()
    single<GetBookWithProgressUseCase>()
    single<FetchBookDetailsUseCase>()
    single<DownloadAudioFileUseCase>()
    single<DeleteLocalBookFilesUseCase>()
    single<SaveProgressUseCase>()
    single<StartPlaybackSessionUseCase>()
    single<LogoutUseCase>()
    single<GetMiniPlayerStateUseCase>()
}

