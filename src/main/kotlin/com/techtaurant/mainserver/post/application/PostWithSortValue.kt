package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.entity.Post

internal data class PostWithSortValue(
    val post: Post,
    val sortValue: Long,
)
