package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.dto.PostCursor
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostSortType
import java.util.UUID

data class PostListQueryCriteria(
    val cursor: PostCursor?,
    val size: Int,
    val period: PostPeriod,
    val sortType: PostSortType,
    val currentUserId: UUID?,
    val authorId: UUID?,
    val categoryId: UUID?,
    val tagIds: List<UUID>?,
) {
    val queryType: PostListQueryType
        get() =
            when {
                authorId == null -> PostListQueryType.ALL_VISIBLE
                currentUserId == authorId -> PostListQueryType.OWN_VISIBLE
                else -> PostListQueryType.AUTHOR_PUBLIC
            }

    val querySize: Int
        get() = size + 1
}
