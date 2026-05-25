package dev.vikingsen.absclientapp.domain.usecase

import dev.vikingsen.absclientapp.domain.model.LoggedUser
import dev.vikingsen.absclientapp.domain.repository.AudiobookshelfRepository

class LoginUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(url: String, user: String, pass: String): Result<LoggedUser> {
        return repository.login(url, user, pass)
    }
}
