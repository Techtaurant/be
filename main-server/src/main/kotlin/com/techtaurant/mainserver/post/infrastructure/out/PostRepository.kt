package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.entity.Post
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Date
import java.util.UUID

interface PostRepository : JpaRepository<Post, UUID>, PostRepositoryCustom {

    /**
     * 커서 기반 페이징 (첫 페이지)
     * author와 tags를 JOIN FETCH하여 N+1 문제 방지
     */
    @Query("""
        SELECT DISTINCT p FROM Post p
        JOIN FETCH p.author
        LEFT JOIN FETCH p.tags
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    fun findPostsFirstPage(pageable: Pageable): List<Post>

    /**
     * 커서 기반 페이징 (다음 페이지)
     * 커서 위치 이후의 게시물만 조회
     *
     * @param cursorCreatedAt 커서 기준 생성일시
     * @param cursorId 커서 기준 게시물 ID
     */
    @Query("""
        SELECT DISTINCT p FROM Post p
        JOIN FETCH p.author
        LEFT JOIN FETCH p.tags
        WHERE (p.createdAt < :cursorCreatedAt)
           OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    fun findPostsAfterCursor(cursorCreatedAt: Date, cursorId: UUID, pageable: Pageable): List<Post>

    /**
     * 통계 동기화가 필요한 게시물 조회
     * statsUpdatedAt이 null이거나 updatedAt보다 이전인 게시물
     */
    @Query("""
        SELECT p FROM Post p
        WHERE p.statsUpdatedAt IS NULL OR p.statsUpdatedAt < p.updatedAt
    """)
    fun findPostsNeedingStatsSync(pageable: Pageable): List<Post>
}
