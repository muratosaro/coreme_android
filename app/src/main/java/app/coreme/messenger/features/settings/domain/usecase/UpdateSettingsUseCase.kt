package app.coreme.messenger.features.settings.domain.usecase

import app.coreme.messenger.features.settings.domain.model.AppSettings
import app.coreme.messenger.features.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(settings: AppSettings): Result<AppSettings> =
        repository.updateSettings(settings)
}
