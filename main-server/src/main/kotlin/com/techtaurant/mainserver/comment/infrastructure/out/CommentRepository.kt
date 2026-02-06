package com.techtaurant.mainserver.comment.infrastructure.out

import com.techtaurant.mainserver.comment.entity.Comment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CommentRepository : JpaRepository<Comment, UUID>, CommentRepositoryCustom {
    /**
     * 특정 게시물의 총 댓글 수를 조회합니다.
     *
     * @param postId 게시물 ID
     * @return 댓글 수 (대댓글 포함)
     */
    fun countByPostId(postId: UUID): Long
}
