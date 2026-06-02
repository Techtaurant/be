package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import org.springframework.stereotype.Component

@Component
internal class AllVisiblePostsQueryStrategy(
    private val postRepository: PostRepository,
) : PostListQueryStrategy {
    override val queryType: PostListQueryType = PostListQueryType.ALL_VISIBLE

    override fun findPosts(criteria: PostListQueryCriteria): List<PostWithSortValue> =
        postRepository.findPostsWithConditions(
            cursor = criteria.cursor,
            size = criteria.querySize,
            period = criteria.period,
            sortType = criteria.sortType,
            visibleToUserId = criteria.currentUserId,
            tagIds = criteria.tagIds,
            viewerId = criteria.currentUserId,
        )
}
