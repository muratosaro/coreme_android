package app.coreme.messenger.core.network

import app.coreme.messenger.core.storage.SecureTokenStorage
import app.coreme.messenger.features.auth.data.api.AuthApi
import app.coreme.messenger.features.auth.data.dto.RefreshRequest
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    private val tokenStorage: SecureTokenStorage,
    private val authApi: Lazy<AuthApi>,
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry refresh endpoint to prevent infinite loops
        if (response.request.url.encodedPath.contains("/api/auth/refresh")) return null
        // Don't retry more than once per request
        if (responseCount(response) >= 2) return null

        return runBlocking {
            mutex.withLock {
                val refreshToken = tokenStorage.getRefreshToken() ?: return@withLock null

                // Another coroutine may have already refreshed — check if token changed
                val currentAccess = tokenStorage.getAccessToken()
                val requestAccess = response.request.header("Authorization")?.removePrefix("Bearer ")
                if (currentAccess != null && currentAccess != requestAccess) {
                    // Token was refreshed by a concurrent request — just retry with new token
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $currentAccess")
                        .build()
                }

                try {
                    val refreshResponse = authApi.get().refresh(RefreshRequest(refreshToken))
                    tokenStorage.saveTokens(refreshResponse.accessToken, refreshResponse.refreshToken)
                    Timber.d("Token refreshed successfully")

                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${refreshResponse.accessToken}")
                        .build()
                } catch (e: Exception) {
                    Timber.w(e, "Token refresh failed — clearing session")
                    tokenStorage.clearTokens()
                    null
                }
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
