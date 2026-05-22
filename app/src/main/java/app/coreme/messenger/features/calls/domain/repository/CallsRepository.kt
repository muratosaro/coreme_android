package app.coreme.messenger.features.calls.domain.repository

import app.coreme.messenger.features.calls.domain.model.Call

interface CallsRepository {
    suspend fun getCallHistory(): Result<List<Call>>
    suspend fun initiateCall(targetUserId: String, callType: String): Result<String>
}
