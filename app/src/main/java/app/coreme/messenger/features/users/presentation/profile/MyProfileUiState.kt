package app.coreme.messenger.features.users.presentation.profile

import app.coreme.messenger.features.users.domain.model.UserProfile

data class MyProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val displayName: String = "",
    val bio: String = "",
    val error: String? = null,
    val saveSuccess: Boolean = false,
)
