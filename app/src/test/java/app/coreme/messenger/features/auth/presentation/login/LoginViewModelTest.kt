package app.coreme.messenger.features.auth.presentation.login

import app.coreme.messenger.features.auth.domain.model.User
import app.coreme.messenger.features.auth.domain.usecase.LoginUseCase
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
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var viewModel: LoginViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        loginUseCase = mockk()
        viewModel = LoginViewModel(loginUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty with no error`() {
        val state = viewModel.uiState.value
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isSuccess)
    }

    @Test
    fun `onUsernameChange updates username and clears error`() {
        viewModel.onUsernameChange("testuser")
        assertEquals("testuser", viewModel.uiState.value.username)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onPasswordChange updates password and clears error`() {
        viewModel.onPasswordChange("secret123")
        assertEquals("secret123", viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `login success sets isSuccess true`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Result.success(mockk<User>())

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.login()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `login failure sets error message`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Result.failure(Exception("Невірний пароль"))

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("wrongpass")
        viewModel.login()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Невірний пароль", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `login sets isLoading during execution`() = runTest {
        coEvery { loginUseCase(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(mockk<User>())
        }

        viewModel.login()
        dispatcher.scheduler.advanceTimeBy(50)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Result.failure(Exception("err"))
        viewModel.login()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onUsernameChange clears previous error`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Result.failure(Exception("err"))
        viewModel.login()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onUsernameChange("newuser")

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onPasswordChange clears previous error`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Result.failure(Exception("err"))
        viewModel.login()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onPasswordChange("newpass")

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `login passes correct credentials to use case`() = runTest {
        var capturedUsername = ""
        var capturedPassword = ""
        coEvery { loginUseCase(any(), any()) } answers {
            capturedUsername = firstArg()
            capturedPassword = secondArg()
            Result.success(mockk<User>())
        }

        viewModel.onUsernameChange("myuser")
        viewModel.onPasswordChange("mypass")
        viewModel.login()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("myuser", capturedUsername)
        assertEquals("mypass", capturedPassword)
    }
}
