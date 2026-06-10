package com.techtaurant.mainserver.comment.application

import com.techtaurant.mainserver.comment.dto.CommentContentListResponse
import com.techtaurant.mainserver.comment.dto.CommentListResponse
import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.user.application.BannedUserMaskingService
import com.techtaurant.mainserver.user.application.UserBanService
import com.techtaurant.mainserver.user.application.UserProfileImageResolver
import com.techtaurant.mainserver.user.entity.User
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CommentResponseAssembler(
    private val userBanService: UserBanService,
    private val bannedUserMaskingService: BannedUserMaskingService,
    private val userProfileImageResolver: UserProfileImageResolver,
) {
    fun assembleContents(comments: List<Comment>): List<CommentContentListResponse> {
        if (comments.isEmpty()) {
            return emptyList()
        }

        return comments.map(CommentContentListResponse::from)
    }

    fun assemble(
        comments: List<Comment>,
        likeStatusMap: Map<UUID, LikeStatus>,
        userId: UUID?,
    ): List<CommentListResponse> {
        if (comments.isEmpty()) {
            return emptyList()
        }

        val bannedUserIds = userBanService.getBannedUserIds(userId)
        val authorProfileImageUrlByUserId =
            resolveAuthorProfileImageUrlByUserId(
                comments
                    .map { it.author }
                    .filterNot { bannedUserIds.contains(it.id) }
                    .distinctBy { it.id },
            )

        return comments.map { comment ->
            val likeStatus = likeStatusMap[comment.id!!] ?: LikeStatus.NONE
            val author = comment.author
            val authorId = author.id!!
            if (!bannedUserIds.contains(authorId)) {
                CommentListResponse.from(
                    comment = comment,
                    likeStatus = likeStatus,
                    authorProfileImageUrl = authorProfileImageUrlByUserId[authorId] ?: author.getFallbackProfileImageUrl(),
                )
            } else {
                CommentListResponse.fromMasked(
                    comment = comment,
                    likeStatus = likeStatus,
                    maskedAuthorId = bannedUserMaskingService.maskAuthorId(authorId),
                    maskedAuthorName = bannedUserMaskingService.maskAuthorName(authorId),
                    maskedContent = bannedUserMaskingService.maskCommentContent(comment.id!!),
                )
            }
        }
    }

    private fun resolveAuthorProfileImageUrlByUserId(authors: List<User>): Map<UUID, String> = userProfileImageResolver.resolve(authors)
}
