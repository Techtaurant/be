package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.entity.Post

interface PostListQueryStrategy {
    val queryType: PostListQueryType

    fun findPosts(criteria: PostListQueryCriteria): List<Post>
}
