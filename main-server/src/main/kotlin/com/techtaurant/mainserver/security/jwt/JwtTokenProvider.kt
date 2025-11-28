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

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
