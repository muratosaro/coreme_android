package app.coreme.messenger.features.contacts.presentation

import app.coreme.messenger.features.contacts.domain.model.Contact
import app.coreme.messenger.features.contacts.domain.usecase.AddContactUseCase
import app.coreme.messenger.features.contacts.domain.usecase.GetContactsUseCase
import app.coreme.messenger.features.contacts.domain.usecase.RemoveContactUseCase
import app.coreme.messenger.features.users.domain.usecase.SearchUsersUseCase
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsViewModelExtendedTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var getContacts: GetContactsUseCase
    private lateinit var searchUsers: SearchUsersUseCase
    private lateinit var addContact: AddContactUseCase
    private lateinit var removeContact: RemoveContactUseCase
    private lateinit var viewModel: ContactsViewModel

    private val fakeContact = Contact(
        id = "c1",
        username = "bob",
        displayName = "Bob",
        avatarUrl = null,
        isOnline = false,
    )

    private fun buildViewModel() = ContactsViewModel(getContacts, addContact, removeContact, searchUsers)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        getContacts = mockk()
        searchUsers = mockk()
        addContact = mockk()
        removeContact = mockk()
        coEvery { getContacts() } returns Result.success(emptyList())
        viewModel = buildViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty contacts before load completes`() {
        assertTrue(viewModel.uiState.value.contacts.isEmpty())
    }

    @Test
    fun `contacts loaded from use case on init`() = runTest {
        coEvery { getContacts() } returns Result.success(listOf(fakeContact))
        viewModel = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.contacts.size)
        assertEquals("bob", viewModel.uiState.value.contacts.first().username)
    }

    @Test
    fun `onSearchQueryChange updates query`() {
        viewModel.onSearchQueryChange("ali")
        assertEquals("ali", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `short search query does not trigger search`() = runTest {
        viewModel.onSearchQueryChange("a")
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun `search with 2+ chars triggers search`() = runTest {
        coEvery { searchUsers(any()) } returns Result.success(emptyList())
        viewModel.onSearchQueryChange("al")
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun `search failure sets error`() = runTest {
        coEvery { searchUsers(any()) } returns Result.failure(Exception("Пошук не вдався"))
        viewModel.onSearchQueryChange("ali")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Пошук не вдався", viewModel.uiState.value.error)
    }

    @Test
    fun `addContact failure sets error`() = runTest {
        coEvery { addContact(any()) } returns Result.failure(Exception("Не вдалося додати"))
        viewModel.addContact("user-x")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Не вдалося додати", viewModel.uiState.value.error)
    }

    @Test
    fun `removeContact failure sets error`() = runTest {
        coEvery { getContacts() } returns Result.success(listOf(fakeContact))
        viewModel = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        coEvery { removeContact(any()) } returns Result.failure(Exception("Не вдалося видалити"))
        viewModel.removeContact("c1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Не вдалося видалити", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.removingContact)
    }

    @Test
    fun `clearError nullifies error`() = runTest {
        coEvery { addContact(any()) } returns Result.failure(Exception("err"))
        viewModel.addContact("x")
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `multiple contacts all shown in state`() = runTest {
        val c2 = fakeContact.copy(id = "c2", username = "alice")
        coEvery { getContacts() } returns Result.success(listOf(fakeContact, c2))
        viewModel = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.contacts.size)
    }

    @Test
    fun `search returns results on success`() = runTest {
        val userProfile = mockk<app.coreme.messenger.features.users.domain.model.UserProfile>(relaxed = true)
        coEvery { searchUsers("alice") } returns Result.success(listOf(userProfile))

        viewModel.onSearchQueryChange("alice")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.searchResults.size)
    }
}
