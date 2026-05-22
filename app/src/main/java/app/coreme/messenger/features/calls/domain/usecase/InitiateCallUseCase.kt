package app.coreme.messenger.features.calls.domain.usecase

import app.coreme.messenger.features.calls.domain.repository.CallsRepository
import javax.inject.Inject

class InitiateCallUseCase @Inject constructor(private val repository: CallsRepository) {
    suspend operator fun invoke(targetUserId: String, callType: String = "voice"): Result<String> {
        if (targetUserId.isBlank()) return Result.failure(Exception("Target user is required"))
        return repository.initiateCall(targetUserId, callType)
    }
}
