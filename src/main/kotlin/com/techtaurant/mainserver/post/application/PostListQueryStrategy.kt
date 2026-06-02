package com.techtaurant.mainserver.post.application

interface PostListQueryStrategy {
    val queryType: PostListQueryType

    fun findPosts(criteria: PostListQueryCriteria): List<PostWithSortValue>
}
