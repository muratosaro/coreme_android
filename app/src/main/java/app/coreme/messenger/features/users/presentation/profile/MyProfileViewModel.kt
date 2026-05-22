package app.coreme.messenger.features.users.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coreme.messenger.features.auth.domain.usecase.LogoutUseCase
import app.coreme.messenger.features.users.domain.usecase.GetUserProfileUseCase
import app.coreme.messenger.features.users.domain.usecase.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val getUserProfile: GetUserProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProfileUiState())
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getUserProfile.getMyProfile().fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profile,
                            displayName = profile.displayName,
                            bio = profile.bio ?: "",
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true, saveSuccess = false) }
    }

    fun cancelEditing() {
        val profile = _uiState.value.profile ?: return
        _uiState.update {
            it.copy(
                isEditing = false,
                displayName = profile.displayName,
                bio = profile.bio ?: "",
            )
        }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value) }
    }

    fun onBioChange(value: String) {
        _uiState.update { it.copy(bio = value) }
    }

    fun saveProfile() {
        val state = _uiState.value
        val profile = state.profile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            updateProfile(
                displayName = state.displayName.trim(),
                bio = state.bio.trim().ifBlank { null },
                username = profile.username,
            ).fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            saveSuccess = true,
                            profile = updated,
                            displayName = updated.displayName,
                            bio = updated.bio ?: "",
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                },
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onLogout()
        }
    }
}
