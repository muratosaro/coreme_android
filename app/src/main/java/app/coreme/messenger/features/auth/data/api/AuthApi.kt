package app.coreme.messenger.features.auth.data.api

import app.coreme.messenger.features.auth.data.dto.LoginRequest
import app.coreme.messenger.features.auth.data.dto.LoginResponse
import app.coreme.messenger.features.auth.data.dto.LogoutRequest
import app.coreme.messenger.features.auth.data.dto.RefreshRequest
import app.coreme.messenger.features.auth.data.dto.RefreshResponse
import app.coreme.messenger.features.auth.data.dto.RegisterRequest
import app.coreme.messenger.features.auth.data.dto.RegisterResponse
import app.coreme.messenger.features.auth.data.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): RefreshResponse

    @POST("api/auth/logout")
    suspend fun logout(@Body request: LogoutRequest)

    @GET("api/users/me")
    suspend fun getMe(): UserDto
}
