package app.coreme.messenger.features.contacts.domain.usecase

import app.coreme.messenger.features.contacts.domain.repository.ContactsRepository
import javax.inject.Inject

class RemoveContactUseCase @Inject constructor(private val repository: ContactsRepository) {
    suspend operator fun invoke(contactId: String): Result<Unit> = repository.removeContact(contactId)
}
