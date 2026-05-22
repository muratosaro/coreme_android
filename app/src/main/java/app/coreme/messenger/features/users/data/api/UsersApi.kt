package app.coreme.messenger.features.users.data.api

import app.coreme.messenger.features.users.data.dto.UpdateProfileRequest
import app.coreme.messenger.features.users.data.dto.UserProfileDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface UsersApi {
    @GET("api/users/me")
    suspend fun getMe(): UserProfileDto

    @PATCH("api/users/me")
    suspend fun updateMe(@Body request: UpdateProfileRequest): UserProfileDto

    @GET("api/users/search")
    suspend fun searchUsers(@Query("q") query: String): List<UserProfileDto>

    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") id: String): UserProfileDto
}
