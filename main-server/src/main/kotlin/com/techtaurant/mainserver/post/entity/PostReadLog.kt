package com.techtaurant.mainserver.post.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.user.entity.User
import jakarta.persistence.*
import java.util.UUID

/**
 * 게시물 읽음 표시 기록 엔티티
 * 사용자가 명시적으로 게시물을 읽었다고 표시한 기록을 저장합니다.
 * 레코드가 존재하면 읽음, 존재하지 않으면 안읽음 상태입니다.
 *
 * @property postId 읽음 표시한 게시물 ID
 * @property user 읽음 표시한 사용자
 */
@Entity
@Table(
    name = "post_read_log",
    uniqueConstraints = [UniqueConstraint(columnNames = ["post_id", "user_id"])],
    indexes = [
        Index(name = "idx_post_read_log_user_post", columnList = "user_id,post_id"),
    ],
)
class PostReadLog(
    @Column(name = "post_id", nullable = false)
    var postId: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,
) : EntityBase()
