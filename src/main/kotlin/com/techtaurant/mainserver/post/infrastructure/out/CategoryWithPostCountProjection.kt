package com.techtaurant.mainserver.post.infrastructure.out

import java.util.UUID

interface CategoryWithPostCountProjection {
    fun getId(): UUID

    fun getName(): String

    fun getPath(): String

    fun getDepth(): Int

    fun getParentId(): UUID?

    fun getPostCount(): Long
}
