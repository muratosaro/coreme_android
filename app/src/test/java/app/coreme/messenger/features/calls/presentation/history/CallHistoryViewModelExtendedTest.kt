package app.coreme.messenger.features.calls.presentation.history

import app.coreme.messenger.core.notifications.NotificationHelper
import app.coreme.messenger.core.session.UserSession
import app.coreme.messenger.core.socket.SocketEvent
import app.coreme.messenger.core.socket.SocketManager
import app.coreme.messenger.features.calls.domain.model.Call
import app.coreme.messenger.features.calls.domain.model.CallStatus
import app.coreme.messenger.features.calls.domain.model.CallType
import app.coreme.messenger.features.calls.domain.usecase.GetCallHistoryUseCase
import app.coreme.messenger.features.calls.domain.usecase.InitiateCallUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
class CallHistoryViewModelExtendedTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var getCallHistory: GetCallHistoryUseCase
    private lateinit var initiateCall: InitiateCallUseCase
    private lateinit var socketManager: SocketManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var userSession: UserSession
    private lateinit var viewModel: CallHistoryViewModel

    private val socketEvents: SharedFlow<SocketEvent> = MutableSharedFlow()

    private fun makeCall(
        id: String,
        status: CallStatus = CallStatus.ANSWERED,
        type: CallType = CallType.VOICE,
    ) = Call(
        id = id,
        chatId = "chat-$id",
        callType = type,
        status = status,
        duration = 60,
        participantId = "other",
        participantName = "Other User",
        participantAvatarUrl = null,
        startedAt = Instant.now(),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        getCallHistory = mockk()
        initiateCall = mockk()
        socketManager = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)
        userSession = mockk()
        every { socketManager.events } returns socketEvents
        coEvery { getCallHistory() } returns Result.success(
            listOf(makeCall("1"), makeCall("2", CallStatus.MISSED)),
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads both answered and missed calls`() = runTest {
        viewModel = CallHistoryViewModel(getCallHistory, initiateCall, socketManager, notificationHelper, userSession)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.calls.size)
    }

    @Test
    fun `missed call is identified correctly`() = runTest {
        viewModel = CallHistoryViewModel(getCallHistory, initiateCall, socketManager, notificationHelper, userSession)
        dispatcher.scheduler.advanceUntilIdle()

        val missed = viewModel.uiState.value.calls.find { it.status == CallStatus.MISSED }
        assertEquals("2", missed?.id)
    }

    @Test
    fun `video call type is preserved`() = runTest {
        coEvery { getCallHistory() } returns Result.success(listOf(makeCall("v1", type = CallType.VIDEO)))
        viewModel = CallHistoryViewModel(getCallHistory, initiateCall, socketManager, notificationHelper, userSession)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(CallType.VIDEO, viewModel.uiState.value.calls.first().callType)
    }

    @Test
    fun `call method invokes socketManager initiateCall`() = runTest {
        viewModel = CallHistoryViewModel(getCallHistory, initiateCall, socketManager, notificationHelper, userSession)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.call("user-target", "voice")
        dispatcher.scheduler.advanceUntilIdle()

        verify { socketManager.initiateCall("user-target", "voice") }
    }

    @Test
    fun `call uses voice as default type`() = runTest {
        viewModel = CallHistoryViewModel(getCallHistory, initiateCall, socketManager, notificationHelper, userSession)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.call("user-x")
        dispatcher.scheduler.advanceUntilIdle()

        verify { socketManager.initiateCall("user-x", "voice") }
    }

    @Test
    fun `isLoading is false after successful load`() = runTest {
        viewModel = CallHistoryViewModel(getCallHistory, initiateCall, socketManager, notificationHelper, userSession)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `acceptCall delegates to socketManager`() = runTest {
        viewModel = CallHistoryViewModel(getCallHistory, initiateCall, socketManager, notificationHelper, userSession)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.acceptCall("call-1")

        verify { socketManager.acceptCall("call-1") }
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `rejectCall delegates to socketManager`() = runTest {
        viewModel = CallHistoryViewModel(getCallHistory, initiateCall, socketManager, notificationHelper, userSession)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.rejectCall("call-1")

        verify { socketManager.rejectCall("call-1") }
        assertNull(viewModel.uiState.value.error)
    }
}
