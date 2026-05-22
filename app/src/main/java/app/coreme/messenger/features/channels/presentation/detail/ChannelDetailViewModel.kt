package app.coreme.messenger.features.channels.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coreme.messenger.features.channels.domain.usecase.GetChannelPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelDetailViewModel @Inject constructor(
    private val getChannelPosts: GetChannelPostsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val channelId: String = checkNotNull(savedStateHandle["channelId"])
    private val channelName: String = savedStateHandle["channelName"] ?: ""

    private val _uiState = MutableStateFlow(ChannelDetailUiState(channelName = channelName))
    val uiState: StateFlow<ChannelDetailUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getChannelPosts(channelId).fold(
                onSuccess = { posts ->
                    _uiState.update { it.copy(isLoading = false, posts = posts) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }
}
