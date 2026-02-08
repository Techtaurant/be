package com.techtaurant.mainserver.comment.application

import com.techtaurant.mainserver.comment.entity.CommentLikeLog
import com.techtaurant.mainserver.comment.enums.CommentStatus
import com.techtaurant.mainserver.comment.infrastructure.out.CommentLikeLogRepository
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepository
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * 댓글 좋아요/싫어요 로그 생성 및 수정 서비스
 * 사용자의 댓글 평가 이벤트를 기록하여 실시간 통계 집계를 지원합니다.
 */
@Service
class CommentLikeLogService(
    private val commentLikeLogRepository: CommentLikeLogRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
) {
    /**
     * 댓글 좋아요/싫어요 로그를 생성하거나 수정합니다.
     * 동일한 사용자의 기존 로그가 있으면 isLiked 값만 업데이트하고, 없으면 새로 생성합니다.
     *
     * @param commentId 평가할 댓글 ID
     * @param userId 평가한 사용자 ID
     * @param isLiked true이면 좋아요, false이면 싫어요
     * @throws ApiException 댓글 또는 사용자가 존재하지 않는 경우
     */
    @Transactional
    fun recordLike(
        commentId: UUID,
        userId: UUID,
        isLiked: Boolean,
    ) {
        val comment =
            commentRepository.findById(commentId).orElseThrow {
                ApiException(CommentStatus.COMMENT_NOT_FOUND)
            }

        val user =
            userRepository.findById(userId).orElseThrow {
                ApiException(UserStatus.ID_NOT_FOUND)
            }

        val existingLog = commentLikeLogRepository.findByCommentIdAndUserId(commentId, userId)

        if (existingLog != null) {
            val previousIsLiked = existingLog.isLiked
            if (previousIsLiked != isLiked) {
                existingLog.isLiked = isLiked
                commentLikeLogRepository.save(existingLog)

                // 좋아요/싫어요 상태 변경 시:
                // 1. 이전 상태 취소 (±1)
                // 2. 새 상태 적용 (±1)
                // 총 변경량: ±2
                //
                // 예시:
                // - 좋아요(true) → 싫어요(false): -1(취소) + -1(싫어요) = -2
                // - 싫어요(false) → 좋아요(true): +1(취소) + +1(좋아요) = +2
                updateLikeCount(commentId, isLiked) // 이전 상태 취소
                updateLikeCount(commentId, isLiked) // 새 상태 적용
            }
        } else {
            val newLog =
                CommentLikeLog(
                    comment = comment,
                    user = user,
                    isLiked = isLiked,
                )
            commentLikeLogRepository.save(newLog)

            // 중립 상태에서 좋아요/싫어요 적용
            if (isLiked) {
                updateLikeCount(commentId, true) // 좋아요 +1
            } else {
                updateLikeCount(commentId, false) // 싫어요 -1
            }
        }
    }

    /**
     * 좋아요 상태 변경에 따라 Comment의 likeCount를 원자적으로 갱신합니다.
     *
     * @param commentId 댓글 ID
     * @param increment true이면 증가, false이면 감소
     */
    private fun updateLikeCount(
        commentId: UUID,
        increment: Boolean,
    ) {
        if (increment) {
            commentRepository.incrementLikeCount(commentId)
        } else {
            commentRepository.decrementLikeCount(commentId)
        }
    }
}
