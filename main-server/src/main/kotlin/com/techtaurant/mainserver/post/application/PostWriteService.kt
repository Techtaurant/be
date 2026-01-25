package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.dto.CreatePostRequest
import com.techtaurant.mainserver.post.dto.PostResponse
import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.Tag
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.post.infrastructure.out.CategoryRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.post.infrastructure.out.TagRepository
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PostWriteService(
    private val postRepository: PostRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val userRepository: UserRepository,
) {

    companion object {
        private const val MAX_CATEGORY_DEPTH = 5
    }

    /**
     * 게시물을 생성합니다.
     *
     * @param userId 작성자 ID
     * @param request 게시물 생성 요청
     * @return 생성된 게시물 응답
     * @throws ApiException 카테고리 depth 초과 시 CATEGORY_DEPTH_EXCEEDED
     */
    @Transactional
    fun createPost(userId: UUID, request: CreatePostRequest): PostResponse {
        val author = findUserById(userId)
        val category = resolveCategory(request.categoryPath)
        val tags = resolveTags(request.tags)

        val post = Post(
            title = request.title,
            content = request.content,
            author = author,
            category = category,
            tags = tags.toMutableSet(),
        )

        val savedPost = postRepository.save(post)
        return PostResponse.from(savedPost)
    }

    private fun findUserById(userId: UUID): User {
        return userRepository.findById(userId).orElseThrow {
            ApiException(UserStatus.ID_NOT_FOUND)
        }
    }

    /**
     * 카테고리 경로를 파싱하여 해당 카테고리를 반환합니다.
     * 존재하지 않는 카테고리는 자동으로 생성됩니다.
     */
    private fun resolveCategory(categoryPath: String?): Category? {
        if (categoryPath.isNullOrBlank()) {
            return null
        }

        val segments = categoryPath.split("/").filter { it.isNotBlank() }

        if (segments.isEmpty()) {
            return null
        }

        if (segments.size > MAX_CATEGORY_DEPTH) {
            throw ApiException(PostStatus.CATEGORY_DEPTH_EXCEEDED)
        }

        var currentPath = ""
        var parentCategory: Category? = null

        for ((index, segment) in segments.withIndex()) {
            currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"

            val existingCategory = categoryRepository.findByPath(currentPath)
            parentCategory = existingCategory ?: categoryRepository.save(
                Category(
                    name = segment,
                    path = currentPath,
                    depth = index + 1,
                    parent = parentCategory,
                )
            )
        }

        return parentCategory
    }

    /**
     * 태그 이름 목록을 받아 태그 엔티티 목록을 반환합니다.
     * 존재하지 않는 태그는 자동으로 생성됩니다.
     */
    private fun resolveTags(tagNames: List<String>?): List<Tag> {
        if (tagNames.isNullOrEmpty()) {
            return emptyList()
        }

        val normalizedNames = tagNames.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()

        if (normalizedNames.isEmpty()) {
            return emptyList()
        }

        val existingTags = tagRepository.findByNameIn(normalizedNames)
        val existingTagNames = existingTags.map { it.name }.toSet()
        val newTagNames = normalizedNames.filter { it !in existingTagNames }

        val newTags = newTagNames.map { tagRepository.save(Tag(name = it)) }

        return existingTags + newTags
    }
}
