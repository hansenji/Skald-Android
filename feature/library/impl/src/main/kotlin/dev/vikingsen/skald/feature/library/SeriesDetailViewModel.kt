package dev.vikingsen.skald.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vikingsen.skald.core.model.Series
import dev.vikingsen.skald.domain.repository.SettingsRepository
import dev.vikingsen.skald.domain.usecase.GetMiniPlayerStateUseCase
import dev.vikingsen.skald.domain.usecase.GetSeriesDetailsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class SeriesDetailViewModel(
    private val getSeriesDetailsUseCase: GetSeriesDetailsUseCase,
    private val settingsRepository: SettingsRepository,
    private val getMiniPlayerStateUseCase: GetMiniPlayerStateUseCase
) : ViewModel() {

    val seriesId = MutableStateFlow<String?>(null)

    val series: StateFlow<Series?> = seriesId
        .filterNotNull()
        .map { id -> getSeriesDetailsUseCase.getSeries(id) }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    val books: StateFlow<List<BookCardUiModel>> = seriesId
        .filterNotNull()
        .flatMapLatest { id -> getSeriesDetailsUseCase.getBooks(id) }
        .map { booksWithProgress ->
            val serverUrl = settingsRepository.getServerUrl() ?: ""
            val token = settingsRepository.getToken() ?: ""
            
            booksWithProgress
                .sortedBy { it.book.seriesSequence.toSequenceNumber() }
                .map { bookWithProgress ->
                    val book = bookWithProgress.book
                    val progress = bookWithProgress.progress
                    
                    val coverUrl = if (!book.coverPath.isNullOrEmpty()) book.coverPath!!
                                   else "${serverUrl.trimEnd('/')}/api/items/${book.id}/cover"
                    val authHeader = if (!book.coverPath.isNullOrEmpty()) null
                                     else "Bearer $token"

                    BookCardUiModel(
                        id = book.id,
                        title = book.title,
                        author = book.author,
                        narrator = book.narrator,
                        coverUrl = coverUrl,
                        authorizationHeader = authHeader,
                        isDownloaded = book.isDownloaded,
                        duration = book.duration,
                        progress = progress?.let {
                            PlaybackProgressUiModel(
                                progress = it.progress,
                                isFinished = it.isFinished,
                                currentTime = it.currentTime,
                                lastUpdated = it.lastUpdated
                            )
                        },
                        seriesSequence = book.seriesSequence
                    )
                }
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    val showMiniPlayer: StateFlow<Boolean> = getMiniPlayerStateUseCase()
        .map { it != null }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = false)

    fun setSeriesId(id: String) {
        seriesId.value = id
    }

    private fun String?.toSequenceNumber(): Double {
        if (this == null) return Double.MAX_VALUE
        val cleaned = this.trim().takeWhile { it.isDigit() || it == '.' }
        return cleaned.toDoubleOrNull() ?: Double.MAX_VALUE
    }
}
