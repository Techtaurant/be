package com.techtaurant.mainserver.user.infrastructure.out

import com.techtaurant.mainserver.user.entity.UserBan
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserBanRepository : JpaRepository<UserBan, UUID> {
    fun existsByUserIdAndBannedUserId(
        userId: UUID,
        bannedUserId: UUID,
    ): Boolean

    fun findByUserIdAndBannedUserId(
        userId: UUID,
        bannedUserId: UUID,
    ): UserBan?

    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<UserBan>

    fun findAllByUserId(userId: UUID): List<UserBan>
}
