package app.coreme.messenger.features.calls.data.repository

import app.coreme.messenger.core.network.safeApiCall
import app.coreme.messenger.features.calls.data.api.CallsApi
import app.coreme.messenger.features.calls.data.dto.InitiateCallRequest
import app.coreme.messenger.features.calls.data.mapper.toDomain
import app.coreme.messenger.features.calls.domain.model.Call
import app.coreme.messenger.features.calls.domain.repository.CallsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallsRepositoryImpl @Inject constructor(
    private val api: CallsApi,
) : CallsRepository {

    override suspend fun getCallHistory(): Result<List<Call>> = safeApiCall {
        api.getCallHistory().map { it.toDomain() }
    }

    override suspend fun initiateCall(targetUserId: String, callType: String): Result<String> =
        safeApiCall {
            api.initiateCall(InitiateCallRequest(targetUserId, callType)).callId
        }
}
