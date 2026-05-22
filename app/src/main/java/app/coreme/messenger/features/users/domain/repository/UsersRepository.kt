package app.coreme.messenger.features.users.domain.repository

import app.coreme.messenger.features.users.domain.model.UserProfile

interface UsersRepository {
    suspend fun getMyProfile(): Result<UserProfile>
    suspend fun updateProfile(displayName: String? = null, bio: String? = null, username: String? = null, avatarUrl: String? = null): Result<UserProfile>
    suspend fun searchUsers(query: String): Result<List<UserProfile>>
    suspend fun getUserById(id: String): Result<UserProfile>
}
