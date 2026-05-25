package dev.vikingsen.absclientapp.domain.usecase

import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository
import dev.vikingsen.absclientapp.domain.repository.SettingsRepository

class LogoutUseCase(
    private val repository: AudiobookshelfRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke() {
        settingsRepository.clear()
        repository.clearLocalData()
    }
}
