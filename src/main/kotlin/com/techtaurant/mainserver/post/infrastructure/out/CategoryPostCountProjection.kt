package com.techtaurant.mainserver.post.infrastructure.out

import java.util.UUID

interface CategoryPostCountProjection {
    fun getCategoryId(): UUID

    fun getPostCount(): Long
}
