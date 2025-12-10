package com.techtaurant.mainserver.security.jwt

import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.security.helper.JwtExceptionMapper
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
 * DB 조회나 캐시 없이 JWT만으로 인증/인가를 완료하여 최고의 성능을 제공합니다.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)

        if (token != null) {
            try {
                // JWT에서 userId + role 추출 (DB 조회 없이 완료)
                val claims = jwtTokenProvider.validateAndGetClaims(token)

                // 권한 생성
                val authorities = listOf(SimpleGrantedAuthority(claims.role))

                // SecurityContext에 인증 정보 설정 (principal: userId)
                val authentication = UsernamePasswordAuthenticationToken(
                    claims.userId,  // principal: userId만 저장
                    null,
                    authorities
                )
                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: Exception) {
                request.setAttribute(SecurityConstants.ERROR_ATTRIBUTE, JwtExceptionMapper.mapToJwtStatus(e = e))
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        // 1. Authorization 헤더에서 토큰 확인
        val bearerToken = request.getHeader("Authorization")
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }

        // 2. 쿠키에서 토큰 확인
        return request.cookies?.find { it.name == JwtConstants.ACCESS_TOKEN_COOKIE }?.value
    }
}
