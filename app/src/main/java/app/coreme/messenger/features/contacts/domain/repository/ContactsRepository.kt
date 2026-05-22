package app.coreme.messenger.features.contacts.domain.repository

import app.coreme.messenger.features.contacts.domain.model.Contact

interface ContactsRepository {
    suspend fun getContacts(): Result<List<Contact>>
    suspend fun addContact(userId: String): Result<Contact>
    suspend fun removeContact(contactId: String): Result<Unit>
}
