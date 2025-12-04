package com.techtaurant.mainserver.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.security.jwt.JwtStatus
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * 인증되지 않은 사용자가 보호된 리소스에 접근할 때 호출되는 핸들러
 * Spring Security 필터 체인에서 인증 실패 시 공통 에러 응답을 반환
 */
@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        // Request Attribute에서 JWT 예외 정보 확인
        val jwtStatus = request.getAttribute("jwtStatus") as? JwtStatus
            ?: JwtStatus.AUTHENTICATION_REQUIRED

        val errorResponse = ApiResponse.error<Any>(jwtStatus)

        response.status = jwtStatus.getHttpStatusCode()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
