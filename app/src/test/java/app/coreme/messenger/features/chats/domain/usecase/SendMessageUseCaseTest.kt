package app.coreme.messenger.features.chats.domain.usecase

import app.coreme.messenger.features.chats.domain.model.Message
import app.coreme.messenger.features.chats.domain.model.MessageType
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

class SendMessageUseCaseTest {

    private lateinit var repository: ChatsRepository
    private lateinit var useCase: SendMessageUseCase

    private val fakeMessage = Message(
        id = "msg-1",
        chatId = "chat-1",
        senderId = "user-1",
        type = MessageType.TEXT,
        content = "Hello",
        isRead = false,
        createdAt = Instant.now(),
        isEdited = false,
        isDeleted = false,
    )

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = SendMessageUseCase(repository)
    }

    @Test
    fun `invoke returns success with valid content`() = runTest {
        coEvery { repository.sendMessage(any(), any(), any()) } returns Result.success(fakeMessage)

        val result = useCase("chat-1", "Hello")

        assertTrue(result.isSuccess)
        assertEquals(fakeMessage, result.getOrNull())
    }

    @Test
    fun `invoke fails when content is blank`() = runTest {
        val result = useCase("chat-1", "   ")

        assertTrue(result.isFailure)
        assertEquals("Message cannot be empty", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `invoke trims content before sending`() = runTest {
        coEvery { repository.sendMessage(any(), "Hello", any()) } returns Result.success(fakeMessage)

        useCase("chat-1", "  Hello  ")

        coVerify { repository.sendMessage("chat-1", "Hello", null) }
    }

    @Test
    fun `invoke passes replyToId`() = runTest {
        coEvery { repository.sendMessage(any(), any(), any()) } returns Result.success(fakeMessage)

        useCase("chat-1", "Hello", replyToId = "msg-0")

        coVerify { repository.sendMessage("chat-1", "Hello", "msg-0") }
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        coEvery { repository.sendMessage(any(), any(), any()) } returns Result.failure(Exception("No internet"))

        val result = useCase("chat-1", "Hello")

        assertTrue(result.isFailure)
        assertEquals("No internet", result.exceptionOrNull()?.message)
    }
}
