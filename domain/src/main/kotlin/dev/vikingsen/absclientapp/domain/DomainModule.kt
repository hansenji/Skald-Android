package dev.vikingsen.absclientapp.domain

import dev.vikingsen.absclientapp.domain.usecase.*
import org.koin.dsl.module

val domainModule = module {
    single { LoginUseCase(get()) }
    single { FetchLibrariesUseCase(get()) }
    single { SyncLibraryBooksUseCase(get()) }
    single { GetBooksUseCase(get()) }
    single { GetPlaybackProgressUseCase(get()) }
    single { GetBookWithProgressUseCase(get()) }
    single { FetchBookDetailsUseCase(get()) }
    single { DownloadAudioFileUseCase(get()) }
    single { DeleteLocalBookFilesUseCase(get()) }
    single { SaveProgressUseCase(get()) }
    single { StartPlaybackSessionUseCase(get()) }
    single { LogoutUseCase(get(), get()) }
    single { GetMiniPlayerStateUseCase(get(), get()) }
}
