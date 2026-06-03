package com.techtaurant.mainserver.post.infrastructure.out

import java.time.Instant
import java.util.UUID

interface TagWithPostCountProjection {
    fun getId(): UUID

    fun getName(): String

    fun getCreatedAt(): Instant

    fun getUpdatedAt(): Instant

    fun getPostCount(): Long
}
