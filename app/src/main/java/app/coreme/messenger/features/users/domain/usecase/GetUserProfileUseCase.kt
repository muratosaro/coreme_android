package app.coreme.messenger.features.users.domain.usecase

import app.coreme.messenger.features.users.domain.model.UserProfile
import app.coreme.messenger.features.users.domain.repository.UsersRepository
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(private val repository: UsersRepository) {
    suspend operator fun invoke(id: String): Result<UserProfile> = repository.getUserById(id)
    suspend fun getMyProfile(): Result<UserProfile> = repository.getMyProfile()
}
