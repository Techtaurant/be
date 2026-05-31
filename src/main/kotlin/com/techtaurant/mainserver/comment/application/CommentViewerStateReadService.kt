package com.techtaurant.mainserver.comment.application

import com.techtaurant.mainserver.comment.dto.CommentViewerStateResponse
import com.techtaurant.mainserver.comment.entity.CommentLikeLog
import com.techtaurant.mainserver.comment.infrastructure.out.CommentLikeLogRepository
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepositoryCustom
import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.user.application.UserBanService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CommentViewerStateReadService(
    private val commentRepository: CommentRepositoryCustom,
    private val commentLikeLogRepository: CommentLikeLogRepository,
    private val userBanService: UserBanService,
) {
    fun getCommentViewerStates(
        userId: UUID,
        commentIds: List<UUID>,
    ): List<CommentViewerStateResponse> {
        val normalizedCommentIds = commentIds.distinct()
        if (normalizedCommentIds.isEmpty()) {
            return emptyList()
        }

        val commentById =
            commentRepository.findCommentsByIdsIncludingDeleted(normalizedCommentIds)
                .associateBy { it.id!! }
        val loadedCommentIds = normalizedCommentIds.filter { commentById.containsKey(it) }
        val likeStatusByCommentId =
            commentLikeLogRepository
                .findByCommentIdInAndUserId(commentIds = loadedCommentIds, userId = userId)
                .associate { likeLog -> likeLog.comment.id!! to likeLog.toLikeStatus() }
        val bannedUserIds = userBanService.getBannedUserIds(userId)

        return loadedCommentIds.mapNotNull { commentId ->
            val comment = commentById[commentId] ?: return@mapNotNull null
            CommentViewerStateResponse(
                commentId = commentId,
                likeStatus = likeStatusByCommentId[commentId] ?: LikeStatus.NONE,
                isBanned = comment.author?.id?.let { it in bannedUserIds } ?: false,
            )
        }
    }

    private fun CommentLikeLog.toLikeStatus(): LikeStatus =
        if (isLiked) {
            LikeStatus.LIKE
        } else {
            LikeStatus.DISLIKE
        }
}
