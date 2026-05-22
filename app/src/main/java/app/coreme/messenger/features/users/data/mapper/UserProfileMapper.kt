package app.coreme.messenger.features.users.data.mapper

import app.coreme.messenger.features.users.data.dto.UserProfileDto
import app.coreme.messenger.features.users.domain.model.UserProfile
import java.time.Instant
import javax.inject.Inject

class UserProfileMapper @Inject constructor() {
    fun toDomain(dto: UserProfileDto) = UserProfile(
        id = dto.id,
        username = dto.username,
        displayName = dto.displayName,
        email = dto.email,
        bio = dto.bio,
        avatarUrl = dto.avatarUrl,
        isOnline = dto.isOnline,
        lastSeen = dto.lastSeen?.toInstantOrNull(),
        createdAt = dto.createdAt?.toInstantOrNull(),
    )

    private fun String.toInstantOrNull(): Instant? = try { Instant.parse(this) } catch (_: Exception) { null }
}
