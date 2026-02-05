package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.entity.PostViewLog
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostViewLogRepository
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 게시글 조회 로그 생성 서비스
 * 게시글 조회 이벤트를 기록하여 실시간 통계 집계를 지원합니다.
 */
@Service
class PostViewLogService(
    private val postViewLogRepository: PostViewLogRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) {
    /**
     * 게시글 조회 로그를 생성합니다.
     * 비회원 조회도 지원하며, userId가 null인 경우 비회원으로 간주합니다.
     *
     * @param postId 조회된 게시글 ID
     * @param userId 조회한 사용자 ID (비회원인 경우 null)
     * @param ipAddress 조회한 IP 주소
     * @param userAgent 브라우저 User-Agent 정보
     * @throws ApiException 게시글이 존재하지 않는 경우 POST_NOT_FOUND
     */
    @Transactional
    fun recordView(
        postId: UUID,
        userId: UUID?,
        ipAddress: String?,
        userAgent: String?,
    ) {
        val post =
            postRepository.findById(postId).orElseThrow {
                ApiException(PostStatus.POST_NOT_FOUND)
            }

        val user =
            userId?.let {
                userRepository.findById(it).orElse(null)
            }

        val viewLog =
            PostViewLog(
                post = post,
                user = user,
                ipAddress = ipAddress,
                userAgent = userAgent,
            )

        postViewLogRepository.save(viewLog)
    }
}
