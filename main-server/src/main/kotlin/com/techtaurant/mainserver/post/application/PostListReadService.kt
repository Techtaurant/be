package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.post.dto.DraftListItemResponse
import com.techtaurant.mainserver.post.dto.PostCursor
import com.techtaurant.mainserver.post.dto.PostListItemResponse
import com.techtaurant.mainserver.post.dto.PostListTagResponse
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostSortType
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostViewLogRepository
import com.techtaurant.mainserver.user.entity.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 게시물 목록 조회 서비스
 */
@Service
@Transactional(readOnly = true)
class PostListReadService(
    private val postRepository: PostRepository,
    private val postViewLogRepository: PostViewLogRepository,
    @param:Value("\${app.default-post-thumbnail-url}")
    private val defaultThumbnailUrl: String,
    @param:Value("\${swagger.base-url}")
    private val baseUrl: String,
) {
    /**
     * 게시물 목록을 커서 기반 페이지네이션으로 조회
     *
     * @param cursor 이전 응답의 nextCursor (null이면 첫 페이지)
     * @param size 페이지 크기
     * @param period 기간 필터 (WEEK, MONTH, YEAR, ALL)
     * @param sortType 정렬 기준 (LATEST, VIEW, LIKE, COMMENT)
     * @return 커서 기반 페이지 응답
     */
    fun getPosts(
        cursor: String?,
        size: Int,
        period: PostPeriod = PostPeriod.ALL,
        sortType: PostSortType = PostSortType.LATEST,
    ): CursorPageResponse<PostListItemResponse> {
        val postCursor = cursor?.let { PostCursor.decode(it) }

        if (cursor != null && postCursor == null) {
            return CursorPageResponse(
                content = emptyList(),
                nextCursor = null,
                hasNext = false,
                size = 0,
            )
        }

        val posts =
            postRepository.findPostsWithConditions(
                cursor = postCursor,
                size = size + 1,
                period = period,
                sortType = sortType,
            )

        val hasNext = posts.size > size
        val content = posts.take(size)

        val nextCursor =
            if (hasNext && content.isNotEmpty()) {
                PostCursor.from(content.last(), sortType).encode()
            } else {
                null
            }

        val currentUserId = getCurrentUserId()
        val readPostIds =
            if (currentUserId != null && content.isNotEmpty()) {
                postViewLogRepository.findDistinctPostIdsByUserIdAndPostIdIn(
                    userId = currentUserId,
                    postIds = content.mapNotNull { it.id },
                ).toSet()
            } else {
                emptySet()
            }

        return CursorPageResponse(
            content =
                content.map { post ->
                    convertToResponse(post, readPostIds.contains(post.id))
                },
            nextCursor = nextCursor,
            hasNext = hasNext,
            size = content.size,
        )
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
    @Transactional(readOnly = true)
    fun getMyDrafts(
        userId: UUID,
        cursor: String?,
        size: Int,
    ): CursorPageResponse<DraftListItemResponse> {
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
     * SecurityContext에서 현재 로그인한 사용자의 ID를 추출합니다.
     * 인증되지 않은 사용자(비회원)인 경우 null을 반환합니다.
     *
     * @return 현재 사용자 ID (비회원이면 null)
     */
    private fun getCurrentUserId(): UUID? {
        val authentication = SecurityContextHolder.getContext().authentication
        return (authentication?.principal as? User)?.id
    }

    /**
     * Post 엔티티를 PostListItemResponse DTO로 변환합니다.
     * 썸네일 URL과 읽음 여부를 계산하여 포함합니다.
     *
     * @param post 게시물 엔티티
     * @param isRead 현재 사용자가 읽은 게시물인지 여부
     * @return 응답 DTO
     */
    private fun convertToResponse(
        post: Post,
        isRead: Boolean,
    ): PostListItemResponse {
        val thumbnailUrl =
            post.pictures
                .filter { it.isThumbnail }
                .minByOrNull { it.displayOrder }
                ?.pictureUrl
                ?: post.pictures
                    .minByOrNull { it.displayOrder }
                    ?.pictureUrl
                ?: "$baseUrl$defaultThumbnailUrl"

        return PostListItemResponse(
            id = post.id!!,
            title = post.title,
            authorName = post.author.name,
            authorProfileImageUrl = post.author.profileImageUrl,
            thumbnailUrl = thumbnailUrl,
            isRead = isRead,
            tags = post.tags.map { PostListTagResponse.from(it) },
            viewCount = post.viewCount,
            likeCount = post.likeCount,
            commentCount = post.commentCount,
            createdAt = post.createdAt,
        )
    }
}
