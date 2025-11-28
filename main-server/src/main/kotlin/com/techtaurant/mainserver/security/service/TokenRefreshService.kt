package com.techtaurant.mainserver.security.service

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.security.cookie.CookieHelper
import com.techtaurant.mainserver.security.jwt.JwtConstants
import com.techtaurant.mainserver.security.jwt.JwtProperties
import com.techtaurant.mainserver.security.jwt.JwtStatus
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service

@Service
class TokenRefreshService(
    private val cookieHelper: CookieHelper,
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtProperties: JwtProperties
) {

    fun execute(request: HttpServletRequest, response: HttpServletResponse): Unit {
        // 쿠키에서 refresh token 읽기
        val refreshToken = cookieHelper.getCookie(request, JwtConstants.REFRESH_TOKEN_COOKIE)
            ?: throw ApiException(JwtStatus.MISSING_REFRESH_TOKEN)

        // refresh token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw ApiException(JwtStatus.INVALID_REFRESH_TOKEN)
        }

        // userId 추출
        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)

        // 새 토큰 발급
        val newAccessToken = jwtTokenProvider.createAccessToken(userId)
        val newRefreshToken = jwtTokenProvider.createRefreshToken(userId)

        // 쿠키에 새 토큰 설정
        cookieHelper.addCookie(
            response,
            JwtConstants.ACCESS_TOKEN_COOKIE,
            newAccessToken,
            (jwtProperties.accessTokenExpiration / 1000).toInt()
        )
        cookieHelper.addCookie(
            response,
            JwtConstants.REFRESH_TOKEN_COOKIE,
            newRefreshToken,
            (jwtProperties.refreshTokenExpiration / 1000).toInt()
        )
    }
}
