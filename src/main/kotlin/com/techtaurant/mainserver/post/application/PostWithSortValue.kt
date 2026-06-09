package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.entity.Post

data class PostWithSortValue(
    val post: Post,
    val sortValue: Long,
) {
    val id
        get() = post.id
}
