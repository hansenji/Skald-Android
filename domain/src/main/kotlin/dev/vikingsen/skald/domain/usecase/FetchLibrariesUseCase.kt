package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.Library
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository

class FetchLibrariesUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(): Result<List<Library>> = repository.fetchLibraries()
}
