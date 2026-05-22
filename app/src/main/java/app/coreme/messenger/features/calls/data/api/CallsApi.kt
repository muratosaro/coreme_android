package app.coreme.messenger.features.calls.data.api

import app.coreme.messenger.features.calls.data.dto.CallDto
import app.coreme.messenger.features.calls.data.dto.InitiateCallRequest
import app.coreme.messenger.features.calls.data.dto.InitiateCallResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CallsApi {
    @GET("api/calls")
    suspend fun getCallHistory(): List<CallDto>

    @POST("api/calls")
    suspend fun initiateCall(@Body request: InitiateCallRequest): InitiateCallResponse
}
