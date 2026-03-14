package com.techtaurant.mainserver.comment.application

import com.techtaurant.mainserver.comment.enums.CommentStatus
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepository
import com.techtaurant.mainserver.common.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Date
import java.util.UUID

/**
 * 댓글 삭제 서비스
 * soft delete 방식으로 댓글을 삭제하며, 원문 내용은 해시 문자열로 치환합니다.
 */
@Service
class CommentDeleteService(
    private val commentRepository: CommentRepository,
) {
    /**
     * 댓글을 soft delete 처리합니다.
     * 내용은 SHA-256 해시 문자열로 치환되며, 대댓글은 그대로 유지됩니다.
     *
     * @param commentId 삭제할 댓글 ID
     * @param userId 요청 사용자 ID
     * @throws ApiException 댓글 없음(COMMENT_NOT_FOUND), 이미 삭제됨(COMMENT_ALREADY_DELETED), 권한 없음(COMMENT_AUTHOR_MISMATCH)
     */
    @Transactional
    fun deleteComment(
        commentId: UUID,
        userId: UUID,
    ) {
        val comment =
            commentRepository.findById(commentId).orElseThrow {
                ApiException(CommentStatus.COMMENT_NOT_FOUND)
            }

        if (comment.deletedAt != null) {
            throw ApiException(CommentStatus.COMMENT_ALREADY_DELETED)
        }

        if (comment.author.id != userId) {
            throw ApiException(CommentStatus.COMMENT_AUTHOR_MISMATCH)
        }

        comment.content = hashContent(comment.content)
        comment.deletedAt = Date()

        commentRepository.save(comment)
    }

    private fun hashContent(content: String): String {
        return MessageDigest.getInstance(HASH_ALGORITHM)
            .digest(content.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        private const val HASH_ALGORITHM = "SHA-256"
    }
}
