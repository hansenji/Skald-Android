package dev.vikingsen.absclientapp.domain.usecase

import dev.vikingsen.absclientapp.domain.model.Library
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository

class FetchLibrariesUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(): Result<List<Library>> = repository.fetchLibraries()
}
