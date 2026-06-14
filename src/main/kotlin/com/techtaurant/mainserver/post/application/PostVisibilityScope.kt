package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.enums.PostStatusEnum
import java.util.UUID

internal sealed class PostVisibilityScope {
    abstract val authorId: UUID?
    abstract val statuses: List<PostStatusEnum>?
    abstract val visibleToUserId: UUID?
    abstract val viewerId: UUID?
    abstract val appliesCategoryFilter: Boolean

    fun categoryIdOrNull(categoryId: UUID?): UUID? = categoryId.takeIf { appliesCategoryFilter }

    data class AllVisible(
        private val currentUserId: UUID?,
    ) : PostVisibilityScope() {
        override val authorId: UUID? = null
        override val statuses: List<PostStatusEnum>? = null
        override val visibleToUserId: UUID? = currentUserId
        override val viewerId: UUID? = currentUserId
        override val appliesCategoryFilter: Boolean = false
    }

    data class OwnVisible(
        private val userId: UUID,
    ) : PostVisibilityScope() {
        override val authorId: UUID = userId
        override val statuses: List<PostStatusEnum> = listOf(PostStatusEnum.PUBLISHED, PostStatusEnum.PRIVATE)
        override val visibleToUserId: UUID? = null
        override val viewerId: UUID = userId
        override val appliesCategoryFilter: Boolean = true
    }

    data class AuthorPublic(
        override val authorId: UUID,
        override val viewerId: UUID?,
    ) : PostVisibilityScope() {
        override val statuses: List<PostStatusEnum> = listOf(PostStatusEnum.PUBLISHED)
        override val visibleToUserId: UUID? = null
        override val appliesCategoryFilter: Boolean = true
    }

    companion object {
        fun from(
            currentUserId: UUID?,
            authorId: UUID?,
        ): PostVisibilityScope =
            when {
                authorId == null -> AllVisible(currentUserId)
                currentUserId == authorId -> OwnVisible(authorId)
                else -> AuthorPublic(authorId = authorId, viewerId = currentUserId)
            }
    }
}
