package com.techtaurant.mainserver.comment.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.user.entity.User
import jakarta.persistence.*

/**
 * 댓글 좋아요 이벤트 로그 엔티티
 * 좋아요/취소 이벤트를 기록하여 실시간 통계 집계에 사용합니다.
 * 한 사용자는 같은 댓글에 대해 하나의 좋아요 상태만 가질 수 있습니다.
 *
 * @property comment 좋아요된 댓글
 * @property user 좋아요한 사용자
 * @property isLiked TRUE: 좋아요, FALSE: 좋아요 취소
 */
@Entity
@Table(
    name = "comment_like_log",
    uniqueConstraints = [UniqueConstraint(columnNames = ["comment_id", "user_id"])],
    indexes = [
        Index(name = "idx_comment_like_log_comment_created_utc", columnList = "comment_id,created_at_utc"),
        Index(name = "idx_comment_like_log_created_utc", columnList = "created_at_utc"),
    ],
)
class CommentLikeLog(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    var comment: Comment,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,
    @Column(name = "is_liked", nullable = false)
    var isLiked: Boolean = true,
) : EntityBase()
