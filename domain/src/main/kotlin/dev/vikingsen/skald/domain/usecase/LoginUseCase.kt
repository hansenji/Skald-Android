package dev.vikingsen.skald.domain.usecase

import dev.vikingsen.skald.core.model.LoggedUser
import dev.vikingsen.skald.domain.repository.AudiobookshelfRepository

class LoginUseCase(private val repository: AudiobookshelfRepository) {
    suspend operator fun invoke(url: String, user: String, pass: String): Result<LoggedUser> {
        return repository.login(url, user, pass)
    }
}
