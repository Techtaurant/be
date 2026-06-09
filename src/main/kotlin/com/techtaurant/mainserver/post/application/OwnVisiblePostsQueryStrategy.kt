package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import org.springframework.stereotype.Component

@Component
internal class OwnVisiblePostsQueryStrategy(
    private val postRepository: PostRepository,
) : PostListQueryStrategy {
    override val queryType: PostListQueryType = PostListQueryType.OWN_VISIBLE

    override fun findPosts(criteria: PostListQueryCriteria): List<PostWithSortValue> =
        postRepository.findPostsWithConditions(
            cursor = criteria.cursor,
            size = criteria.querySize,
            period = criteria.period,
            sortType = criteria.sortType,
            authorId = criteria.authorId!!,
            statuses = listOf(PostStatusEnum.PUBLISHED, PostStatusEnum.PRIVATE),
            categoryId = criteria.categoryId,
            tagIds = criteria.tagIds,
            viewerId = criteria.currentUserId,
        )
}
