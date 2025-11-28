package com.techtaurant.mainserver.security.helper

import com.techtaurant.mainserver.security.jwt.JwtStatus
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException

class JwtExceptionMapper {
    companion object {
        fun mapToJwtStatus(e: Exception): JwtStatus {
            return when (e) {
                is ExpiredJwtException -> JwtStatus.TOKEN_EXPIRED
                is MalformedJwtException -> JwtStatus.MALFORMED_TOKEN
                is UnsupportedJwtException -> JwtStatus.UNSUPPORTED_TOKEN
                is IllegalArgumentException -> JwtStatus.INVALID_TOKEN
                else -> JwtStatus.UNKNOWN_ERROR
            }
        }
    }
}
