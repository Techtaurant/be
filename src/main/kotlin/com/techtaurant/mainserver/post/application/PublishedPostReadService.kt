package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PublishedPostReadService(
    private val postRepository: PostRepository,
) {
    fun getPublishedPostsByIds(postIds: List<UUID>): List<Post> {
        val normalizedPostIds = postIds.distinct()
        if (normalizedPostIds.isEmpty()) {
            return emptyList()
        }

        val postById = postRepository.findPublishedPostsByIdIn(normalizedPostIds).associateBy { it.id!! }

        return normalizedPostIds.mapNotNull { postById[it] }
    }

    fun getPublishedPostById(postId: UUID): Post? = getPublishedPostsByIds(listOf(postId)).firstOrNull()
}
