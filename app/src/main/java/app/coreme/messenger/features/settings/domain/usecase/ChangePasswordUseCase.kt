package app.coreme.messenger.features.settings.domain.usecase

import app.coreme.messenger.features.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(currentPassword: String, newPassword: String): Result<Unit> {
        if (currentPassword.isBlank()) return Result.failure(Exception("Current password is required"))
        if (newPassword.length < 6) return Result.failure(Exception("New password must be at least 6 characters"))
        if (currentPassword == newPassword) return Result.failure(Exception("New password must differ from current"))
        return repository.changePassword(currentPassword, newPassword)
    }
}
