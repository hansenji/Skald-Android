package dev.vikingsen.skald.domain

import dev.vikingsen.skald.domain.usecase.*
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
    single<GetPersonalizedShelvesUseCase>()
    single<SyncPersonalizedShelvesUseCase>()
    single<SyncGlobalProgressUseCase>()
    single<GetSeriesUseCase>()
    single<SyncLibrarySeriesUseCase>()
    single<GetSeriesDetailsUseCase>()
    single<GetAuthorsUseCase>()
    single<SyncLibraryAuthorsUseCase>()
    single<GetAuthorDetailsUseCase>()
    single<GetCollectionsUseCase>()
    single<SyncLibraryCollectionsUseCase>()
    single<GetCollectionDetailsUseCase>()
    single<GetPlaylistsUseCase>()
    single<SyncPlaylistsUseCase>()
    single<PlayPlaylistUseCase>()
}

