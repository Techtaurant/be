package com.techtaurant.mainserver.user.infrastructure.out

import com.techtaurant.mainserver.user.entity.UserBan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserBanRepository : JpaRepository<UserBan, UUID> {
    fun findByUserIdAndBannedUserId(
        userId: UUID,
        bannedUserId: UUID,
    ): UserBan?

    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<UserBan>

    @Query("SELECT ub.bannedUser.id FROM UserBan ub WHERE ub.user.id = :userId")
    fun findBannedUserIdsByUserId(
        @Param("userId") userId: UUID,
    ): List<UUID>
}
