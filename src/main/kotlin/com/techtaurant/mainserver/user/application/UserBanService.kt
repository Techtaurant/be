package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.user.dto.UserBanListItemResponse
import com.techtaurant.mainserver.user.dto.UserBanResponse
import com.techtaurant.mainserver.user.entity.UserBan
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserBanRepository
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.dao.DataIntegrityViolationException
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

        val user =
            userRepository.findById(userId).orElseThrow {
                // @AuthenticationPrincipal로 주입된 userId는 이미 인증된 사용자이므로
                // 정상 플로우에서는 도달 불가. 토큰 유효 기간 내 사용자가 탈퇴한 경우에만 발생.
                ApiException(UserStatus.ID_NOT_FOUND)
            }
        val targetUser =
            userRepository.findById(targetUserId).orElseThrow {
                ApiException(UserStatus.USER_NOT_FOUND)
            }

        // 이미 차단한 경우 기존 차단 정보를 그대로 반환 (idempotent)
        userBanRepository.findByUserIdAndBannedUserId(userId, targetUserId)?.let {
            return UserBanResponse.from(it)
        }

        val userBan =
            try {
                userBanRepository.save(
                    UserBan(
                        user = user,
                        bannedUser = targetUser,
                    ),
                )
            } catch (e: DataIntegrityViolationException) {
                // 동시 요청으로 인한 유니크 제약 위반 시 기존 차단 정보를 반환 (idempotent)
                userBanRepository.findByUserIdAndBannedUserId(userId, targetUserId)!!
            }

        return UserBanResponse.from(userBan)
    }

    fun getBannedUsers(userId: UUID): List<UserBanListItemResponse> {
        return userBanRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
            .map(UserBanListItemResponse::from)
    }

    fun getBannedUserIds(userId: UUID?): Set<UUID> {
        if (userId == null) {
            return emptySet()
        }

        return userBanRepository.findBannedUserIdsByUserId(userId).toHashSet()
    }

    @Transactional
    fun unbanUser(
        userId: UUID,
        targetUserId: UUID,
    ) {
        validateNotSelfBan(userId, targetUserId)

        val userBan =
            userBanRepository.findByUserIdAndBannedUserId(userId, targetUserId)
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
