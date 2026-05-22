package app.coreme.messenger.features.calls.domain.usecase

import app.coreme.messenger.features.calls.domain.model.Call
import app.coreme.messenger.features.calls.domain.repository.CallsRepository
import javax.inject.Inject

class GetCallHistoryUseCase @Inject constructor(private val repository: CallsRepository) {
    suspend operator fun invoke(): Result<List<Call>> = repository.getCallHistory()
}
