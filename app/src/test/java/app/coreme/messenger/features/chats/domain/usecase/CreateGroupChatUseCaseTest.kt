package app.coreme.messenger.features.chats.domain.usecase

import app.coreme.messenger.features.chats.domain.model.Chat
import app.coreme.messenger.features.chats.domain.model.ChatType
import app.coreme.messenger.features.chats.domain.repository.ChatsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class CreateGroupChatUseCaseTest {

    private lateinit var repository: ChatsRepository
    private lateinit var useCase: CreateGroupChatUseCase

    private val fakeChat = Chat(id = "g1", type = ChatType.GROUP, name = "Team", createdAt = Instant.now())

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = CreateGroupChatUseCase(repository)
        coEvery { repository.createGroupChat(any(), any()) } returns Result.success(fakeChat)
    }

    @Test
    fun `returns failure when name is blank`() = runTest {
        val result = useCase("", listOf("u1"))

        assertTrue(result.isFailure)
        assertEquals("Group name is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun `returns failure when members empty`() = runTest {
        val result = useCase("Team", emptyList())

        assertTrue(result.isFailure)
        assertEquals("Select at least one member", result.exceptionOrNull()?.message)
    }

    @Test
    fun `delegates to repository on valid input`() = runTest {
        val result = useCase("Team", listOf("u1", "u2"))

        assertTrue(result.isSuccess)
        assertEquals(fakeChat, result.getOrNull())
        coVerify { repository.createGroupChat("Team", listOf("u1", "u2")) }
    }

    @Test
    fun `propagates repository failure`() = runTest {
        coEvery { repository.createGroupChat(any(), any()) } returns Result.failure(Exception("Server error"))

        val result = useCase("Team", listOf("u1"))

        assertTrue(result.isFailure)
        assertEquals("Server error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `name with only spaces is treated as blank`() = runTest {
        val result = useCase("   ", listOf("u1"))

        assertTrue(result.isFailure)
    }
}
