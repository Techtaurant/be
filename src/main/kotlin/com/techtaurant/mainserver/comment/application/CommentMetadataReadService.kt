package com.techtaurant.mainserver.comment.application

import com.techtaurant.mainserver.comment.dto.CommentMetadataResponse
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepositoryCustom
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CommentMetadataReadService(
    private val commentRepository: CommentRepositoryCustom,
) {
    fun getCommentMetadata(commentIds: List<UUID>): List<CommentMetadataResponse> {
        val normalizedCommentIds = commentIds.distinct()
        if (normalizedCommentIds.isEmpty()) {
            return emptyList()
        }

        val commentById =
            commentRepository.findCommentsByIdsIncludingDeleted(normalizedCommentIds)
                .associateBy { it.id!! }

        return normalizedCommentIds.mapNotNull { commentId ->
            commentById[commentId]?.let(CommentMetadataResponse::from)
        }
    }
}
