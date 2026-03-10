package com.techtaurant.mainserver.security.dto

/**
 * 개발 환경 테스트 로그인 응답 DTO
 *
 * @property accessToken 액세스 토큰
 * @property refreshToken 리프레시 토큰
 */
data class DevTestLoginResponse(
    val accessToken: String,
    val refreshToken: String,
)
