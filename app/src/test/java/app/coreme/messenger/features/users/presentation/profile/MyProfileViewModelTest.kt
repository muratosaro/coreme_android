package app.coreme.messenger.features.users.presentation.profile

import app.coreme.messenger.features.auth.domain.usecase.LogoutUseCase
import app.coreme.messenger.features.users.domain.model.UserProfile
import app.coreme.messenger.features.users.domain.usecase.GetUserProfileUseCase
import app.coreme.messenger.features.users.domain.usecase.UpdateProfileUseCase
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class MyProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var getUserProfile: GetUserProfileUseCase
    private lateinit var updateProfile: UpdateProfileUseCase
    private lateinit var logoutUseCase: LogoutUseCase
    private lateinit var viewModel: MyProfileViewModel

    private val fakeProfile = UserProfile(
        id = "user-1",
        username = "alice",
        displayName = "Alice Smith",
        email = "alice@example.com",
        bio = "Розробник",
        avatarUrl = null,
        isOnline = true,
        lastSeen = Instant.now(),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        getUserProfile = mockk()
        updateProfile = mockk()
        logoutUseCase = mockk()
        coEvery { getUserProfile.getMyProfile() } returns Result.success(fakeProfile)
        viewModel = MyProfileViewModel(getUserProfile, updateProfile, logoutUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads profile successfully`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.profile)
        assertEquals("alice", viewModel.uiState.value.profile?.username)
        assertEquals("Alice Smith", viewModel.uiState.value.displayName)
        assertEquals("Розробник", viewModel.uiState.value.bio)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `init failure sets error`() = runTest {
        coEvery { getUserProfile.getMyProfile() } returns Result.failure(Exception("Помилка завантаження"))
        viewModel = MyProfileViewModel(getUserProfile, updateProfile, logoutUseCase)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Помилка завантаження", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `startEditing sets isEditing true`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.startEditing()

        assertTrue(viewModel.uiState.value.isEditing)
        assertFalse(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun `cancelEditing restores original values`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.startEditing()
        viewModel.onDisplayNameChange("Нове ім'я")
        viewModel.cancelEditing()

        assertFalse(viewModel.uiState.value.isEditing)
        assertEquals("Alice Smith", viewModel.uiState.value.displayName)
    }

    @Test
    fun `onDisplayNameChange updates displayName`() {
        viewModel.onDisplayNameChange("Bob")
        assertEquals("Bob", viewModel.uiState.value.displayName)
    }

    @Test
    fun `onBioChange updates bio`() {
        viewModel.onBioChange("Новий опис")
        assertEquals("Новий опис", viewModel.uiState.value.bio)
    }

    @Test
    fun `saveProfile success updates profile and exits editing`() = runTest {
        val updated = fakeProfile.copy(displayName = "Alice Updated")
        coEvery { updateProfile(any(), any(), any()) } returns Result.success(updated)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.startEditing()
        viewModel.onDisplayNameChange("Alice Updated")

        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditing)
        assertTrue(viewModel.uiState.value.saveSuccess)
        assertEquals("Alice Updated", viewModel.uiState.value.displayName)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `saveProfile failure sets error`() = runTest {
        coEvery { updateProfile(any(), any(), any()) } returns Result.failure(Exception("Помилка збереження"))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.startEditing()

        viewModel.saveProfile()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Помилка збереження", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `saveProfile sets isSaving during execution`() = runTest {
        coEvery { updateProfile(any(), any(), any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(fakeProfile)
        }
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.startEditing()

        viewModel.saveProfile()
        dispatcher.scheduler.advanceTimeBy(50)

        assertTrue(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `clearError removes error`() = runTest {
        coEvery { getUserProfile.getMyProfile() } returns Result.failure(Exception("err"))
        viewModel = MyProfileViewModel(getUserProfile, updateProfile, logoutUseCase)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `logout calls logoutUseCase`() = runTest {
        coJustRun { logoutUseCase() }
        dispatcher.scheduler.advanceUntilIdle()
        var logoutCalled = false

        viewModel.logout { logoutCalled = true }
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { logoutUseCase() }
        assertTrue(logoutCalled)
    }

    @Test
    fun `bio is empty string when profile has null bio`() = runTest {
        val profileNoBio = fakeProfile.copy(bio = null)
        coEvery { getUserProfile.getMyProfile() } returns Result.success(profileNoBio)
        viewModel = MyProfileViewModel(getUserProfile, updateProfile, logoutUseCase)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.bio)
    }
}
