package app.coreme.messenger.features.auth.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.coreme.messenger.features.auth.domain.usecase.CheckAuthUseCase
import app.coreme.messenger.features.auth.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashDestination {
    data object None : SplashDestination
    data object Login : SplashDestination
    data object Main : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkAuth: CheckAuthUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.None)
    val destination: StateFlow<SplashDestination> = _destination

    init {
        viewModelScope.launch {
            delay(1_200)
            if (checkAuth()) {
                getCurrentUser() // populate UserSession
                _destination.value = SplashDestination.Main
            } else {
                _destination.value = SplashDestination.Login
            }
        }
    }
}
