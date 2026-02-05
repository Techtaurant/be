package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.lock.DistributedLock
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
    private val distributedLock: DistributedLock,
) {
    companion object {
        private const val MAX_CATEGORY_DEPTH = 5
    }

    /**
     * 게시물을 생성합니다.
     * 카테고리/태그 생성 시 락과 트랜잭션이 함께 관리되며, 게시물 저장은 별도 트랜잭션에서 수행됩니다.
     *
     * @param userId 작성자 ID
     * @param request 게시물 생성 요청
     * @return 생성된 게시물 응답
     * @throws ApiException 카테고리 depth 초과 시 CATEGORY_DEPTH_EXCEEDED
     */
    @Transactional
    fun createPost(
        userId: UUID,
        request: CreatePostRequest,
    ): PostResponse {
        val author = findUserById(userId)
        val category = resolveCategory(request.categoryPath, author)
        val tags = resolveTags(request.tags)

        val post =
            Post(
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
     * 존재하지 않는 카테고리는 자동으로 생성되며, 동시성 제어를 위해 락을 사용합니다.
     *
     * @param categoryPath 카테고리 경로 (예: "java/spring")
     * @param user 카테고리 소유자
     * @return 카테고리 엔티티 (경로가 없으면 null)
     */
    private fun resolveCategory(
        categoryPath: String?,
        user: User,
    ): Category? {
        if (categoryPath.isNullOrBlank()) {
            return null
        }

        val pathSegments = categoryPath.split("/").filter { it.isNotBlank() }

        if (pathSegments.isEmpty()) {
            return null
        }

        if (pathSegments.size > MAX_CATEGORY_DEPTH) {
            throw ApiException(PostStatus.CATEGORY_DEPTH_EXCEEDED)
        }

        var totalPath = ""
        var parentCategory: Category? = null

        for ((index, curPathSegment) in pathSegments.withIndex()) {
            totalPath = if (totalPath.isEmpty()) curPathSegment else "$totalPath/$curPathSegment"

            val lockKey = "category:${user.id}:$totalPath"
            parentCategory =
                distributedLock.withLockAndTransaction(lockKey) {
                    categoryRepository.findByUserAndPath(user, totalPath)
                        ?: categoryRepository.save(
                            Category(
                                user = user,
                                name = curPathSegment,
                                path = totalPath,
                                depth = index + 1,
                                parent = parentCategory,
                            ),
                        )
                }
        }

        return parentCategory
    }

    /**
     * 태그 이름 목록을 받아 태그 엔티티 목록을 반환합니다.
     * 존재하지 않는 태그는 자동으로 생성되며, 동시성 제어를 위해 락을 사용합니다.
     *
     * @param tagNames 태그 이름 목록
     * @return 태그 엔티티 목록
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

        val newTags =
            newTagNames.map { tagName ->
                val lockKey = "tag:$tagName"
                distributedLock.withLockAndTransaction(lockKey) {
                    tagRepository.findByName(tagName)
                        ?: tagRepository.save(Tag(name = tagName))
                }
            }

        return existingTags + newTags
    }
}
