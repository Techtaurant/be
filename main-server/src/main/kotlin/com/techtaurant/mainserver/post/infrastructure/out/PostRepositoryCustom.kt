package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.dto.PostCursor
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostSortType

/**
 * 게시물 동적 쿼리를 위한 커스텀 Repository
 */
interface PostRepositoryCustom {
    /**
     * 동적 조건으로 게시물 목록 조회
     *
     * @param cursor 커서 (null이면 첫 페이지)
     * @param size 페이지 크기
     * @param period 기간 필터
     * @param sortType 정렬 타입
     * @return 게시물 목록
     */
    fun findPostsWithConditions(
        cursor: PostCursor?,
        size: Int,
        period: PostPeriod,
        sortType: PostSortType,
    ): List<Post>
}
