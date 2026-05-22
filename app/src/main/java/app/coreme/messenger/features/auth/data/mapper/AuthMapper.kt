package app.coreme.messenger.features.auth.data.mapper

import app.coreme.messenger.features.auth.data.dto.UserDto
import app.coreme.messenger.features.auth.domain.model.User
import javax.inject.Inject

class AuthMapper @Inject constructor() {

    fun toDomain(dto: UserDto) = User(
        id = dto.id,
        username = dto.username,
        displayName = dto.displayName,
        email = dto.email,
        bio = dto.bio,
        avatarUrl = dto.avatarUrl,
        isOnline = dto.isOnline,
    )
}
