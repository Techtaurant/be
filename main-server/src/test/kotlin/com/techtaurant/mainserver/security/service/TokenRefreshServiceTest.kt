package com.techtaurant.mainserver.security.service

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.security.cache.TokenCacheManager
import com.techtaurant.mainserver.security.config.CacheType
import com.techtaurant.mainserver.security.helper.CookieHelper
import com.techtaurant.mainserver.security.jwt.JwtConstants
import com.techtaurant.mainserver.security.jwt.JwtProperties
import com.techtaurant.mainserver.security.jwt.JwtStatus
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import io.jsonwebtoken.ExpiredJwtException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class TokenRefreshServiceTest {

    private lateinit var tokenRefreshService: TokenRefreshService
    private val cookieHelper: CookieHelper = mockk()
    private val jwtTokenProvider: JwtTokenProvider = mockk()
    private val jwtProperties: JwtProperties = JwtProperties("test-secret", 1000, 2000)
    private val tokenCacheManager: TokenCacheManager = mockk()

    @BeforeEach
    fun setUp() {
        tokenRefreshService = TokenRefreshService(cookieHelper, jwtTokenProvider, jwtProperties, tokenCacheManager)
    }

    @Test
    @DisplayName("토큰 리프레시 성공")
    fun `token refresh success`() {
        // given
        val userId = UUID.randomUUID()
        val refreshTokenValue = "valid-refresh-token"
        val newAccessToken = "new-access-token"
        val newRefreshToken = "new-refresh-token"
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>(relaxed = true)

        every { cookieHelper.getCookie(request, JwtConstants.REFRESH_TOKEN_COOKIE) } returns refreshTokenValue
        every { cookieHelper.addCookie(any(), any(), any(), any()) } returns Unit
        every { tokenCacheManager.get(CacheType.REFRESH_TOKEN, refreshTokenValue, String::class.java) } returns userId.toString()
        every { jwtTokenProvider.validateAndGetUserId(refreshTokenValue) } returns userId
        every { jwtTokenProvider.createAccessToken(userId) } returns newAccessToken
        every { jwtTokenProvider.createRefreshToken(userId) } returns newRefreshToken
        every { tokenCacheManager.delete(CacheType.REFRESH_TOKEN, refreshTokenValue) } returns Unit
        every { tokenCacheManager.save(CacheType.REFRESH_TOKEN, newRefreshToken, userId.toString()) } returns Unit

        // when
        tokenRefreshService.execute(request, response)

        // then
        verify { cookieHelper.addCookie(response, JwtConstants.ACCESS_TOKEN_COOKIE, newAccessToken, (jwtProperties.accessTokenExpiration / 1000).toInt()) }
        verify { cookieHelper.addCookie(response, JwtConstants.REFRESH_TOKEN_COOKIE, newRefreshToken, (jwtProperties.refreshTokenExpiration / 1000).toInt()) }
    }

    @Test
    @DisplayName("캐시에 존재하지 않는 리프레시 토큰으로 요청 시 예외 발생")
    fun `refresh with non-existent token in cache`() {
        // given
        val refreshTokenValue = "non-existent-token"
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()

        every { cookieHelper.getCookie(request, JwtConstants.REFRESH_TOKEN_COOKIE) } returns refreshTokenValue
        every { tokenCacheManager.get(CacheType.REFRESH_TOKEN, refreshTokenValue, String::class.java) } returns null

        // when & then
        val exception = assertThrows<ApiException> {
            tokenRefreshService.execute(request, response)
        }
        assertEquals(JwtStatus.INVALID_REFRESH_TOKEN, exception.status)
    }

    @Test
    @DisplayName("만료된 리프레시 토큰으로 요청 시 예외 발생")
    fun `refresh with expired token`() {
        // given
        val userId = UUID.randomUUID()
        val refreshTokenValue = "expired-refresh-token"
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()

        every { cookieHelper.getCookie(request, JwtConstants.REFRESH_TOKEN_COOKIE) } returns refreshTokenValue
        every { tokenCacheManager.get(CacheType.REFRESH_TOKEN, refreshTokenValue, String::class.java) } returns userId.toString()
        every { jwtTokenProvider.validateAndGetUserId(refreshTokenValue) } throws ExpiredJwtException(null, null, "expired")

        // when & then
        val exception = assertThrows<ApiException> {
            tokenRefreshService.execute(request, response)
        }
        assertEquals(JwtStatus.TOKEN_EXPIRED, exception.status)
    }

    @Test
    @DisplayName("캐시의 userId와 토큰의 userId가 다를 경우 예외 발생")
    fun `mismatched userId between cache and token`() {
        // given
        val cachedUserId = UUID.randomUUID()
        val tokenUserId = UUID.randomUUID()
        val refreshTokenValue = "mismatched-token"
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()

        every { cookieHelper.getCookie(request, JwtConstants.REFRESH_TOKEN_COOKIE) } returns refreshTokenValue
        every { tokenCacheManager.get(CacheType.REFRESH_TOKEN, refreshTokenValue, String::class.java) } returns cachedUserId.toString()
        every { jwtTokenProvider.validateAndGetUserId(refreshTokenValue) } returns tokenUserId

        // when & then
        val exception = assertThrows<ApiException> {
            tokenRefreshService.execute(request, response)
        }
        assertEquals(JwtStatus.INVALID_REFRESH_TOKEN, exception.status)
    }
}
