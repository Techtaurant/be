package com.techtaurant.mainserver.security.jwt

import java.util.UUID

/**
 * JWT 토큰에서 추출한 Claims 정보
 *
 * @property userId 사용자 ID
 * @property role 사용자 권한 (예: "ROLE_USER", "ROLE_ADMIN")
 * @property isPermanent 영구 토큰 여부
 */
data class JwtClaims(
    val userId: UUID,
    val role: String,
    val isPermanent: Boolean,
)
