package app.coreme.messenger.features.auth.domain.usecase

import app.coreme.messenger.features.auth.domain.model.User
import app.coreme.messenger.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(username: String, password: String): Result<User> {
        if (username.isBlank()) return Result.failure(Exception("Username cannot be empty"))
        if (password.length < 6) return Result.failure(Exception("Password must be at least 6 characters"))
        return repository.login(username.trim(), password)
    }
}
