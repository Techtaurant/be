package com.techtaurant.mainserver.security.jwt

import com.techtaurant.mainserver.common.status.StatusIfs

enum class JwtStatus(
    private val httpStatusCode: Int,
    private val customStatusCode: Int,
    private val description: String,
) : StatusIfs {
    // Refresh Token 관련
    INVALID_REFRESH_TOKEN(401, 3001, "유효하지 않은 Refresh Token입니다"),
    MISSING_REFRESH_TOKEN(401, 3002, "Refresh Token이 없습니다"),

    // Access Token 관련
    TOKEN_EXPIRED(401, 3003, "토큰이 만료되었습니다"),
    INVALID_TOKEN(401, 3004, "유효하지 않은 토큰입니다"),
    MALFORMED_TOKEN(401, 3005, "잘못된 형식의 토큰입니다"),
    UNSUPPORTED_TOKEN(401, 3006, "지원하지 않는 토큰입니다"),

    // 인증/인가 관련
    AUTHENTICATION_REQUIRED(401, 3008, "인증이 필요합니다"),
    ACCESS_DENIED(403, 3009, "접근 권한이 없습니다"),

    UNKNOWN_ERROR(500, 3099, "알 수 없는 JWT 오류가 발생했습니다")
    ;

    override fun getHttpStatusCode(): Int = httpStatusCode

    override fun getCustomStatusCode(): Int = customStatusCode

    override fun getDescription(): String = description
}
