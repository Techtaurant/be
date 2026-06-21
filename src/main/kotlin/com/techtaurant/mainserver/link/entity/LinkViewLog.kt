package com.techtaurant.mainserver.link.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "link_view_log",
    indexes = [
        Index(name = "idx_link_view_log_link_created_utc", columnList = "link_id,created_at_utc"),
        Index(name = "idx_link_view_log_created_utc", columnList = "created_at_utc"),
    ],
)
class LinkViewLog(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    var link: Link,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,
    @Column(name = "ip_address", length = 45)
    var ipAddress: String? = null,
    @Column(name = "user_agent", columnDefinition = "TEXT")
    var userAgent: String? = null,
) : EntityBase()
