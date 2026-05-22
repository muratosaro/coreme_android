package app.coreme.messenger.features.contacts.data.repository

import app.coreme.messenger.core.network.safeApiCall
import app.coreme.messenger.features.contacts.data.api.ContactsApi
import app.coreme.messenger.features.contacts.data.dto.AddContactRequest
import app.coreme.messenger.features.contacts.data.mapper.ContactMapper
import app.coreme.messenger.features.contacts.domain.model.Contact
import app.coreme.messenger.features.contacts.domain.repository.ContactsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val api: ContactsApi,
    private val mapper: ContactMapper,
) : ContactsRepository {

    override suspend fun getContacts(): Result<List<Contact>> =
        safeApiCall { api.getContacts().map { mapper.toDomain(it) } }

    override suspend fun addContact(userId: String): Result<Contact> =
        safeApiCall { mapper.toDomain(api.addContact(AddContactRequest(contactId = userId))) }

    override suspend fun removeContact(contactId: String): Result<Unit> =
        safeApiCall { api.removeContact(contactId) }
}
