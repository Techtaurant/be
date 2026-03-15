package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.user.dto.UserFollowCountResponse
import com.techtaurant.mainserver.user.dto.UserFollowListItemResponse
import com.techtaurant.mainserver.user.dto.UserFollowResponse
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.entity.UserFollow
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserFollowRepository
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserFollowService(
    private val userRepository: UserRepository,
    private val userFollowRepository: UserFollowRepository,
) {
    @Transactional
    fun follow(
        followerId: UUID,
        targetUserId: UUID,
    ): UserFollowResponse {
        validateNotSelfFollow(followerId, targetUserId)

        val follower = getUser(followerId, UserStatus.ID_NOT_FOUND)
        val following = getUser(targetUserId, UserStatus.USER_NOT_FOUND)

        userFollowRepository.findByFollowerIdAndFollowingId(followerId, targetUserId)?.let {
            return UserFollowResponse.from(it)
        }

        val userFollow =
            try {
                userFollowRepository.save(
                    UserFollow(
                        follower = follower,
                        following = following,
                    ),
                )
            } catch (e: DataIntegrityViolationException) {
                userFollowRepository.findByFollowerIdAndFollowingId(followerId, targetUserId)!!
            }

        return UserFollowResponse.from(userFollow)
    }

    @Transactional
    fun unfollow(
        followerId: UUID,
        targetUserId: UUID,
    ) {
        validateNotSelfFollow(followerId, targetUserId)

        val userFollow =
            userFollowRepository.findByFollowerIdAndFollowingId(followerId, targetUserId)
                ?: throw ApiException(UserStatus.USER_FOLLOW_NOT_FOUND)

        userFollowRepository.delete(userFollow)
    }

    @Transactional(readOnly = true)
    fun getFollowCounts(userId: UUID): UserFollowCountResponse {
        getUser(userId, UserStatus.USER_NOT_FOUND)

        return UserFollowCountResponse(
            followerCount = userFollowRepository.countByFollowingId(userId),
            followingCount = userFollowRepository.countByFollowerId(userId),
        )
    }

    @Transactional(readOnly = true)
    fun getFollowings(userId: UUID): List<UserFollowListItemResponse> {
        getUser(userId, UserStatus.USER_NOT_FOUND)

        return userFollowRepository.findAllByFollowerIdOrderByCreatedAtDesc(userId).map {
            UserFollowListItemResponse.fromFollowing(it)
        }
    }

    @Transactional(readOnly = true)
    fun getFollowers(userId: UUID): List<UserFollowListItemResponse> {
        getUser(userId, UserStatus.USER_NOT_FOUND)

        return userFollowRepository.findAllByFollowingIdOrderByCreatedAtDesc(userId).map {
            UserFollowListItemResponse.fromFollower(it)
        }
    }

    @Transactional
    fun deleteMutualFollows(
        firstUserId: UUID,
        secondUserId: UUID,
    ) {
        if (firstUserId == secondUserId) {
            return
        }

        userFollowRepository.deleteMutualFollows(firstUserId, secondUserId)
    }

    private fun getUser(
        userId: UUID,
        status: UserStatus,
    ): User {
        return userRepository.findById(userId).orElseThrow {
            ApiException(status)
        }
    }

    private fun validateNotSelfFollow(
        followerId: UUID,
        targetUserId: UUID,
    ) {
        if (followerId == targetUserId) {
            throw ApiException(UserStatus.CANNOT_FOLLOW_SELF)
        }
    }
}
