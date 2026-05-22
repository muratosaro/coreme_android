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

class SearchUsersUseCaseTest {

    private lateinit var repository: UsersRepository
    private lateinit var useCase: SearchUsersUseCase

    private val fakeUsers = listOf(
        UserProfile(id = "1", username = "alice", displayName = "Alice"),
        UserProfile(id = "2", username = "alicia", displayName = "Alicia"),
    )

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = SearchUsersUseCase(repository)
    }

    @Test
    fun `invoke returns empty list when query is too short`() = runTest {
        val result = useCase("a")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<UserProfile>(), result.getOrNull())
        coVerify(exactly = 0) { repository.searchUsers(any()) }
    }

    @Test
    fun `invoke skips repository call for single char`() = runTest {
        useCase("x")
        coVerify(exactly = 0) { repository.searchUsers(any()) }
    }

    @Test
    fun `invoke calls repository when query length is 2 or more`() = runTest {
        coEvery { repository.searchUsers("al") } returns Result.success(fakeUsers)

        val result = useCase("al")

        assertTrue(result.isSuccess)
        assertEquals(fakeUsers, result.getOrNull())
        coVerify(exactly = 1) { repository.searchUsers("al") }
    }

    @Test
    fun `invoke returns results for longer query`() = runTest {
        coEvery { repository.searchUsers("alice") } returns Result.success(listOf(fakeUsers[0]))

        val result = useCase("alice")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("alice", result.getOrNull()?.first()?.username)
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        coEvery { repository.searchUsers(any()) } returns Result.failure(Exception("Network error"))

        val result = useCase("al")

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
