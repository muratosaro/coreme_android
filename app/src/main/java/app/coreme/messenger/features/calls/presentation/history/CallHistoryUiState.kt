package app.coreme.messenger.features.calls.presentation.history

import app.coreme.messenger.features.calls.domain.model.Call

data class CallHistoryUiState(
    val calls: List<Call> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
