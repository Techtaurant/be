package com.techtaurant.mainserver.security.jwt

import com.techtaurant.mainserver.user.enums.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.Date
import java.util.HexFormat
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
) {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    fun createAccessToken(
        userId: UUID,
        role: UserRole,
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtProperties.accessTokenExpireMs)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role.key) // 권한 정보 포함
            .claim("permanent", false)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun createPermanentAccessToken(
        userId: UUID,
        role: UserRole,
    ): String {
        val now = Date()

        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId.toString())
            .claim("role", role.key)
            .claim("permanent", true)
            .issuedAt(now)
            .signWith(secretKey)
            .compact()
    }

    fun createRefreshToken(userId: UUID): String {
        return createToken(userId, jwtProperties.refreshTokenExpireMs)
    }

    private fun createToken(
        userId: UUID,
        expiration: Long,
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
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

    /**
     * AccessToken을 검증하고 Claims를 추출합니다.
     *
     * userId와 role, 영구 토큰 여부를 포함한 Claims 객체를 반환합니다.
     *
     * @param token AccessToken
     * @return JWT에서 추출한 Claims (userId + role)
     * @throws ExpiredJwtException 토큰이 만료된 경우
     * @throws UnsupportedJwtException 지원하지 않는 토큰 형식인 경우
     * @throws MalformedJwtException 잘못된 형식의 토큰인 경우
     * @throws SecurityException 서명 검증에 실패한 경우
     */
    fun validateAndGetClaims(token: String): JwtClaims {
        val claims = getClaims(token)
        return JwtClaims(
            userId = UUID.fromString(claims.subject),
            role = claims["role"] as String,
            isPermanent = claims["permanent"] as? Boolean ?: false,
        )
    }

    fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return HexFormat.of().formatHex(digest)
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
