package app.coreme.messenger.features.auth.domain.usecase

import app.coreme.messenger.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class CheckAuthUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(): Boolean = repository.isLoggedIn()
}
