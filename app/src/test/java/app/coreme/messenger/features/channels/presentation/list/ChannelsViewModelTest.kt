package app.coreme.messenger.features.channels.presentation.list

import app.coreme.messenger.features.channels.domain.model.Channel
import app.coreme.messenger.features.channels.domain.usecase.GetChannelsUseCase
import app.coreme.messenger.features.channels.domain.usecase.ToggleSubscriptionUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelsViewModelTest {

    private lateinit var getChannels: GetChannelsUseCase
    private lateinit var toggleSubscription: ToggleSubscriptionUseCase
    private lateinit var viewModel: ChannelsViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val fakeChannels = listOf(
        Channel(id = "1", name = "Tech News", description = null, avatarUrl = null, subscriberCount = 100, isSubscribed = false, ownerId = "u1", lastPostAt = null),
        Channel(id = "2", name = "CoreMe Updates", description = "Official", avatarUrl = null, subscriberCount = 500, isSubscribed = true, ownerId = "u2", lastPostAt = null),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getChannels = mockk()
        toggleSubscription = mockk()
        coEvery { getChannels() } returns Result.success(fakeChannels)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads channels`() = runTest {
        viewModel = ChannelsViewModel(getChannels, toggleSubscription)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(fakeChannels, viewModel.uiState.value.channels)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `init shows error when load fails`() = runTest {
        coEvery { getChannels() } returns Result.failure(Exception("Server error"))
        viewModel = ChannelsViewModel(getChannels, toggleSubscription)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Server error", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.channels.isEmpty())
    }

    @Test
    fun `toggleSubscription optimistically updates isSubscribed`() = runTest {
        coEvery { toggleSubscription("1", false) } returns Result.success(Unit)
        viewModel = ChannelsViewModel(getChannels, toggleSubscription)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSubscription("1", false)
        testDispatcher.scheduler.advanceUntilIdle()

        val ch = viewModel.uiState.value.channels.first { it.id == "1" }
        assertTrue(ch.isSubscribed)
        assertEquals(101, ch.subscriberCount)
    }

    @Test
    fun `toggleSubscription reverts on failure`() = runTest {
        coEvery { toggleSubscription("2", true) } returns Result.failure(Exception("err"))
        viewModel = ChannelsViewModel(getChannels, toggleSubscription)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSubscription("2", true)
        testDispatcher.scheduler.advanceUntilIdle()

        val ch = viewModel.uiState.value.channels.first { it.id == "2" }
        assertTrue(ch.isSubscribed)
        assertEquals(500, ch.subscriberCount)
    }

    @Test
    fun `clearError resets error`() = runTest {
        coEvery { getChannels() } returns Result.failure(Exception("err"))
        viewModel = ChannelsViewModel(getChannels, toggleSubscription)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }
}
