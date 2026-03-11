package com.techtaurant.mainserver.comment.application

import com.techtaurant.mainserver.comment.dto.CommentListResponse
import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.user.application.BannedUserMaskingService
import com.techtaurant.mainserver.user.application.UserBanService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CommentResponseAssembler(
    private val userBanService: UserBanService,
    private val bannedUserMaskingService: BannedUserMaskingService,
) {
    fun assemble(
        comments: List<Comment>,
        likeStatusMap: Map<UUID, LikeStatus>,
        userId: UUID?,
    ): List<CommentListResponse> {
        if (comments.isEmpty()) {
            return emptyList()
        }

        val bannedUserIds = userBanService.getBannedUserIds(userId)

        return comments.map { comment ->
            val likeStatus = likeStatusMap[comment.id!!] ?: LikeStatus.NONE
            if (!bannedUserIds.contains(comment.author.id)) {
                CommentListResponse.from(comment, likeStatus)
            } else {
                CommentListResponse.fromMasked(
                    comment = comment,
                    likeStatus = likeStatus,
                    maskedAuthorId = bannedUserMaskingService.maskAuthorId(comment.author.id!!),
                    maskedAuthorName = bannedUserMaskingService.maskAuthorName(comment.author.id!!),
                    maskedContent = bannedUserMaskingService.maskCommentContent(comment.id!!),
                )
            }
        }
    }
}
