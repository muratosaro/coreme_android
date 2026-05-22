package app.coreme.messenger.features.chats.presentation.newchat

import app.coreme.messenger.features.chats.domain.model.Chat
import app.coreme.messenger.features.chats.domain.model.ChatType
import app.coreme.messenger.features.chats.domain.usecase.CreateDirectChatUseCase
import app.coreme.messenger.features.users.domain.model.UserProfile
import app.coreme.messenger.features.users.domain.usecase.SearchUsersUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NewChatViewModelTest {

    private lateinit var searchUsers: SearchUsersUseCase
    private lateinit var createDirectChat: CreateDirectChatUseCase
    private lateinit var viewModel: NewChatViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val fakeUsers = listOf(
        UserProfile(id = "u1", username = "alice", displayName = "Alice", isOnline = true),
        UserProfile(id = "u2", username = "bob", displayName = "Bob"),
    )

    private val fakeChat = Chat(
        id = "c1", type = ChatType.DIRECT, createdAt = Instant.now(),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        searchUsers = mockk()
        createDirectChat = mockk()
        coEvery { searchUsers(any()) } returns Result.success(fakeUsers)
        coEvery { createDirectChat(any()) } returns Result.success(fakeChat)
        viewModel = NewChatViewModel(searchUsers, createDirectChat)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        assertTrue(viewModel.uiState.value.results.isEmpty())
        assertEquals("", viewModel.uiState.value.query)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `short query does not trigger search`() = runTest {
        viewModel.onQueryChange("a")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.results.isEmpty())
    }

    @Test
    fun `query of 2+ chars triggers search after debounce`() = runTest {
        viewModel.onQueryChange("al")
        advanceUntilIdle()

        assertEquals(fakeUsers, viewModel.uiState.value.results)
    }

    @Test
    fun `startChat sets createdChatId on success`() = runTest {
        viewModel.startChat("u1")
        advanceUntilIdle()

        assertEquals("c1", viewModel.uiState.value.createdChatId)
    }

    @Test
    fun `startChat sets error on failure`() = runTest {
        coEvery { createDirectChat(any()) } returns Result.failure(Exception("Network error"))
        viewModel.startChat("u1")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.createdChatId)
    }
}
