package com.techtaurant.mainserver.common.dto

data class CursorPageResponse<T>(
    val content: List<T>,
    val nextCursor: String?,
    val hasNext: Boolean,
    val size: Int,
)
