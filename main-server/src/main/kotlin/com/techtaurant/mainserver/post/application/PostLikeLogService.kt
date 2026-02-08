package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.entity.PostLikeLog
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.post.infrastructure.out.PostLikeLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 게시글 좋아요/싫어요 로그 생성 및 수정 서비스
 * 사용자의 게시글 평가 이벤트를 기록하여 실시간 통계 집계를 지원합니다.
 */
@Service
class PostLikeLogService(
    private val postLikeLogRepository: PostLikeLogRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val postDailyStatsService: PostDailyStatsService,
) {
    /**
     * 게시글 좋아요 상태를 기록합니다.
     * 동일한 사용자의 기존 로그가 있으면 상태를 전이하고, 없으면 새로 생성합니다.
     *
     * @param postId 평가할 게시글 ID
     * @param userId 평가한 사용자 ID
     * @param likeStatus 좋아요 상태 (NONE: 취소, LIKE: 좋아요, DISLIKE: 싫어요)
     * @throws ApiException 게시글 또는 사용자가 존재하지 않는 경우
     */
    @Transactional
    fun recordLike(
        postId: UUID,
        userId: UUID,
        likeStatus: LikeStatus,
    ) {
        val post =
            postRepository.findById(postId).orElseThrow {
                ApiException(PostStatus.POST_NOT_FOUND)
            }

        val user =
            userRepository.findById(userId).orElseThrow {
                ApiException(UserStatus.ID_NOT_FOUND)
            }

        val existingLog = postLikeLogRepository.findByPostIdAndUserId(postId, userId)

        if (existingLog != null) {
            val previousIsLiked = existingLog.isLiked

            when (likeStatus) {
                LikeStatus.NONE -> {
                    // 좋아요/싫어요 취소 → 로그 삭제, 카운트 복원
                    postLikeLogRepository.delete(existingLog)
                    updateLikeCount(postId, !previousIsLiked)
                }
                LikeStatus.LIKE -> {
                    if (!previousIsLiked) {
                        // DISLIKE → LIKE: +2
                        existingLog.isLiked = true
                        postLikeLogRepository.save(existingLog)
                        updateLikeCount(postId, true)
                        updateLikeCount(postId, true)
                    }
                }
                LikeStatus.DISLIKE -> {
                    if (previousIsLiked) {
                        // LIKE → DISLIKE: -2
                        existingLog.isLiked = false
                        postLikeLogRepository.save(existingLog)
                        updateLikeCount(postId, false)
                        updateLikeCount(postId, false)
                    }
                }
            }
        } else {
            when (likeStatus) {
                LikeStatus.NONE -> { /* 이미 중립 상태, 무시 */ }
                LikeStatus.LIKE -> {
                    postLikeLogRepository.save(PostLikeLog(post = post, user = user, isLiked = true))
                    updateLikeCount(postId, true)
                }
                LikeStatus.DISLIKE -> {
                    postLikeLogRepository.save(PostLikeLog(post = post, user = user, isLiked = false))
                    updateLikeCount(postId, false)
                }
            }
        }
    }

    /**
     * 좋아요 상태 변경에 따라 Post와 DailyStats의 likeCount를 원자적으로 갱신합니다.
     *
     * @param postId 게시물 ID
     * @param increment true이면 증가, false이면 감소
     */
    private fun updateLikeCount(
        postId: UUID,
        increment: Boolean,
    ) {
        if (increment) {
            postRepository.incrementLikeCount(postId)
            postDailyStatsService.incrementLikeCount(postId)
        } else {
            postRepository.decrementLikeCount(postId)
            postDailyStatsService.decrementLikeCount(postId)
        }
    }
}
