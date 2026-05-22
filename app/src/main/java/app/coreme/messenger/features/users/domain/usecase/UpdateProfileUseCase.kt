package app.coreme.messenger.features.users.domain.usecase

import app.coreme.messenger.features.users.domain.model.UserProfile
import app.coreme.messenger.features.users.domain.repository.UsersRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(private val repository: UsersRepository) {
    suspend operator fun invoke(
        displayName: String? = null,
        bio: String? = null,
        username: String? = null,
    ): Result<UserProfile> {
        if (displayName != null && displayName.isBlank())
            return Result.failure(Exception("Display name cannot be empty"))
        if (username != null && username.length < 3)
            return Result.failure(Exception("Username must be at least 3 characters"))
        return repository.updateProfile(
            displayName = displayName?.trim(),
            bio = bio?.trim(),
            username = username?.trim(),
        )
    }
}
