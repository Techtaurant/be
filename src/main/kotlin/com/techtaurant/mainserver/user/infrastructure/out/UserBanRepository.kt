package com.techtaurant.mainserver.user.infrastructure.out

import com.techtaurant.mainserver.user.entity.UserBan
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserBanRepository : JpaRepository<UserBan, UUID> {
    fun existsByUser_IdAndBannedUser_Id(
        userId: UUID,
        bannedUserId: UUID,
    ): Boolean

    fun findByUser_IdAndBannedUser_Id(
        userId: UUID,
        bannedUserId: UUID,
    ): UserBan?

    fun findAllByUser_IdOrderByCreatedAtDesc(userId: UUID): List<UserBan>

    fun findAllByUser_Id(userId: UUID): List<UserBan>
}
