package app.coreme.messenger.features.auth.presentation.register

import app.coreme.messenger.features.auth.domain.model.User
import app.coreme.messenger.features.auth.domain.usecase.RegisterUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var registerUseCase: RegisterUseCase
    private lateinit var viewModel: RegisterViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        registerUseCase = mockk()
        viewModel = RegisterViewModel(registerUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is blank with no error`() {
        val state = viewModel.uiState.value
        assertEquals("", state.username)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.displayName)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isSuccess)
    }

    @Test
    fun `onUsernameChange updates field and clears error`() {
        viewModel.onUsernameChange("alice")
        assertEquals("alice", viewModel.uiState.value.username)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onEmailChange updates field and clears error`() {
        viewModel.onEmailChange("alice@example.com")
        assertEquals("alice@example.com", viewModel.uiState.value.email)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onPasswordChange updates field and clears error`() {
        viewModel.onPasswordChange("pass123")
        assertEquals("pass123", viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onDisplayNameChange updates field and clears error`() {
        viewModel.onDisplayNameChange("Alice Smith")
        assertEquals("Alice Smith", viewModel.uiState.value.displayName)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `register success sets isSuccess true and clears loading`() = runTest {
        coEvery { registerUseCase(any(), any(), any(), any()) } returns Result.success(mockk<User>())
        fillValidForm()

        viewModel.register()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `register failure sets error and clears loading`() = runTest {
        coEvery { registerUseCase(any(), any(), any(), any()) } returns
            Result.failure(Exception("Ім'я користувача вже зайняте"))
        fillValidForm()

        viewModel.register()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Ім'я користувача вже зайняте", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `register sets isLoading while running`() = runTest {
        coEvery { registerUseCase(any(), any(), any(), any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(mockk<User>())
        }
        fillValidForm()

        viewModel.register()
        dispatcher.scheduler.advanceTimeBy(50)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `clearError nullifies error state`() = runTest {
        coEvery { registerUseCase(any(), any(), any(), any()) } returns Result.failure(Exception("err"))
        viewModel.register()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `register passes all four fields to use case`() = runTest {
        var u = ""; var e = ""; var p = ""; var d = ""
        coEvery { registerUseCase(any(), any(), any(), any()) } answers {
            u = firstArg(); e = secondArg(); p = thirdArg(); d = args[3] as String
            Result.success(mockk<User>())
        }
        viewModel.onUsernameChange("bob")
        viewModel.onEmailChange("bob@b.com")
        viewModel.onPasswordChange("pw")
        viewModel.onDisplayNameChange("Bob")
        viewModel.register()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("bob", u)
        assertEquals("bob@b.com", e)
        assertEquals("pw", p)
        assertEquals("Bob", d)
    }

    private fun fillValidForm() {
        viewModel.onUsernameChange("testuser")
        viewModel.onEmailChange("test@test.com")
        viewModel.onPasswordChange("password")
        viewModel.onDisplayNameChange("Test User")
    }
}
