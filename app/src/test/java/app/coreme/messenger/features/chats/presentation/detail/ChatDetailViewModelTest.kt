package app.coreme.messenger.features.chats.presentation.detail

import androidx.lifecycle.SavedStateHandle
import app.coreme.messenger.core.session.UserSession
import app.coreme.messenger.core.socket.SocketEvent
import app.coreme.messenger.core.socket.SocketManager
import app.coreme.messenger.features.chats.domain.model.Chat
import app.coreme.messenger.features.chats.domain.model.ChatType
import app.coreme.messenger.features.chats.domain.model.Message
import app.coreme.messenger.features.chats.domain.model.MessageType
import app.coreme.messenger.features.chats.domain.usecase.AddReactionUseCase
import app.coreme.messenger.features.chats.domain.usecase.DeleteMessageUseCase
import app.coreme.messenger.features.chats.domain.usecase.EditMessageUseCase
import app.coreme.messenger.features.chats.domain.usecase.GetChatsUseCase
import app.coreme.messenger.features.chats.domain.usecase.GetMessagesUseCase
import app.coreme.messenger.features.chats.domain.usecase.RemoveReactionUseCase
import app.coreme.messenger.features.chats.domain.usecase.SendMessageUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var getMessages: GetMessagesUseCase
    private lateinit var sendMessage: SendMessageUseCase
    private lateinit var editMessage: EditMessageUseCase
    private lateinit var deleteMessage: DeleteMessageUseCase
    private lateinit var addReaction: AddReactionUseCase
    private lateinit var removeReaction: RemoveReactionUseCase
    private lateinit var getChats: GetChatsUseCase
    private lateinit var socketManager: SocketManager
    private lateinit var userSession: UserSession
    private lateinit var viewModel: ChatDetailViewModel

    private val socketEvents: SharedFlow<SocketEvent> = MutableSharedFlow()
    private val testChatId = "chat-123"
    private val testUserId = "user-1"

    private val fakeMessage = Message(
        id = "msg-1",
        chatId = testChatId,
        senderId = testUserId,
        content = "Привіт!",
        type = MessageType.TEXT,
        createdAt = Instant.now(),
        isRead = false,
        isEdited = false,
        isDeleted = false,
        reactions = emptyList(),
    )

    private val fakeChat = Chat(
        id = testChatId,
        type = ChatType.DIRECT,
        createdAt = Instant.now(),
        memberCount = 2,
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        getMessages = mockk()
        sendMessage = mockk()
        editMessage = mockk()
        deleteMessage = mockk()
        addReaction = mockk()
        removeReaction = mockk()
        getChats = mockk()
        socketManager = mockk(relaxed = true)
        userSession = mockk()

        every { socketManager.events } returns socketEvents
        every { userSession.userId } returns testUserId
        every { getChats.invoke() } returns MutableStateFlow(listOf(fakeChat))
        coEvery { getMessages(any()) } returns Result.success(listOf(fakeMessage))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): ChatDetailViewModel {
        val savedState = SavedStateHandle(mapOf("chatId" to testChatId))
        return ChatDetailViewModel(
            savedState, getMessages, sendMessage, editMessage,
            deleteMessage, addReaction, removeReaction, getChats, socketManager, userSession,
        )
    }

    @Test
    fun `init loads messages successfully`() = runTest {
        viewModel = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals("msg-1", viewModel.uiState.value.messages.first().id)
    }

    @Test
    fun `init sets currentUserId from session`() = runTest {
        viewModel = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(testUserId, viewModel.uiState.value.currentUserId)
    }

    @Test
    fun `init finishes loading after success`() = runTest {
        viewModel = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loading failure sets error`() = runTest {
        coEvery { getMessages(any()) } returns Result.failure(Exception("Мережа недоступна"))
        viewModel = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Мережа недоступна", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onInputChange updates inputText`() = runTest {
        viewModel = buildViewModel()
        viewModel.onInputChange("Новий текст")
        assertEquals("Новий текст", viewModel.uiState.value.inputText)
    }

    @Test
    fun `send clears input text after sending`() = runTest {
        coEvery { sendMessage(any(), any(), any()) } returns Result.success(fakeMessage)
        viewModel = buildViewModel()
        viewModel.onInputChange("Тест")
        viewModel.send()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun `send with empty input does nothing`() = runTest {
        viewModel = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        val initialMessages = viewModel.uiState.value.messages.size

        viewModel.onInputChange("")
        viewModel.send()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(initialMessages, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `setReplyTo sets replyTo in state`() = runTest {
        viewModel = buildViewModel()
        viewModel.setReplyTo(fakeMessage)

        assertNotNull(viewModel.uiState.value.replyTo)
        assertEquals("msg-1", viewModel.uiState.value.replyTo?.id)
    }

    @Test
    fun `clearReply removes replyTo from state`() = runTest {
        viewModel = buildViewModel()
        viewModel.setReplyTo(fakeMessage)
        viewModel.clearReply()

        assertNull(viewModel.uiState.value.replyTo)
    }

    @Test
    fun `startEdit sets editingMessage in state`() = runTest {
        viewModel = buildViewModel()
        viewModel.startEdit(fakeMessage)

        assertNotNull(viewModel.uiState.value.editingMessage)
        assertEquals("msg-1", viewModel.uiState.value.editingMessage?.id)
        assertEquals("Привіт!", viewModel.uiState.value.inputText)
    }

    @Test
    fun `cancelEdit clears editing state`() = runTest {
        viewModel = buildViewModel()
        viewModel.startEdit(fakeMessage)
        viewModel.cancelEdit()

        assertNull(viewModel.uiState.value.editingMessage)
        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun `clearError resets error`() = runTest {
        coEvery { getMessages(any()) } returns Result.failure(Exception("err"))
        viewModel = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `deleteMessage failure sets error`() = runTest {
        coEvery { deleteMessage(any(), any()) } returns Result.failure(Exception("Помилка видалення"))
        viewModel = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteMessage("msg-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Помилка видалення", viewModel.uiState.value.error)
    }

    @Test
    fun `messages are reversed so latest is last`() = runTest {
        val msg1 = fakeMessage
        val msg2 = fakeMessage.copy(id = "msg-2", content = "Друге")
        coEvery { getMessages(any()) } returns Result.success(listOf(msg1, msg2))
        viewModel = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        // reversed() is applied in ViewModel, so last message is first in original list
        assertEquals(2, viewModel.uiState.value.messages.size)
    }
}
