package app.coreme.messenger.features.channels.presentation.detail

import app.coreme.messenger.features.channels.domain.model.ChannelPost

data class ChannelDetailUiState(
    val channelName: String = "",
    val posts: List<ChannelPost> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
