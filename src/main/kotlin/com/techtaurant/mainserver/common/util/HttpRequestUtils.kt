package com.techtaurant.mainserver.common.util

import jakarta.servlet.http.HttpServletRequest

/**
 * HTTP 요청 관련 유틸리티
 */
object HttpRequestUtils {
    /**
     * 클라이언트의 IP 주소를 추출합니다.
     * 프록시를 거친 경우 X-Forwarded-For 헤더에서 원본 IP를 추출하고,
     * 그렇지 않으면 request.remoteAddr를 사용합니다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    fun extractIpAddress(request: HttpServletRequest): String? {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrBlank()) {
            xForwardedFor.split(",").firstOrNull()?.trim()
        } else {
            request.remoteAddr
        }
    }
}
