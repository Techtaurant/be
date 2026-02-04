package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.entity.PostLikeLog
import com.techtaurant.mainserver.post.infrastructure.out.PostLikeLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import com.techtaurant.mainserver.user.enums.UserStatus
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
) {

    /**
     * 게시글 좋아요/싫어요 로그를 생성하거나 수정합니다.
     * 동일한 사용자의 기존 로그가 있으면 isLiked 값만 업데이트하고, 없으면 새로 생성합니다.
     *
     * @param postId 평가할 게시글 ID
     * @param userId 평가한 사용자 ID
     * @param isLiked true이면 좋아요, false이면 싫어요
     * @throws ApiException 게시글 또는 사용자가 존재하지 않는 경우
     */
    @Transactional
    fun recordLike(
        postId: UUID,
        userId: UUID,
        isLiked: Boolean,
    ) {
        val post = postRepository.findById(postId).orElseThrow {
            ApiException(PostStatus.POST_NOT_FOUND)
        }

        val user = userRepository.findById(userId).orElseThrow {
            ApiException(UserStatus.ID_NOT_FOUND)
        }

        val existingLog = postLikeLogRepository.findByPostIdAndUserId(postId, userId)

        if (existingLog != null) {
            existingLog.isLiked = isLiked
            postLikeLogRepository.save(existingLog)
        } else {
            val newLog = PostLikeLog(
                post = post,
                user = user,
                isLiked = isLiked,
            )
            postLikeLogRepository.save(newLog)
        }
    }
}
