package com.techtaurant.mainserver.security.jwt

import com.techtaurant.mainserver.user.enums.UserRole
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

    /**
     * AccessToken을 생성합니다.
     *
     * JWT에 userId와 role을 포함하여 Stateless 인증을 구현합니다.
     *
     * @param userId 사용자 ID
     * @param role 사용자 권한
     * @return 생성된 AccessToken
     */
    fun createAccessToken(userId: UUID, role: UserRole): String {
        val now = Date()
        val expiryDate = Date(now.time + JwtConstants.ACCESS_TOKEN_EXPIRED_TIME)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role.key)  // 권한 정보 포함
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    /**
     * RefreshToken을 생성합니다.
     *
     * RefreshToken은 userId만 포함합니다.
     * 토큰 갱신 시 DB에서 최신 권한을 조회하여 새 AccessToken에 반영합니다.
     *
     * @param userId 사용자 ID
     * @return 생성된 RefreshToken
     */
    fun createRefreshToken(userId: UUID): String {
        return createToken(userId, JwtConstants.REFRESH_TOKEN_EXPIRED_TIME)
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
     * userId와 role을 포함한 Claims 객체를 반환하여
     * 별도의 DB 조회 없이 인증/인가를 완료합니다.
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
            role = claims["role"] as String
        )
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
