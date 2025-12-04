package com.techtaurant.mainserver.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
) {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    fun createAccessToken(userId: UUID): String {
        return createToken(userId, jwtProperties.accessTokenExpiration)
    }

    fun createRefreshToken(userId: UUID): String {
        return createToken(userId, jwtProperties.refreshTokenExpiration)
    }

    private fun createToken(userId: UUID, expiration: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims = getClaims(token)
        return UUID.fromString(claims.subject)
    }

    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 토큰을 검증하고 userId를 추출합니다.
     * 한 번의 파싱으로 검증과 추출을 동시에 수행하여 성능을 최적화합니다.
     *
     * @param token JWT 토큰
     * @return 토큰에서 추출한 userId
     * @throws ExpiredJwtException 토큰이 만료된 경우
     * @throws UnsupportedJwtException 지원하지 않는 토큰 형식인 경우
     * @throws MalformedJwtException 잘못된 형식의 토큰인 경우
     * @throws SecurityException 서명 검증에 실패한 경우
     */
    fun validateAndGetUserId(token: String): UUID {
        val claims = getClaims(token)
        return UUID.fromString(claims.subject)
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
