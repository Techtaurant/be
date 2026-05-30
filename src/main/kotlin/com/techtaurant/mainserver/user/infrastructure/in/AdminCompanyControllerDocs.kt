package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.security.jwt.JwtStatus
import com.techtaurant.mainserver.user.dto.CompanyResponse
import com.techtaurant.mainserver.user.dto.CreateCompanyRequest
import com.techtaurant.mainserver.user.dto.CreateUserTokenRequest
import com.techtaurant.mainserver.user.dto.UserTokenResponse
import com.techtaurant.mainserver.user.enums.UserStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "관리자 회사", description = "관리자 전용 회사 등록/조회 API")
interface AdminCompanyControllerDocs {
    @Operation(summary = "회사 등록", description = "관리자가 회사를 COMPANY 역할의 사용자로 등록합니다")
    @SwaggerApiResponse(responseCode = "201", description = "회사 등록 성공")
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED", "ACCESS_DENIED"]),
            ApiErrorCodeResponse(UserStatus::class, ["USER_NAME_ALREADY_EXISTS"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST", "UNKNOWN_EXCEPTION"]),
        ],
    )
    fun createCompany(
        @Valid request: CreateCompanyRequest,
    ): ApiResponse<CompanyResponse>

    @Operation(summary = "회사 목록 조회", description = "관리자가 등록된 회사 목록을 이름순으로 조회합니다")
    @SwaggerApiResponse(responseCode = "200", description = "회사 목록 조회 성공")
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED", "ACCESS_DENIED"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getCompanies(): ApiResponse<List<CompanyResponse>>

    @Operation(
        summary = "회사 봇 영구 토큰 발급",
        description = "관리자가 회사 봇에서 사용할 만료 없는 JWT를 발급하고 사용자 토큰 저장소에 등록합니다",
    )
    @SwaggerApiResponse(responseCode = "201", description = "회사 봇 영구 토큰 발급 성공")
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED", "ACCESS_DENIED"]),
            ApiErrorCodeResponse(UserStatus::class, ["COMPANY_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST", "UNKNOWN_EXCEPTION"]),
        ],
    )
    fun createCompanyToken(
        @Parameter(description = "회사 사용자 ID") companyUserId: UUID,
        @Valid request: CreateUserTokenRequest,
    ): ApiResponse<UserTokenResponse>
}
