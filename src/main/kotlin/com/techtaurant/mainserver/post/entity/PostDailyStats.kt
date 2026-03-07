package com.techtaurant.mainserver.post.entity

import com.techtaurant.mainserver.common.base.EntityBase
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 일별 게시물 통계 엔티티 (이벤트 로그)
 *
 * @property post 대상 게시물
 * @property statDate 통계 집계 일자
 * @property viewCount 해당 일자 조회 증분
 * @property likeCount 해당 일자 좋아요 증분
 * @property commentCount 해당 일자 댓글 증분
 */
@Entity
@Table(
    name = "post_daily_stats",
    uniqueConstraints = [UniqueConstraint(columnNames = ["post_id", "stat_date"])],
)
class PostDailyStats(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    var post: Post,
    @Column(name = "stat_date", nullable = false)
    var statDate: LocalDate,
    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0,
    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0,
    @Column(name = "comment_count", nullable = false)
    var commentCount: Long = 0,
) : EntityBase()
