package app.coreme.messenger.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coreme.messenger.features.settings.domain.model.AppSettings
import app.coreme.messenger.features.settings.domain.usecase.GetSettingsUseCase
import app.coreme.messenger.features.settings.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettings: GetSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getSettings().fold(
                onSuccess = { settings ->
                    _uiState.update { it.copy(isLoading = false, settings = settings) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    fun toggle(update: (AppSettings) -> AppSettings) {
        val current = _uiState.value.settings ?: return
        val updated = update(current)
        _uiState.update { it.copy(settings = updated) }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            updateSettings(updated).fold(
                onSuccess = { saved ->
                    _uiState.update { it.copy(isSaving = false, settings = saved, saveSuccess = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, settings = current, error = e.message) }
                },
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
