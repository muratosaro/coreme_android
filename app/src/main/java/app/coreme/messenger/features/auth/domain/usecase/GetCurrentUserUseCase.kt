package app.coreme.messenger.features.auth.domain.usecase

import app.coreme.messenger.features.auth.domain.model.User
import app.coreme.messenger.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<User> = repository.getCurrentUser()
}
