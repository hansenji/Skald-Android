package com.example.absclientapp.domain.usecase

import com.example.absclientapp.domain.model.Library
import com.example.absclientapp.domain.repository.AudiobookshelfRepository

class FetchLibrariesUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(): Result<List<Library>> = repository.fetchLibraries()
}
