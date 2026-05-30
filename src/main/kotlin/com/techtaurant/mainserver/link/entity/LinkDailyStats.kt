package com.techtaurant.mainserver.link.entity

import com.techtaurant.mainserver.common.base.EntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.sql.Date

@Entity
@Table(
    name = "link_daily_stats",
    uniqueConstraints = [UniqueConstraint(name = "uk_link_daily_stats_link_id_stat_date", columnNames = ["link_id", "stat_date"])],
)
class LinkDailyStats(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    var link: Link,
    @Column(name = "stat_date", nullable = false)
    var statDate: Date,
    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0,
    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0,
    @Column(name = "save_count", nullable = false)
    var saveCount: Long = 0,
) : EntityBase()
