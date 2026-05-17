package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.user.application.UserBanService
import com.techtaurant.mainserver.user.application.UserFollowService
import com.techtaurant.mainserver.user.application.UserReadService
import com.techtaurant.mainserver.user.application.UserWriteService
import com.techtaurant.mainserver.user.dto.UpdateUserRequest
import com.techtaurant.mainserver.user.dto.UserBanListItemResponse
import com.techtaurant.mainserver.user.dto.UserBanResponse
import com.techtaurant.mainserver.user.dto.UserFollowResponse
import com.techtaurant.mainserver.user.dto.UserResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.API_PREFIX}/users")
class UserController(
    private val userReadService: UserReadService,
    private val userWriteService: UserWriteService,
    private val userBanService: UserBanService,
    private val userFollowService: UserFollowService,
) : UserControllerDocs {
    @GetMapping("/me")
    override fun getMe(
        @AuthenticationPrincipal userId: UUID,
    ): ApiResponse<UserResponse> {
        return ApiResponse.ok(userReadService.getMe(userId))
    }

    @ApiErrorResponses(includeAuthenticationErrors = true, includeValidationError = true)
    @PatchMapping("/me")
    override fun updateMe(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: UpdateUserRequest,
    ): ApiResponse<UserResponse> {
        return ApiResponse.ok(userWriteService.updateMe(userId, request))
    }

    @PostMapping("/{targetUserId}/ban")
    @ResponseStatus(HttpStatus.CREATED)
    override fun banUser(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable targetUserId: UUID,
    ): ApiResponse<UserBanResponse> {
        return ApiResponse.created(userBanService.banUser(userId, targetUserId))
    }

    @PostMapping("/{targetUserId}/follow")
    @ResponseStatus(HttpStatus.CREATED)
    override fun followUser(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable targetUserId: UUID,
    ): ApiResponse<UserFollowResponse> {
        return ApiResponse.created(userFollowService.follow(userId, targetUserId))
    }

    @GetMapping("/me/bans")
    override fun getMyBannedUsers(
        @AuthenticationPrincipal userId: UUID,
    ): ApiResponse<List<UserBanListItemResponse>> {
        return ApiResponse.ok(userBanService.getBannedUsers(userId))
    }

    @DeleteMapping("/{targetUserId}/ban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun unbanUser(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable targetUserId: UUID,
    ) {
        userBanService.unbanUser(userId, targetUserId)
    }

    @DeleteMapping("/{targetUserId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun unfollowUser(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable targetUserId: UUID,
    ) {
        userFollowService.unfollow(userId, targetUserId)
    }
}
