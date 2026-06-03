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
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "link_like_log",
    uniqueConstraints = [UniqueConstraint(name = "uk_link_like_log_link_id_user_id", columnNames = ["link_id", "user_id"])],
    indexes = [
        Index(name = "idx_link_like_log_link_created_utc", columnList = "link_id,created_at_utc"),
        Index(name = "idx_link_like_log_created_utc", columnList = "created_at_utc"),
    ],
)
class LinkLikeLog(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    var link: Link,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,
    @Column(name = "is_liked", nullable = false)
    var isLiked: Boolean = true,
) : EntityBase()
