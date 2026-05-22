package app.coreme.messenger.features.calls.presentation.history

import app.coreme.messenger.core.notifications.NotificationHelper
import app.coreme.messenger.core.session.UserSession
import app.coreme.messenger.core.socket.SocketManager
import app.coreme.messenger.features.calls.domain.model.Call
import app.coreme.messenger.features.calls.domain.model.CallStatus
import app.coreme.messenger.features.calls.domain.model.CallType
import app.coreme.messenger.features.calls.domain.usecase.GetCallHistoryUseCase
import app.coreme.messenger.features.calls.domain.usecase.InitiateCallUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import app.coreme.messenger.core.socket.SocketEvent
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CallHistoryViewModelTest {

    private lateinit var getCallHistory: GetCallHistoryUseCase
    private lateinit var initiateCall: InitiateCallUseCase
    private lateinit var socketManager: SocketManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var userSession: UserSession
    private lateinit var viewModel: CallHistoryViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val fakeCalls = listOf(
        Call(
            id = "1", chatId = "c1", callType = CallType.VOICE, status = CallStatus.ANSWERED,
            duration = 120, participantId = "u2", participantName = "Alice",
            participantAvatarUrl = null, startedAt = Instant.now(),
        ),
    )

    private val socketEvents: SharedFlow<SocketEvent> = MutableSharedFlow()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getCallHistory = mockk()
        initiateCall = mockk()
        socketManager = mockk()
        notificationHelper = mockk(relaxed = true)
        userSession = mockk()
        every { socketManager.events } returns socketEvents
        coEvery { getCallHistory() } returns Result.success(fakeCalls)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads call history`() = runTest {
        viewModel = CallHistoryViewModel(getCallHistory, initiateCall, socketManager, notificationHelper, userSession)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(fakeCalls, viewModel.uiState.value.calls)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `init shows error when history fails`() = runTest {
        coEvery { getCallHistory() } returns Result.failure(Exception("Network error"))
        viewModel = CallHistoryViewModel(getCallHistory, initiateCall, socketManager, notificationHelper, userSession)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Network error", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.calls.isEmpty())
    }

    @Test
    fun `init finishes in non-loading state after idle`() = runTest {
        viewModel = CallHistoryViewModel(getCallHistory, initiateCall, socketManager, notificationHelper, userSession)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!viewModel.uiState.value.isLoading)
        assertEquals(fakeCalls, viewModel.uiState.value.calls)
    }

    @Test
    fun `clearError resets error state`() = runTest {
        coEvery { getCallHistory() } returns Result.failure(Exception("err"))
        viewModel = CallHistoryViewModel(getCallHistory, initiateCall, socketManager, notificationHelper, userSession)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `incomingCall is null initially`() = runTest {
        viewModel = CallHistoryViewModel(getCallHistory, initiateCall, socketManager, notificationHelper, userSession)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.incomingCall.value)
    }
}
