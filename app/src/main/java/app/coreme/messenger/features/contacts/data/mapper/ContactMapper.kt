package app.coreme.messenger.features.contacts.data.mapper

import app.coreme.messenger.features.contacts.data.dto.ContactDto
import app.coreme.messenger.features.contacts.domain.model.Contact
import java.time.Instant
import javax.inject.Inject

class ContactMapper @Inject constructor() {
    fun toDomain(dto: ContactDto) = Contact(
        id = dto.id,
        nickname = dto.nickname,
        username = dto.username,
        displayName = dto.displayName,
        avatarUrl = dto.avatarUrl,
        isOnline = dto.isOnline,
        lastSeen = dto.lastSeen?.let { runCatching { Instant.parse(it) }.getOrNull() },
        bio = dto.bio,
    )
}
