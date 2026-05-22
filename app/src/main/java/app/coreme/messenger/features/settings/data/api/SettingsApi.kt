package app.coreme.messenger.features.settings.data.api

import app.coreme.messenger.features.settings.data.dto.AppSettingsDto
import app.coreme.messenger.features.settings.data.dto.ChangePasswordRequest
import app.coreme.messenger.features.settings.data.dto.UpdateSettingsRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface SettingsApi {
    @GET("api/settings")
    suspend fun getSettings(): AppSettingsDto

    @PATCH("api/settings")
    suspend fun updateSettings(@Body request: UpdateSettingsRequest): AppSettingsDto

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest)
}
