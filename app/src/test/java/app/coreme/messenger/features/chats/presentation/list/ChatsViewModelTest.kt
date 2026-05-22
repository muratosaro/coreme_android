package app.coreme.messenger.features.chats.presentation.list

import app.cash.turbine.test
import app.coreme.messenger.core.socket.SocketEvent
import app.coreme.messenger.core.socket.SocketManager
import app.coreme.messenger.features.chats.domain.model.Chat
import app.coreme.messenger.features.chats.domain.model.ChatType
import app.coreme.messenger.features.chats.domain.usecase.GetChatsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ChatsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var getChatsUseCase: GetChatsUseCase
    private lateinit var socketManager: SocketManager
    private lateinit var viewModel: ChatsViewModel

    private val _chatsFlow = MutableStateFlow<List<Chat>>(emptyList())
    private val chatsFlow: Flow<List<Chat>> = _chatsFlow

    private val _socketEvents = MutableSharedFlow<SocketEvent>()
    private val socketEvents: SharedFlow<SocketEvent> = _socketEvents

    private val fakeChat = Chat(
        id = "chat-1",
        type = ChatType.DIRECT,
        createdAt = Instant.now(),
        memberCount = 2,
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        getChatsUseCase = mockk()
        every { getChatsUseCase.invoke() } returns chatsFlow
        coEvery { getChatsUseCase.refresh() } returns Result.success(Unit)

        socketManager = mockk(relaxed = true)
        every { socketManager.events } returns socketEvents

        viewModel = ChatsViewModel(getChatsUseCase, socketManager)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty chats and no error`() {
        val state = viewModel.uiState.value
        assertTrue(state.chats.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `chats flow updates ui state`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            _chatsFlow.value = listOf(fakeChat)
            val updated = awaitItem()
            assertEquals(1, updated.chats.size)
            assertEquals("chat-1", updated.chats.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh failure sets error`() = runTest {
        coEvery { getChatsUseCase.refresh() } returns Result.failure(Exception("Network error"))

        viewModel.uiState.test {
            awaitItem()
            viewModel.refresh()
            val errorState = awaitItem()
            assertEquals("Network error", errorState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        coEvery { getChatsUseCase.refresh() } returns Result.failure(Exception("err"))
        viewModel.refresh()
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `multiple chats are shown`() = runTest {
        val chat2 = fakeChat.copy(id = "chat-2")
        _chatsFlow.value = listOf(fakeChat, chat2)

        val state = viewModel.uiState.value
        assertEquals(2, state.chats.size)
    }
}
