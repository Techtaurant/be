package com.techtaurant.mainserver.post.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.user.entity.User
import jakarta.persistence.*

/**
 * 게시글 조회 이벤트 로그 엔티티
 * 각 조회 이벤트를 기록하여 실시간 통계 집계에 사용합니다.
 *
 * @property post 조회된 게시글
 * @property user 조회한 사용자 (비회원 조회는 null)
 * @property ipAddress 조회한 IP 주소
 * @property userAgent 브라우저 User-Agent
 */
@Entity
@Table(
    name = "post_view_log",
    indexes = [
        Index(name = "idx_post_view_log_post_created_utc", columnList = "post_id,created_at_utc"),
        Index(name = "idx_post_view_log_created_utc", columnList = "created_at_utc"),
    ],
)
class PostViewLog(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    var post: Post,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,
    @Column(name = "ip_address", length = 45)
    var ipAddress: String? = null,
    @Column(name = "user_agent", columnDefinition = "TEXT")
    var userAgent: String? = null,
) : EntityBase()
