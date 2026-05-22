package app.coreme.messenger.features.channels.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coreme.messenger.features.channels.domain.usecase.GetChannelsUseCase
import app.coreme.messenger.features.channels.domain.usecase.ToggleSubscriptionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val getChannels: GetChannelsUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelsUiState())
    val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()

    init {
        loadChannels()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getChannels().fold(
                onSuccess = { list ->
                    _uiState.update { it.copy(isLoading = false, channels = list) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    fun toggleSubscription(channelId: String, isSubscribed: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(togglingSubscription = channelId) }
            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    channels = state.channels.map { ch ->
                        if (ch.id == channelId) ch.copy(
                            isSubscribed = !isSubscribed,
                            subscriberCount = if (isSubscribed) ch.subscriberCount - 1 else ch.subscriberCount + 1,
                        ) else ch
                    },
                )
            }
            toggleSubscriptionUseCase(channelId, isSubscribed).fold(
                onSuccess = {
                    _uiState.update { it.copy(togglingSubscription = null) }
                },
                onFailure = { e ->
                    // Revert on failure
                    _uiState.update { state ->
                        state.copy(
                            togglingSubscription = null,
                            error = e.message,
                            channels = state.channels.map { ch ->
                                if (ch.id == channelId) ch.copy(
                                    isSubscribed = isSubscribed,
                                    subscriberCount = if (isSubscribed) ch.subscriberCount + 1 else ch.subscriberCount - 1,
                                ) else ch
                            },
                        )
                    }
                },
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
