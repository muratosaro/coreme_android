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

class RegisterUseCaseTest {

    private lateinit var repository: AuthRepository
    private lateinit var useCase: RegisterUseCase

    private val fakeUser = User(id = "1", username = "john", displayName = "John Doe", email = "john@example.com")

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = RegisterUseCase(repository)
    }

    @Test
    fun `invoke returns success with valid inputs`() = runTest {
        coEvery { repository.register(any(), any(), any(), any()) } returns Result.success(fakeUser)

        val result = useCase("john", "john@example.com", "password123", "John Doe")

        assertTrue(result.isSuccess)
        assertEquals(fakeUser, result.getOrNull())
    }

    @Test
    fun `invoke fails when username is blank`() = runTest {
        val result = useCase("", "john@example.com", "password123", "John Doe")

        assertTrue(result.isFailure)
        assertEquals("Username cannot be empty", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.register(any(), any(), any(), any()) }
    }

    @Test
    fun `invoke fails when username is too short`() = runTest {
        val result = useCase("jo", "john@example.com", "password123", "John Doe")

        assertTrue(result.isFailure)
        assertEquals("Username must be at least 3 characters", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke fails when email has no at sign`() = runTest {
        val result = useCase("john", "notanemail", "password123", "John Doe")

        assertTrue(result.isFailure)
        assertEquals("Invalid email address", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.register(any(), any(), any(), any()) }
    }

    @Test
    fun `invoke fails when password is too short`() = runTest {
        val result = useCase("john", "john@example.com", "12345", "John Doe")

        assertTrue(result.isFailure)
        assertEquals("Password must be at least 6 characters", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke fails when display name is blank`() = runTest {
        val result = useCase("john", "john@example.com", "password123", "")

        assertTrue(result.isFailure)
        assertEquals("Display name cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke trims string fields before passing to repository`() = runTest {
        coEvery { repository.register(any(), any(), any(), any()) } returns Result.success(fakeUser)

        useCase("  john  ", "  john@example.com  ", "password123", "  John Doe  ")

        coVerify { repository.register("john", "john@example.com", "password123", "John Doe") }
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        val error = Exception("Username already taken")
        coEvery { repository.register(any(), any(), any(), any()) } returns Result.failure(error)

        val result = useCase("john", "john@example.com", "password123", "John Doe")

        assertTrue(result.isFailure)
        assertEquals("Username already taken", result.exceptionOrNull()?.message)
    }
}
