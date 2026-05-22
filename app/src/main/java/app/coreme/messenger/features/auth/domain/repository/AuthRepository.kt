package app.coreme.messenger.features.auth.domain.repository

import app.coreme.messenger.features.auth.domain.model.User

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<User>
    suspend fun register(username: String, email: String, password: String, displayName: String): Result<User>
    suspend fun logout()
    suspend fun getCurrentUser(): Result<User>
    fun isLoggedIn(): Boolean
}
