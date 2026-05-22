package app.coreme.messenger.features.settings.data.repository

import app.coreme.messenger.core.network.safeApiCall
import app.coreme.messenger.features.settings.data.api.SettingsApi
import app.coreme.messenger.features.settings.data.dto.ChangePasswordRequest
import app.coreme.messenger.features.settings.data.dto.UpdateSettingsRequest
import app.coreme.messenger.features.settings.domain.model.AppSettings
import app.coreme.messenger.features.settings.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val api: SettingsApi,
) : SettingsRepository {

    override suspend fun getSettings(): Result<AppSettings> = safeApiCall {
        val dto = api.getSettings()
        AppSettings(
            notificationsEnabled = dto.notificationsEnabled,
            soundEnabled = dto.soundEnabled,
            showReadReceipts = dto.showReadReceipts,
            lastSeenVisible = dto.lastSeenVisible,
            autoReplyEnabled = dto.autoReplyEnabled,
            autoReplyMessage = dto.autoReplyMessage,
        )
    }

    override suspend fun updateSettings(settings: AppSettings): Result<AppSettings> = safeApiCall {
        val dto = api.updateSettings(
            UpdateSettingsRequest(
                notificationsEnabled = settings.notificationsEnabled,
                soundEnabled = settings.soundEnabled,
                showReadReceipts = settings.showReadReceipts,
                lastSeenVisible = settings.lastSeenVisible,
                autoReplyEnabled = settings.autoReplyEnabled,
                autoReplyMessage = settings.autoReplyMessage,
            )
        )
        AppSettings(
            notificationsEnabled = dto.notificationsEnabled,
            soundEnabled = dto.soundEnabled,
            showReadReceipts = dto.showReadReceipts,
            lastSeenVisible = dto.lastSeenVisible,
            autoReplyEnabled = dto.autoReplyEnabled,
            autoReplyMessage = dto.autoReplyMessage,
        )
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        safeApiCall { api.changePassword(ChangePasswordRequest(currentPassword, newPassword)) }
}
