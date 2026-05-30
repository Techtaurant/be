package com.techtaurant.mainserver.security.jwt

import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.security.helper.JwtExceptionMapper
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserTokenRepository
import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT 기반 인증 필터
 *
 * AccessToken에서 userId와 role을 추출하여 Stateless 인증을 수행합니다.
 * 일반 AccessToken은 JWT만으로 인증하고, 영구 토큰은 DB 등록 여부와 현재 사용자 권한을 추가로 확인합니다.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userTokenRepository: UserTokenRepository,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)

        if (token != null) {
            try {
                // JWT에서 userId와 role을 추출합니다.
                val claims = jwtTokenProvider.validateAndGetClaims(token)

                if (!canAuthenticateByTokenPolicy(claims, token)) {
                    SecurityContextHolder.clearContext()
                    request.setAttribute(SecurityConstants.ERROR_ATTRIBUTE, JwtStatus.INVALID_TOKEN)
                    filterChain.doFilter(request, response)
                    return
                }

                // 권한 생성
                val authorities = listOf(SimpleGrantedAuthority(claims.role))

                // SecurityContext에 인증 정보 설정 (principal: userId)
                val authentication =
                    UsernamePasswordAuthenticationToken(
                        claims.userId, // principal: userId만 저장
                        null,
                        authorities,
                    )
                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: ExpiredJwtException) {
                request.setAttribute(SecurityConstants.ERROR_ATTRIBUTE, JwtStatus.ACCESS_TOKEN_EXPIRED)
            } catch (e: Exception) {
                request.setAttribute(SecurityConstants.ERROR_ATTRIBUTE, JwtExceptionMapper.mapToJwtStatus(e = e))
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        // 1. Authorization 헤더에서 토큰 확인
        val bearerToken = request.getHeader("Authorization")
        if (bearerToken != null && bearerToken.startsWith(JwtConstants.BEARER_PREFIX)) {
            return bearerToken.substring(JwtConstants.BEARER_PREFIX.length)
        }

        // 2. 쿠키에서 토큰 확인
        return request.cookies?.find { it.name == JwtConstants.ACCESS_TOKEN_COOKIE }?.value
    }

    private fun canAuthenticateByTokenPolicy(
        claims: JwtClaims,
        token: String,
    ): Boolean {
        if (!claims.isPermanent) {
            return true
        }

        return isRegisteredPermanentTokenWithCurrentUserRole(claims, token)
    }

    private fun isRegisteredPermanentTokenWithCurrentUserRole(
        claims: JwtClaims,
        token: String,
    ): Boolean {
        val claimedRole = UserRole.fromKey(claims.role) ?: return false

        return userTokenRepository.existsByUserIdAndTokenHashAndUserRole(
            claims.userId,
            jwtTokenProvider.hashToken(token),
            claimedRole,
        )
    }
}
