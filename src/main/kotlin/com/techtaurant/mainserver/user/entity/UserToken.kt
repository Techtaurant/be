package com.techtaurant.mainserver.user.entity

import com.techtaurant.mainserver.common.base.EntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "user_tokens",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_tokens_user_id", columnNames = ["user_id"]),
    ],
)
class UserToken(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    var tokenHash: String,
) : EntityBase()
