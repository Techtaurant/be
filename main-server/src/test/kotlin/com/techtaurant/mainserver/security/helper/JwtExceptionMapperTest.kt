package com.techtaurant.mainserver.security.helper

import com.techtaurant.mainserver.security.jwt.JwtStatus
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class JwtExceptionMapperTest {

    @Test
    @DisplayName("ExpiredJwtException은 TOKEN_EXPIRED로 매핑")
    fun `map ExpiredJwtException to TOKEN_EXPIRED`() {
        val status = JwtExceptionMapper.mapToJwtStatus(ExpiredJwtException(null, null, "expired"))
        assertEquals(JwtStatus.TOKEN_EXPIRED, status)
    }

    @Test
    @DisplayName("MalformedJwtException은 MALFORMED_TOKEN으로 매핑")
    fun `map MalformedJwtException to MALFORMED_TOKEN`() {
        val status = JwtExceptionMapper.mapToJwtStatus(MalformedJwtException("malformed"))
        assertEquals(JwtStatus.MALFORMED_TOKEN, status)
    }

    @Test
    @DisplayName("UnsupportedJwtException은 UNSUPPORTED_TOKEN으로 매핑")
    fun `map UnsupportedJwtException to UNSUPPORTED_TOKEN`() {
        val status = JwtExceptionMapper.mapToJwtStatus(UnsupportedJwtException("unsupported"))
        assertEquals(JwtStatus.UNSUPPORTED_TOKEN, status)
    }

    @Test
    @DisplayName("IllegalArgumentException은 INVALID_TOKEN으로 매핑")
    fun `map IllegalArgumentException to INVALID_TOKEN`() {
        val status = JwtExceptionMapper.mapToJwtStatus(IllegalArgumentException("invalid"))
        assertEquals(JwtStatus.INVALID_TOKEN, status)
    }

    @Test
    @DisplayName("알 수 없는 예외는 UNKNOWN_ERROR로 매핑")
    fun `map unknown exception to UNKNOWN_ERROR`() {
        val status = JwtExceptionMapper.mapToJwtStatus(RuntimeException("unknown"))
        assertEquals(JwtStatus.UNKNOWN_ERROR, status)
    }
}
