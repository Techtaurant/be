package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.dto.CategoryResponse
import com.techtaurant.mainserver.post.infrastructure.out.CategoryRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 카테고리 조회 서비스
 */
@Service
@Transactional(readOnly = true)
class CategoryReadService(
    private val categoryRepository: CategoryRepository,
    private val postRepository: PostRepository,
) {
    /**
     * 카테고리 path prefix 검색
     *
     * @param userId 검색 대상 유저 ID
     * @param pathPrefix 카테고리 경로 prefix (null이면 전체 조회)
     * @return 해당 prefix로 시작하는 카테고리 목록
     */
    fun searchByPath(
        userId: UUID,
        pathPrefix: String?,
    ): List<CategoryResponse> {
        val categories =
            if (pathPrefix.isNullOrBlank()) {
                categoryRepository.findByUserId(userId)
            } else {
                categoryRepository.findByUserIdAndPathPrefix(userId, pathPrefix)
            }
        val postCountByCategoryId =
            if (categories.isEmpty()) {
                emptyMap()
            } else {
                postRepository.countByCategoryIds(categories.mapNotNull { it.id }).associate {
                    it.getCategoryId() to it.getPostCount()
                }
            }

        return categories.map { category ->
            CategoryResponse.from(
                category = category,
                postCount = postCountByCategoryId[category.id] ?: 0L,
            )
        }
    }
}
