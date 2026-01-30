package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.post.dto.PostCursor
import com.techtaurant.mainserver.post.dto.PostListItemResponse
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostSortType
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 게시물 목록 조회 서비스
 */
@Service
@Transactional(readOnly = true)
class PostListReadService(
    private val postRepository: PostRepository,
) {

    /**
     * 게시물 목록을 커서 기반 페이지네이션으로 조회
     *
     * @param cursor 이전 응답의 nextCursor (null이면 첫 페이지)
     * @param size 페이지 크기
     * @param period 기간 필터 (WEEK, MONTH, YEAR, ALL)
     * @param sortType 정렬 기준 (LATEST, VIEW, LIKE, COMMENT)
     * @return 커서 기반 페이지 응답
     */
    fun getPosts(
        cursor: String?,
        size: Int,
        period: PostPeriod = PostPeriod.ALL,
        sortType: PostSortType = PostSortType.LATEST,
    ): CursorPageResponse<PostListItemResponse> {
        val postCursor = cursor?.let { PostCursor.decode(it) }

        if (cursor != null && postCursor == null) {
            return CursorPageResponse(
                content = emptyList(),
                nextCursor = null,
                hasNext = false,
                size = 0,
            )
        }

        val posts = postRepository.findPostsWithConditions(
            cursor = postCursor,
            size = size + 1,
            period = period,
            sortType = sortType,
        )

        val hasNext = posts.size > size
        val content = posts.take(size)

        val nextCursor = if (hasNext && content.isNotEmpty()) {
            PostCursor.from(content.last(), sortType).encode()
        } else {
            null
        }

        return CursorPageResponse(
            content = content.map { PostListItemResponse.from(it) },
            nextCursor = nextCursor,
            hasNext = hasNext,
            size = content.size,
        )
    }
}
