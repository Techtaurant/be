package com.techtaurant.mainserver.user.entity

import com.techtaurant.mainserver.common.base.EntityBase
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "user_bans",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_bans_user_id_banned_user_id",
            columnNames = ["user_id", "banned_user_id"],
        ),
    ],
)
class UserBan(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_user_id", nullable = false)
    val bannedUser: User,
) : EntityBase()
