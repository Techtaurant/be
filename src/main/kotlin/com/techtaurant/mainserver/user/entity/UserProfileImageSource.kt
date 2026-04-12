package com.techtaurant.mainserver.user.entity

import java.util.UUID

sealed interface UserProfileImageSource {
    data class ServiceAttachment(
        val attachmentId: UUID,
    ) : UserProfileImageSource

    data class Url(
        val url: String,
    ) : UserProfileImageSource
}
