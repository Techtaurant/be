package com.techtaurant.mainserver.security.jwt

import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)

        if (token != null) {
            try {
                // 토큰 검증
                if (jwtTokenProvider.validateToken(token)) {
                    val userId = jwtTokenProvider.getUserIdFromToken(token)
                    val user = userRepository.findById(userId).orElse(null)

                    if (user != null) {
                        val authorities = listOf(SimpleGrantedAuthority(user.role.key))
                        val authentication = UsernamePasswordAuthenticationToken(user, null, authorities)
                        SecurityContextHolder.getContext().authentication = authentication
                    }
                }
            } catch (e: ExpiredJwtException) {
                request.setAttribute("jwtStatus", JwtStatus.TOKEN_EXPIRED)
            } catch (e: MalformedJwtException) {
                request.setAttribute("jwtStatus", JwtStatus.MALFORMED_TOKEN)
            } catch (e: UnsupportedJwtException) {
                request.setAttribute("jwtStatus", JwtStatus.UNSUPPORTED_TOKEN)
            } catch (e: IllegalArgumentException) {
                request.setAttribute("jwtStatus", JwtStatus.INVALID_TOKEN)
            } catch (e: Exception) {
                request.setAttribute("jwtStatus", JwtStatus.UNKNOWN_ERROR)
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
