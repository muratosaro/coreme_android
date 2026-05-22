package app.coreme.messenger.features.chats.presentation.detail

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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ChatsViewModelExtendedTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var getChatsUseCase: GetChatsUseCase
    private lateinit var socketManager: SocketManager
    private lateinit var viewModel: app.coreme.messenger.features.chats.presentation.list.ChatsViewModel

    private val _chatsFlow = MutableStateFlow<List<Chat>>(emptyList())
    private val _socketEvents = MutableSharedFlow<SocketEvent>()
    private val socketEvents: SharedFlow<SocketEvent> = _socketEvents

    private fun makeChat(id: String, unread: Int = 0) = Chat(
        id = id,
        type = ChatType.DIRECT,
        createdAt = Instant.now(),
        memberCount = 2,
        unreadCount = unread,
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        getChatsUseCase = mockk()
        every { getChatsUseCase.invoke() } returns _chatsFlow
        coEvery { getChatsUseCase.refresh() } returns Result.success(Unit)

        socketManager = mockk(relaxed = true)
        every { socketManager.events } returns socketEvents

        viewModel = app.coreme.messenger.features.chats.presentation.list.ChatsViewModel(
            getChatsUseCase, socketManager,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `total unread count is sum of all chat unread counts`() = runTest {
        _chatsFlow.value = listOf(makeChat("1", 3), makeChat("2", 5), makeChat("3", 0))

        val totalUnread = viewModel.uiState.value.chats.sumOf { it.unreadCount }
        assertEquals(8, totalUnread)
    }

    @Test
    fun `chats with unread messages are correctly identified`() = runTest {
        _chatsFlow.value = listOf(makeChat("1", 2), makeChat("2", 0))

        val chatsWithUnread = viewModel.uiState.value.chats.filter { it.unreadCount > 0 }
        assertEquals(1, chatsWithUnread.size)
        assertEquals("1", chatsWithUnread.first().id)
    }

    @Test
    fun `refresh success clears isRefreshing`() = runTest {
        viewModel.refresh()
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `refresh failure shows error and clears refreshing`() = runTest {
        coEvery { getChatsUseCase.refresh() } returns Result.failure(Exception("Мережа"))
        viewModel.refresh()
        assertEquals("Мережа", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `clearError after refresh error removes error`() = runTest {
        coEvery { getChatsUseCase.refresh() } returns Result.failure(Exception("err"))
        viewModel.refresh()
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `empty chats list is valid state`() = runTest {
        _chatsFlow.value = emptyList()
        assertTrue(viewModel.uiState.value.chats.isEmpty())
    }

    @Test
    fun `chats are updated when flow emits new list`() = runTest {
        _chatsFlow.value = listOf(makeChat("chat-new"))
        assertEquals(1, viewModel.uiState.value.chats.size)
        assertEquals("chat-new", viewModel.uiState.value.chats.first().id)
    }

    @Test
    fun `group chat has type GROUP`() {
        val group = makeChat("g1").copy(type = ChatType.GROUP, memberCount = 10)
        assertEquals(ChatType.GROUP, group.type)
        assertEquals(10, group.memberCount)
    }
}
