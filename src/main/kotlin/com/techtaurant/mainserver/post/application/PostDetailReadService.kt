package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.dto.PostDetailAttachmentPresignedUrlResponse
import com.techtaurant.mainserver.post.dto.PostDetailResponse
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.PostLikeLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostReadLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.user.application.UserProfileImageResolver
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 게시물 상세 조회 서비스
 *
 * 게시물 상세 정보를 조회하고, 조회 로그를 기록합니다.
 */
@Service
class PostDetailReadService(
    private val postRepository: PostRepository,
    private val postViewLogService: PostViewLogService,
    private val postLikeLogRepository: PostLikeLogRepository,
    private val postReadLogRepository: PostReadLogRepository,
    private val attachmentService: AttachmentService,
    private val userProfileImageResolver: UserProfileImageResolver,
) {
    /**
     * 게시물 상세 정보를 조회합니다.
     * 조회 시 자동으로 조회 로그를 기록합니다.
     * DRAFT/PRIVATE 상태의 게시물은 작성자만 조회할 수 있습니다.
     *
     * @param postId 게시물 ID
     * @param userId 조회한 사용자 ID (비회원인 경우 null)
     * @param ipAddress 클라이언트 IP 주소
     * @param userAgent 브라우저 User-Agent 정보
     * @return 게시물 상세 응답 DTO
     * @throws ApiException 게시물이 존재하지 않거나 권한이 없는 경우 POST_NOT_FOUND
     */
    @Transactional
    fun getPostDetail(
        postId: UUID,
        userId: UUID?,
        ipAddress: String?,
        userAgent: String?,
    ): PostDetailResponse {
        val post =
            postRepository.findVisiblePostDetailById(postId, userId)
                ?: throw ApiException(PostStatus.POST_NOT_FOUND)

        if (post.status != PostStatusEnum.PUBLISHED) {
            if (userId == null || post.author.id != userId) {
                throw ApiException(PostStatus.POST_NOT_FOUND)
            }
        }

        postViewLogService.recordView(
            postId = postId,
            userId = userId,
            ipAddress = ipAddress,
            userAgent = userAgent,
        )

        val likeStatus =
            userId?.let {
                val log = postLikeLogRepository.findByPostIdAndUserId(postId, it)
                when {
                    log == null -> LikeStatus.NONE
                    log.isLiked -> LikeStatus.LIKE
                    else -> LikeStatus.DISLIKE
                }
            } ?: LikeStatus.NONE

        val isRead =
            userId?.let {
                postReadLogRepository.existsByPostIdAndUserId(postId, it)
            } ?: false

        val attachmentPresignedUrls =
            attachmentService.generatePresignedDownloadUrlMapByReference(postId, AttachmentReferenceType.POST)
                .map { (attachmentId, presignedUrl) ->
                    PostDetailAttachmentPresignedUrlResponse.from(attachmentId, presignedUrl)
                }
        val authorProfileImageUrl = userProfileImageResolver.resolve(post.author)

        return PostDetailResponse.from(
            post = post,
            likeStatus = likeStatus,
            isRead = isRead,
            authorProfileImageUrl = authorProfileImageUrl,
            attachmentPresignedUrls = attachmentPresignedUrls,
        )
    }
}
