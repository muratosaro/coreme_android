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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsViewModelTest {

    private lateinit var getContacts: GetContactsUseCase
    private lateinit var addContact: AddContactUseCase
    private lateinit var removeContact: RemoveContactUseCase
    private lateinit var searchUsers: SearchUsersUseCase
    private lateinit var viewModel: ContactsViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val fakeContacts = listOf(
        Contact(id = "1", username = "alice", displayName = "Alice"),
        Contact(id = "2", username = "bob", displayName = "Bob"),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getContacts = mockk()
        addContact = mockk()
        removeContact = mockk()
        searchUsers = mockk()
        coEvery { getContacts() } returns Result.success(fakeContacts)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads contacts on creation`() = runTest {
        viewModel = ContactsViewModel(getContacts, addContact, removeContact, searchUsers)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(fakeContacts, viewModel.uiState.value.contacts)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `removeContact removes item from list optimistically`() = runTest {
        coEvery { removeContact.invoke("1") } returns Result.success(Unit)
        viewModel = ContactsViewModel(getContacts, addContact, removeContact, searchUsers)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.removeContact("1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.contacts.size)
        assertEquals("2", viewModel.uiState.value.contacts.first().id)
    }

    @Test
    fun `removeContact shows error on failure`() = runTest {
        coEvery { removeContact.invoke(any()) } returns Result.failure(Exception("Server error"))
        viewModel = ContactsViewModel(getContacts, addContact, removeContact, searchUsers)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.removeContact("1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Server error", viewModel.uiState.value.error)
        assertEquals(2, viewModel.uiState.value.contacts.size)
    }

    @Test
    fun `searchQuery shorter than 2 chars returns empty results without calling repository`() = runTest {
        viewModel = ContactsViewModel(getContacts, addContact, removeContact, searchUsers)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("a")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun `init shows error when getContacts fails`() = runTest {
        coEvery { getContacts() } returns Result.failure(Exception("Network error"))
        viewModel = ContactsViewModel(getContacts, addContact, removeContact, searchUsers)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Network error", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.contacts.isEmpty())
    }
}
