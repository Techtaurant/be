package com.techtaurant.mainserver.blog.dto

import java.util.UUID

data class BlogCreateRequest(
    val name: String,
    val displayName: String?,
    val iconUrl: String?,
    val baseUrl: String?,
)

data class BlogUpdateRequest(
    val displayName: String?,
    val iconUrl: String?,
    val baseUrl: String?,
)

data class BlogResponse(
    val id: UUID,
    val name: String,
    val displayName: String?,
    val iconUrl: String?,
    val baseUrl: String?,
)
