package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.link.dto.LinkListItemResponse
import com.techtaurant.mainserver.link.dto.RecordLinkLikeRequest
import com.techtaurant.mainserver.link.dto.RecordLinkReadRequest
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.security.jwt.JwtStatus
import com.techtaurant.mainserver.user.enums.UserStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "링크", description = "회사 링크 조회 및 사용자 상호작용 API")
interface LinkControllerDocs {
    @Operation(summary = "회사 링크 목록 조회", description = "회사가 수집한 링크 목록을 커서 기반으로 조회합니다")
    @SwaggerApiResponse(responseCode = "200", description = "링크 목록 조회 성공")
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(UserStatus::class, ["COMPANY_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getCompanyLinks(
        userId: UUID,
        @Parameter(description = "회사 사용자 ID") companyUserId: UUID,
        cursor: String?,
        @Parameter(description = "페이지 크기 (1~100)") @Min(1) @Max(100) size: Int,
        tag: String?,
    ): ApiResponse<CursorPageResponse<LinkListItemResponse>>

    @Operation(summary = "링크 저장", description = "사용자가 링크를 저장합니다. 이미 저장된 경우에도 멱등하게 처리합니다")
    fun saveLink(
        userId: UUID,
        @Parameter(description = "링크 ID") linkId: UUID,
    ): ApiResponse<Unit>

    @Operation(summary = "링크 저장 취소", description = "사용자가 저장한 링크를 해제합니다")
    fun unsaveLink(
        userId: UUID,
        @Parameter(description = "링크 ID") linkId: UUID,
    )

    @Operation(summary = "링크 읽음 상태 변경", description = "링크 읽음/안읽음 상태를 명시적으로 토글합니다")
    fun toggleReadStatus(
        userId: UUID,
        @Parameter(description = "링크 ID") linkId: UUID,
        @Valid request: RecordLinkReadRequest,
    ): ApiResponse<Unit>

    @Operation(
        summary = "링크 좋아요 상태 변경",
        description = "링크에 대한 좋아요 상태를 변경합니다. NONE: 취소, LIKE: 좋아요, DISLIKE: 싫어요. 인증된 사용자만 호출 가능합니다.",
    )
    @SwaggerApiResponse(responseCode = "200", description = "좋아요/싫어요 기록 성공")
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(LinkStatus::class, ["LINK_NOT_FOUND"]),
            ApiErrorCodeResponse(UserStatus::class, ["ID_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun recordLike(
        userId: UUID,
        @Parameter(description = "링크 ID") linkId: UUID,
        @Valid request: RecordLinkLikeRequest,
    ): ApiResponse<Unit>
}
