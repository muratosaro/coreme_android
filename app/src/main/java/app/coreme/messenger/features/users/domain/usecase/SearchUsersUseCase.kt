package app.coreme.messenger.features.users.domain.usecase

import app.coreme.messenger.features.users.domain.model.UserProfile
import app.coreme.messenger.features.users.domain.repository.UsersRepository
import javax.inject.Inject

class SearchUsersUseCase @Inject constructor(private val repository: UsersRepository) {
    suspend operator fun invoke(query: String): Result<List<UserProfile>> {
        if (query.length < 2) return Result.success(emptyList())
        return repository.searchUsers(query)
    }
}
