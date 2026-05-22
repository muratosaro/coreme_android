package app.coreme.messenger.features.settings.presentation

import app.coreme.messenger.features.settings.domain.model.AppSettings

data class SettingsUiState(
    val settings: AppSettings? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
)
