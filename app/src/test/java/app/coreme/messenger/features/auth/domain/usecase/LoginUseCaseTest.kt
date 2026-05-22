package app.coreme.messenger.features.auth.domain.usecase

import app.coreme.messenger.features.auth.domain.model.User
import app.coreme.messenger.features.auth.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginUseCaseTest {

    private lateinit var repository: AuthRepository
    private lateinit var useCase: LoginUseCase

    private val fakeUser = User(id = "1", username = "john", displayName = "John")

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = LoginUseCase(repository)
    }

    @Test
    fun `invoke returns success when credentials are valid`() = runTest {
        coEvery { repository.login(any(), any()) } returns Result.success(fakeUser)

        val result = useCase("john", "password123")

        assertTrue(result.isSuccess)
        assertEquals(fakeUser, result.getOrNull())
        coVerify(exactly = 1) { repository.login("john", "password123") }
    }

    @Test
    fun `invoke fails when username is blank`() = runTest {
        val result = useCase("", "password123")

        assertTrue(result.isFailure)
        assertEquals("Username cannot be empty", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun `invoke fails when username is only whitespace`() = runTest {
        val result = useCase("   ", "password123")

        assertTrue(result.isFailure)
        assertEquals("Username cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke fails when password is too short`() = runTest {
        val result = useCase("john", "12345")

        assertTrue(result.isFailure)
        assertEquals("Password must be at least 6 characters", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun `invoke trims username before passing to repository`() = runTest {
        coEvery { repository.login("john", any()) } returns Result.success(fakeUser)

        useCase("  john  ", "password123")

        coVerify { repository.login("john", "password123") }
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        val error = Exception("Invalid credentials")
        coEvery { repository.login(any(), any()) } returns Result.failure(error)

        val result = useCase("john", "wrongpass")

        assertTrue(result.isFailure)
        assertEquals("Invalid credentials", result.exceptionOrNull()?.message)
    }
}
