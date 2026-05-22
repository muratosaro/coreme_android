package app.coreme.messenger.features.users.data.repository

import app.coreme.messenger.core.network.safeApiCall
import app.coreme.messenger.core.session.UserSession
import app.coreme.messenger.features.auth.domain.model.User
import app.coreme.messenger.features.users.data.api.UsersApi
import app.coreme.messenger.features.users.data.dto.UpdateProfileRequest
import app.coreme.messenger.features.users.data.mapper.UserProfileMapper
import app.coreme.messenger.features.users.domain.model.UserProfile
import app.coreme.messenger.features.users.domain.repository.UsersRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsersRepositoryImpl @Inject constructor(
    private val api: UsersApi,
    private val mapper: UserProfileMapper,
    private val userSession: UserSession,
) : UsersRepository {

    override suspend fun getMyProfile(): Result<UserProfile> = safeApiCall {
        val profile = mapper.toDomain(api.getMe())
        userSession.setUser(User(
            id = profile.id,
            username = profile.username,
            displayName = profile.displayName,
            email = profile.email,
            bio = profile.bio,
            avatarUrl = profile.avatarUrl,
            isOnline = profile.isOnline,
        ))
        profile
    }

    override suspend fun updateProfile(
        displayName: String?,
        bio: String?,
        username: String?,
        avatarUrl: String?,
    ): Result<UserProfile> = safeApiCall {
        val updated = mapper.toDomain(api.updateMe(UpdateProfileRequest(displayName, bio, username, avatarUrl)))
        userSession.setUser(User(
            id = updated.id,
            username = updated.username,
            displayName = updated.displayName,
            email = updated.email,
            bio = updated.bio,
            avatarUrl = updated.avatarUrl,
            isOnline = updated.isOnline,
        ))
        updated
    }

    override suspend fun searchUsers(query: String): Result<List<UserProfile>> =
        safeApiCall { api.searchUsers(query).map { mapper.toDomain(it) } }

    override suspend fun getUserById(id: String): Result<UserProfile> =
        safeApiCall { mapper.toDomain(api.getUserById(id)) }
}
