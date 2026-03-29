package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.entity.Attachment
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.post.dto.DraftListItemResponse
import com.techtaurant.mainserver.post.dto.PostCursor
import com.techtaurant.mainserver.post.dto.PostListItemResponse
import com.techtaurant.mainserver.post.dto.PostListTagResponse
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostSortType
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.PostReadLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Calendar
import java.util.Date
import java.util.UUID

/**
 * 게시물 목록 조회 서비스
 */
@Service
@Transactional(readOnly = true)
class PostListReadService(
    private val postRepository: PostRepository,
    private val postReadLogRepository: PostReadLogRepository,
    private val attachmentService: AttachmentService,
    @param:Value("\${app.default-post-thumbnail-url}")
    private val defaultThumbnailUrl: String,
    @param:Value("\${swagger.base-url}")
    private val baseUrl: String,
) {
    companion object {
        private const val STALE_DRAFT_DAYS = 14
        private const val POST_LIST_CONTENT_MAX_LENGTH = 2000
    }

    /**
     * 게시물 목록을 커서 기반 페이지네이션으로 조회
     *
     * authorId 지정 시 해당 사용자의 게시물만 조회하며, 본인 조회 시 DRAFT/PRIVATE 포함, 타인 조회 시 PUBLISHED만 반환.
     * authorId 미지정 시 전체 게시물 조회 (로그인 사용자의 DRAFT/PRIVATE 포함).
     *
     * @param cursor 이전 응답의 nextCursor (null이면 첫 페이지)
     * @param size 페이지 크기
     * @param period 기간 필터 (WEEK, MONTH, YEAR, ALL)
     * @param sortType 정렬 기준 (LATEST, VIEW, LIKE, COMMENT)
     * @param currentUserId 현재 로그인 사용자 ID (비회원이면 null)
     * @param authorId 작성자 필터 (null이면 전체 조회)
     * @param categoryId 카테고리 필터 (null이면 전체, authorId 지정 시에만 적용)
     * @param tagIds 태그 UUID 필터 (여러 개 전달 시 OR 조건)
     * @return 커서 기반 페이지 응답
     */
    fun getPosts(
        cursor: String?,
        size: Int,
        period: PostPeriod = PostPeriod.ALL,
        sortType: PostSortType = PostSortType.LATEST,
        currentUserId: UUID? = null,
        authorId: UUID? = null,
        categoryId: UUID? = null,
        tagIds: List<UUID>? = null,
    ): CursorPageResponse<PostListItemResponse> {
        val postCursor = cursor?.let { PostCursor.decode(it) }
        val normalizedTagIds = normalizeTagIds(tagIds)

        if (cursor != null && postCursor == null) {
            return CursorPageResponse(
                content = emptyList(),
                nextCursor = null,
                hasNext = false,
                size = 0,
            )
        }

        val posts =
            if (authorId != null) {
                val statuses =
                    if (currentUserId == authorId) {
                        PostStatusEnum.entries
                    } else {
                        listOf(PostStatusEnum.PUBLISHED)
                    }
                postRepository.findPostsWithConditions(
                    cursor = postCursor,
                    size = size + 1,
                    period = period,
                    sortType = sortType,
                    authorId = authorId,
                    statuses = statuses,
                    categoryId = categoryId,
                    tagIds = normalizedTagIds,
                    viewerId = currentUserId,
                )
            } else {
                postRepository.findPostsWithConditions(
                    cursor = postCursor,
                    size = size + 1,
                    period = period,
                    sortType = sortType,
                    visibleToUserId = currentUserId,
                    tagIds = normalizedTagIds,
                    viewerId = currentUserId,
                )
            }

        val hasNext = posts.size > size
        val content = posts.take(size)

        val nextCursor =
            if (hasNext && content.isNotEmpty()) {
                PostCursor.from(content.last(), sortType).encode()
            } else {
                null
            }

        val readPostIds =
            if (currentUserId != null && content.isNotEmpty()) {
                postReadLogRepository.findByUserIdAndPostIdIn(
                    userId = currentUserId,
                    postIds = content.mapNotNull { it.id },
                ).map { it.postId }.toSet()
            } else {
                emptySet()
            }

        val postIds = content.mapNotNull { it.id }
        val attachmentsByPostId =
            if (postIds.isNotEmpty()) {
                attachmentService.getConfirmedAttachmentsByReferenceIds(postIds, AttachmentReferenceType.POST)
            } else {
                emptyMap()
            }
        val presignedThumbnailUrlByAttachmentId =
            attachmentsByPostId
                .values
                .flatten()
                .takeIf { it.isNotEmpty() }
                ?.let { attachmentService.generatePresignedDownloadUrlMapByAttachments(it) }
                ?: emptyMap()

        return CursorPageResponse(
            content =
                content.map { post ->
                    convertToResponse(
                        post,
                        readPostIds.contains(post.id),
                        attachmentsByPostId[post.id] ?: emptyList(),
                        presignedThumbnailUrlByAttachmentId,
                    )
                },
            nextCursor = nextCursor,
            hasNext = hasNext,
            size = content.size,
        )
    }

    private fun normalizeTagIds(tagIds: List<UUID>?): List<UUID>? {
        val normalizedTagIds = tagIds?.distinct()

        return normalizedTagIds?.takeIf { it.isNotEmpty() }
    }

    /**
     * 현재 사용자의 DRAFT 게시물 목록을 커서 기반으로 조회합니다.
     * 최근 수정일 기준 내림차순으로 정렬됩니다.
     *
     * @param userId 사용자 ID
     * @param cursor 커서 문자열 (형식: "updatedAt_id", 없으면 첫 페이지)
     * @param size 페이지 크기
     * @return DRAFT 게시물 목록 커서 페이지
     */
    @Transactional
    fun getMyDrafts(
        userId: UUID,
        cursor: String?,
        size: Int,
    ): CursorPageResponse<DraftListItemResponse> {
        deleteExpiredDrafts(userId)

        val posts =
            if (cursor == null) {
                postRepository.findDraftsByAuthorFirstPage(userId, size + 1)
            } else {
                val (cursorUpdatedAt, cursorId) = parseDraftCursor(cursor)
                postRepository.findDraftsByAuthorWithCursor(userId, cursorUpdatedAt, cursorId, size + 1)
            }

        val hasNext = posts.size > size
        val content = posts.take(size).map { DraftListItemResponse.from(it) }
        val nextCursor =
            if (hasNext) {
                val lastPost = posts[size - 1]
                encodeDraftCursor(lastPost.updatedAt, lastPost.id!!)
            } else {
                null
            }

        return CursorPageResponse(
            content = content,
            nextCursor = nextCursor,
            hasNext = hasNext,
            size = content.size,
        )
    }

    private fun parseDraftCursor(cursor: String): Pair<java.util.Date, UUID> {
        val parts = cursor.split("_")
        val updatedAtMillis = parts[0].toLong()
        val id = UUID.fromString(parts[1])
        return Pair(java.util.Date(updatedAtMillis), id)
    }

    private fun encodeDraftCursor(
        updatedAt: java.util.Date,
        id: UUID,
    ): String {
        return "${updatedAt.time}_$id"
    }

    /**
     * 2주 이상 경과한 DRAFT 게시물을 삭제합니다.
     * S3 첨부파일도 함께 삭제됩니다.
     *
     * @param userId 사용자 ID
     */
    private fun deleteExpiredDrafts(userId: UUID) {
        val calendar =
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -STALE_DRAFT_DAYS)
            }
        val expirationDate = calendar.time

        val staleDrafts = postRepository.findStaleDraftsByAuthor(userId, expirationDate)
        staleDrafts.forEach { post ->
            attachmentService.deleteAttachmentsByReference(post.id!!, AttachmentReferenceType.POST)
            postRepository.delete(post)
        }
    }

    /**
     * Post 엔티티를 PostListItemResponse DTO로 변환합니다.
     * 썸네일 URL과 읽음 여부를 계산하여 포함합니다.
     *
     * @param post 게시물 엔티티
     * @param isRead 현재 사용자가 읽은 게시물인지 여부
     * @param attachments 게시물의 CONFIRMED 첨부파일 목록 (썸네일 계산용)
     * @return 응답 DTO
     */
    private fun convertToResponse(
        post: Post,
        isRead: Boolean,
        attachments: List<Attachment>,
        presignedThumbnailUrlByAttachmentId: Map<UUID, String>,
    ): PostListItemResponse {
        val thumbnailUrl =
            attachments
                .minByOrNull { it.createdAt }
                ?.id
                ?.let { presignedThumbnailUrlByAttachmentId[it] }
                ?: "$baseUrl$defaultThumbnailUrl"

        return PostListItemResponse(
            id = post.id!!,
            title = post.title,
            content = post.content.take(POST_LIST_CONTENT_MAX_LENGTH),
            authorId = post.author.id!!,
            authorName = post.author.name,
            authorProfileImageUrl = post.author.profileImageUrl,
            thumbnailUrl = thumbnailUrl,
            isRead = isRead,
            tags = post.tags.map { PostListTagResponse.from(it) },
            viewCount = post.viewCount,
            likeCount = post.likeCount,
            commentCount = post.commentCount,
            status = post.status,
            createdAt = post.createdAt,
            updatedAt = post.updatedAt,
        )
    }
}
