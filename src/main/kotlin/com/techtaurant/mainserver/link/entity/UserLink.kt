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
    name = "user_links",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_links_user_id_link_id_is_source",
            columnNames = ["user_id", "link_id", "is_source"],
        ),
    ],
    indexes = [
        Index(name = "idx_user_links_user_link", columnList = "user_id,link_id,is_source"),
        Index(name = "idx_user_links_link_source", columnList = "link_id,is_source"),
    ],
)
class UserLink(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    var link: Link,
    @Column(name = "is_source", nullable = false)
    var isSource: Boolean = false,
) : EntityBase()
