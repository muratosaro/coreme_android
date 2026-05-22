package app.coreme.messenger.features.users.domain.usecase

import app.coreme.messenger.features.users.domain.model.UserProfile
import app.coreme.messenger.features.users.domain.repository.UsersRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class UpdateProfileUseCaseTest {

    private lateinit var repository: UsersRepository
    private lateinit var useCase: UpdateProfileUseCase

    private val updatedProfile = UserProfile(
        id = "u1",
        username = "alice",
        displayName = "Alice Updated",
        email = "alice@test.com",
        bio = "Нова біо",
        avatarUrl = null,
        isOnline = false,
        lastSeen = Instant.now(),
    )

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = UpdateProfileUseCase(repository)
    }

    @Test
    fun `invoke returns updated profile on success`() = runTest {
        coEvery { repository.updateProfile(any(), any(), any()) } returns Result.success(updatedProfile)

        val result = useCase("Alice Updated", "Нова біо", "alice")

        assertTrue(result.isSuccess)
        assertEquals("Alice Updated", result.getOrNull()?.displayName)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        coEvery { repository.updateProfile(any(), any(), any()) } returns
            Result.failure(Exception("Помилка мережі"))

        val result = useCase("name", null, "user")

        assertTrue(result.isFailure)
        assertEquals("Помилка мережі", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke fails fast when displayName is blank`() = runTest {
        val result = useCase("   ", null, "user")
        assertTrue(result.isFailure)
        assertEquals("Display name cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke fails when username shorter than 3 chars`() = runTest {
        val result = useCase("Name", null, "ab")
        assertTrue(result.isFailure)
        assertEquals("Username must be at least 3 characters", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke with null displayName skips displayName validation`() = runTest {
        coEvery { repository.updateProfile(any(), any(), any()) } returns Result.success(updatedProfile)

        val result = useCase(null, "bio", "alice")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke trims displayName before passing to repository`() = runTest {
        coEvery { repository.updateProfile(any(), any(), any()) } returns Result.success(updatedProfile)

        useCase("  Alice  ", null, "alice")

        coVerify { repository.updateProfile("Alice", null, "alice") }
    }

    @Test
    fun `invoke trims bio before passing to repository`() = runTest {
        coEvery { repository.updateProfile(any(), any(), any()) } returns Result.success(updatedProfile)

        useCase("Alice", "  bio text  ", "alice")

        coVerify { repository.updateProfile("Alice", "bio text", "alice") }
    }
}
