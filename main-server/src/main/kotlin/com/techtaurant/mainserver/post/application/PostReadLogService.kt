package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.entity.PostReadLog
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.post.infrastructure.out.PostReadLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 게시물 읽음 표시 서비스
 * 사용자의 게시물 읽음/안읽음 상태를 관리합니다.
 */
@Service
class PostReadLogService(
    private val postReadLogRepository: PostReadLogRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) {
    /**
     * 게시물 읽음 상태를 변경합니다.
     * 멱등성을 보장하여 이미 동일 상태인 경우 무시합니다.
     *
     * @param postId 대상 게시물 ID
     * @param userId 사용자 ID
     * @param isRead true: 읽음 표시 (레코드 생성), false: 안읽음 표시 (레코드 삭제)
     * @throws ApiException 게시물 또는 사용자가 존재하지 않는 경우
     */
    @Transactional
    fun toggleReadStatus(
        postId: UUID,
        userId: UUID,
        isRead: Boolean,
    ) {
        if (!postRepository.existsById(postId)) {
            throw ApiException(PostStatus.POST_NOT_FOUND)
        }

        val user =
            userRepository.findById(userId).orElseThrow {
                ApiException(UserStatus.ID_NOT_FOUND)
            }

        val existingLog = postReadLogRepository.findByPostIdAndUserId(postId, userId)

        if (isRead) {
            if (existingLog == null) {
                postReadLogRepository.save(PostReadLog(postId = postId, user = user))
            }
        } else {
            if (existingLog != null) {
                postReadLogRepository.delete(existingLog)
            }
        }
    }
}
