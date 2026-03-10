package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.user.dto.UserBanListItemResponse
import com.techtaurant.mainserver.user.dto.UserBanResponse
import com.techtaurant.mainserver.user.entity.UserBan
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserBanRepository
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserBanService(
    private val userRepository: UserRepository,
    private val userBanRepository: UserBanRepository,
) {
    @Transactional
    fun banUser(
        userId: UUID,
        targetUserId: UUID,
    ): UserBanResponse {
        validateNotSelfBan(userId, targetUserId)

        if (userBanRepository.existsByUser_IdAndBannedUser_Id(userId, targetUserId)) {
            throw ApiException(UserStatus.USER_ALREADY_BANNED)
        }

        val user =
            userRepository.findById(userId).orElseThrow {
                ApiException(UserStatus.ID_NOT_FOUND)
            }
        val targetUser =
            userRepository.findById(targetUserId).orElseThrow {
                ApiException(UserStatus.USER_NOT_FOUND)
            }

        val userBan =
            userBanRepository.save(
                UserBan(
                    user = user,
                    bannedUser = targetUser,
                ),
            )

        return UserBanResponse.from(userBan)
    }

    fun getBannedUsers(userId: UUID): List<UserBanListItemResponse> {
        return userBanRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)
            .map(UserBanListItemResponse::from)
    }

    fun getBannedUserIds(userId: UUID?): Set<UUID> {
        if (userId == null) {
            return emptySet()
        }

        return userBanRepository.findAllByUser_Id(userId)
            .map { it.bannedUser.id!! }
            .toSet()
    }

    @Transactional
    fun unbanUser(
        userId: UUID,
        targetUserId: UUID,
    ) {
        validateNotSelfBan(userId, targetUserId)

        val userBan =
            userBanRepository.findByUser_IdAndBannedUser_Id(userId, targetUserId)
                ?: throw ApiException(UserStatus.USER_BAN_NOT_FOUND)

        userBanRepository.delete(userBan)
    }

    private fun validateNotSelfBan(
        userId: UUID,
        targetUserId: UUID,
    ) {
        if (userId == targetUserId) {
            throw ApiException(UserStatus.CANNOT_BAN_SELF)
        }
    }
}
