package app.coreme.messenger.features.contacts.domain.usecase

import app.coreme.messenger.features.contacts.domain.model.Contact
import app.coreme.messenger.features.contacts.domain.repository.ContactsRepository
import javax.inject.Inject

class AddContactUseCase @Inject constructor(private val repository: ContactsRepository) {
    suspend operator fun invoke(userId: String): Result<Contact> = repository.addContact(userId)
}
