package app.coreme.messenger.features.auth.data.repository

import app.coreme.messenger.core.network.safeApiCall
import app.coreme.messenger.core.session.UserSession
import app.coreme.messenger.core.storage.SecureTokenStorage
import app.coreme.messenger.features.auth.data.api.AuthApi
import app.coreme.messenger.features.auth.data.dto.LoginRequest
import app.coreme.messenger.features.auth.data.dto.LogoutRequest
import app.coreme.messenger.features.auth.data.dto.RegisterRequest
import app.coreme.messenger.features.auth.data.mapper.AuthMapper
import app.coreme.messenger.features.auth.domain.model.User
import app.coreme.messenger.features.auth.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenStorage: SecureTokenStorage,
    private val mapper: AuthMapper,
    private val userSession: UserSession,
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<User> =
        safeApiCall {
            val response = api.login(LoginRequest(username, password))
            tokenStorage.saveTokens(response.accessToken, response.refreshToken)
            val user = mapper.toDomain(response.user)
            userSession.setUser(user)
            user
        }

    override suspend fun register(
        username: String,
        email: String,
        password: String,
        displayName: String,
    ): Result<User> = safeApiCall {
        val response = api.register(RegisterRequest(username, email, password, displayName))
        tokenStorage.saveTokens(response.accessToken, response.refreshToken)
        val user = mapper.toDomain(response.user)
        userSession.setUser(user)
        user
    }

    override suspend fun logout() {
        val refreshToken = tokenStorage.getRefreshToken()
        if (refreshToken != null) {
            safeApiCall { api.logout(LogoutRequest(refreshToken)) }
        }
        tokenStorage.clearTokens()
        userSession.clearUser()
    }

    override suspend fun getCurrentUser(): Result<User> =
        safeApiCall {
            val user = mapper.toDomain(api.getMe())
            userSession.setUser(user)
            user
        }

    override fun isLoggedIn(): Boolean = tokenStorage.hasTokens()
}
