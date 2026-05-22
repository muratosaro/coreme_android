package app.coreme.messenger.features.auth.domain.usecase

import app.coreme.messenger.features.auth.domain.model.User
import app.coreme.messenger.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(
        username: String,
        email: String,
        password: String,
        displayName: String,
    ): Result<User> {
        if (username.isBlank()) return Result.failure(Exception("Username cannot be empty"))
        if (username.length < 3) return Result.failure(Exception("Username must be at least 3 characters"))
        if (!email.contains("@")) return Result.failure(Exception("Invalid email address"))
        if (password.length < 6) return Result.failure(Exception("Password must be at least 6 characters"))
        if (displayName.isBlank()) return Result.failure(Exception("Display name cannot be empty"))
        return repository.register(username.trim(), email.trim(), password, displayName.trim())
    }
}
