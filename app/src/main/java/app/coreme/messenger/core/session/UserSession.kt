package app.coreme.messenger.core.session

import app.coreme.messenger.features.auth.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSession @Inject constructor() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val userId: String get() = _currentUser.value?.id ?: ""

    fun setUser(user: User) { _currentUser.value = user }
    fun clearUser() { _currentUser.value = null }
}
