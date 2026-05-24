package com.example.absclientapp.domain.usecase

import com.example.absclientapp.domain.repository.AudiobookshelfRepository
import com.example.absclientapp.domain.repository.SettingsRepository

class LogoutUseCase(
    private val repository: AudiobookshelfRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke() {
        settingsRepository.clear()
        repository.clearLocalData()
    }
}
