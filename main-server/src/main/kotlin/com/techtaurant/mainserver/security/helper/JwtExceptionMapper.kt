package com.techtaurant.mainserver.security.helper

import com.techtaurant.mainserver.security.jwt.JwtStatus
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import org.slf4j.LoggerFactory

object JwtExceptionMapper {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun mapToJwtStatus(e: Exception): JwtStatus {
        return when (e) {
            is MalformedJwtException -> JwtStatus.MALFORMED_TOKEN
            is UnsupportedJwtException -> JwtStatus.UNSUPPORTED_TOKEN
            is IllegalArgumentException -> JwtStatus.INVALID_TOKEN
            else -> {
                log.error("Unknown JWT error: {}", e.message, e)
                JwtStatus.UNKNOWN_ERROR
            }
        }
    }
}
