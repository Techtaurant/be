package com.techtaurant.mainserver.comment.infrastructure.out

import com.techtaurant.mainserver.comment.entity.CommentLikeLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CommentLikeLogRepository : JpaRepository<CommentLikeLog, UUID> {
    /**
     * 특정 댓글에 대한 특정 사용자의 좋아요 로그를 조회합니다.
     * 기존 로그가 있는지 확인하여 UPDATE 또는 INSERT를 결정하는 데 사용됩니다.
     *
     * @param commentId 댓글 ID
     * @param userId 사용자 ID
     * @return 좋아요 로그 (없으면 null)
     */
    fun findByCommentIdAndUserId(
        commentId: UUID,
        userId: UUID,
    ): CommentLikeLog?
}
