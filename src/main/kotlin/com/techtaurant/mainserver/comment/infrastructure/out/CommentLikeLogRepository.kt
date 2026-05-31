package com.techtaurant.mainserver.comment.infrastructure.out

import com.techtaurant.mainserver.comment.entity.CommentLikeLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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

    /**
     * 여러 댓글에 대한 특정 사용자의 좋아요 로그를 일괄 조회합니다.
     *
     * @param commentIds 댓글 ID 목록
     * @param userId 사용자 ID
     * @return 좋아요 로그 목록
     */
    fun findByCommentIdInAndUserId(
        commentIds: List<UUID>,
        userId: UUID,
    ): List<CommentLikeLog>

    @Query(
        """
        SELECT likeLog
        FROM CommentLikeLog likeLog
        JOIN FETCH likeLog.comment likedComment
        JOIN FETCH likedComment.post post
        WHERE likeLog.user.id = :userId
          AND post.author.id <> :userId
        """,
    )
    fun findAllByUserIdWithCommentsOnSurvivingPosts(
        @Param("userId") userId: UUID,
    ): List<CommentLikeLog>
}
