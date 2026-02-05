package com.techtaurant.mainserver.security.service

import com.techtaurant.mainserver.security.cache.TokenCacheManager
import com.techtaurant.mainserver.security.helper.CookieHelper
import com.techtaurant.mainserver.security.jwt.JwtConstants
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 로그아웃 서비스
 *
 * 캐시에서 토큰을 무효화하고 인증 쿠키를 삭제합니다.
 * 멱등성을 보장하여 중복 호출 시에도 안전합니다.
 */
@Service
class LogoutService(
    private val cookieHelper: CookieHelper,
    private val jwtTokenProvider: JwtTokenProvider,
    private val tokenCacheManager: TokenCacheManager
) {

    fun logout(request: HttpServletRequest, response: HttpServletResponse) {
        // 쿠키에서 토큰 추출
        val accessToken = request.cookies?.find { it.name == JwtConstants.ACCESS_TOKEN_COOKIE }?.value
        val refreshToken = request.cookies?.find { it.name == JwtConstants.REFRESH_TOKEN_COOKIE }?.value

        // 캐시에서 토큰 무효화
        invalidateTokens(accessToken, refreshToken)

        // 쿠키 삭제
        cookieHelper.deleteAllAuthCookies(response)
    }

    /**
     * 캐시에서 토큰을 무효화합니다.
     *
     * REFRESH_TOKEN만 캐싱하므로 userId로 RefreshToken을 삭제합니다.
     * ACCESS_TOKEN은 캐싱하지 않으므로 별도 삭제가 불필요합니다.
     *
     * @param accessToken AccessToken 값 (nullable, userId 추출용)
     * @param refreshToken RefreshToken 값 (nullable, userId 추출용)
     */
    private fun invalidateTokens(accessToken: String?, refreshToken: String?) {
        // userId 추출 (둘 중 하나라도 있으면 가능)
        val userId = extractUserId(accessToken, refreshToken) ?: return

        // REFRESH_TOKEN 캐시에서 삭제 (userId를 키로 사용)
        tokenCacheManager.deleteRefreshToken(userId.toString())
    }

    /**
     * 토큰에서 userId를 추출합니다.
     *
     * 검증에 실패해도 예외를 던지지 않고 null을 반환합니다.
     * (이미 만료된 토큰으로 로그아웃하는 경우 허용)
     *
     * @return userId 또는 null
     */
    private fun extractUserId(accessToken: String?, refreshToken: String?): UUID? {
        return try {
            // accessToken 우선 시도
            if (accessToken != null) {
                jwtTokenProvider.validateAndGetUserId(accessToken)
            } else if (refreshToken != null) {
                jwtTokenProvider.validateAndGetUserId(refreshToken)
            } else {
                null
            }
        } catch (e: Exception) {
            // 토큰이 만료되었거나 유효하지 않아도 로그아웃은 성공 처리 (멱등성)
            null
        }
    }
}
