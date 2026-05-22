package app.coreme.messenger.features.settings.domain.repository

import app.coreme.messenger.features.settings.domain.model.AppSettings

interface SettingsRepository {
    suspend fun getSettings(): Result<AppSettings>
    suspend fun updateSettings(settings: AppSettings): Result<AppSettings>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
}
