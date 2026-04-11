package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.security.jwt.JwtStatus
import com.techtaurant.mainserver.user.dto.UpdateUserRequest
import com.techtaurant.mainserver.user.dto.UserBanListItemResponse
import com.techtaurant.mainserver.user.dto.UserBanResponse
import com.techtaurant.mainserver.user.dto.UserFollowResponse
import com.techtaurant.mainserver.user.dto.UserResponse
import com.techtaurant.mainserver.user.enums.UserStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "사용자", description = "사용자 API")
interface UserControllerDocs {
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다")
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(UserStatus::class, ["ID_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getMe(userId: UUID): ApiResponse<UserResponse>

    @Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 이름과 서비스 프로필 이미지를 수정합니다")
    @SwaggerApiResponse(
        responseCode = "200",
        description = "수정 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(UserStatus::class, ["ID_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST", "UNKNOWN_EXCEPTION"]),
        ],
    )
    fun updateMe(
        userId: UUID,
        @Valid request: UpdateUserRequest,
    ): ApiResponse<UserResponse>

    @Operation(summary = "사용자 차단", description = "현재 로그인한 사용자가 특정 사용자를 차단합니다")
    @SwaggerApiResponse(
        responseCode = "201",
        description = "차단 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(UserStatus::class, ["USER_NOT_FOUND", "CANNOT_BAN_SELF", "USER_ALREADY_BANNED"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun banUser(
        userId: UUID,
        targetUserId: UUID,
    ): ApiResponse<UserBanResponse>

    @Operation(summary = "사용자 팔로우", description = "현재 로그인한 사용자가 특정 사용자를 팔로우합니다")
    @SwaggerApiResponse(
        responseCode = "201",
        description = "팔로우 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(UserStatus::class, ["USER_NOT_FOUND", "CANNOT_FOLLOW_SELF"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun followUser(
        userId: UUID,
        targetUserId: UUID,
    ): ApiResponse<UserFollowResponse>

    @Operation(summary = "내가 차단한 사용자 목록 조회", description = "현재 로그인한 사용자가 차단한 사용자 목록을 최신순으로 조회합니다")
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getMyBannedUsers(userId: UUID): ApiResponse<List<UserBanListItemResponse>>

    @Operation(summary = "사용자 차단 해제", description = "현재 로그인한 사용자가 특정 사용자 차단을 해제합니다")
    @SwaggerApiResponse(
        responseCode = "204",
        description = "차단 해제 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(UserStatus::class, ["CANNOT_BAN_SELF", "USER_BAN_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun unbanUser(
        userId: UUID,
        targetUserId: UUID,
    )

    @Operation(summary = "사용자 팔로우 취소", description = "현재 로그인한 사용자가 특정 사용자 팔로우를 취소합니다")
    @SwaggerApiResponse(
        responseCode = "204",
        description = "팔로우 취소 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(UserStatus::class, ["CANNOT_FOLLOW_SELF", "USER_FOLLOW_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun unfollowUser(
        userId: UUID,
        targetUserId: UUID,
    )
}
