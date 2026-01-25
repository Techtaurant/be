package com.techtaurant.mainserver.post.infrastructure.out

import java.time.LocalDateTime
import java.util.UUID

interface TagWithPostCountProjection {
    fun getId(): UUID
    fun getName(): String
    fun getCreatedAt(): LocalDateTime
    fun getUpdatedAt(): LocalDateTime
    fun getPostCount(): Long
}
