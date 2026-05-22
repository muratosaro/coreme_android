package app.coreme.messenger.features.channels.presentation.list

import app.coreme.messenger.features.channels.domain.model.Channel

data class ChannelsUiState(
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val togglingSubscription: String? = null,
    val error: String? = null,
)
