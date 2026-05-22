package app.coreme.messenger.features.users.presentation.profile

import app.coreme.messenger.features.users.domain.model.UserProfile

data class UserProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)
