package com.techtaurant.mainserver.comment.infrastructure.out

import com.techtaurant.mainserver.comment.entity.Comment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CommentRepository : JpaRepository<Comment, UUID> {
    /**
     * 게시물의 모든 댓글을 생성 시간 오름차순으로 조회합니다.
     * 대댓글은 부모 댓글 다음에 오도록 정렬됩니다.
     *
     * @param postId 게시물 ID
     * @return 댓글 목록 (생성 시간 오름차순)
     */
    fun findByPostIdOrderByCreatedAtAsc(postId: UUID): List<Comment>

    /**
     * 특정 게시물의 총 댓글 수를 조회합니다.
     *
     * @param postId 게시물 ID
     * @return 댓글 수 (대댓글 포함)
     */
    fun countByPostId(postId: UUID): Long
}
