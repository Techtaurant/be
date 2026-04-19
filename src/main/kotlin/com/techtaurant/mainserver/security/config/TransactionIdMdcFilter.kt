package com.techtaurant.mainserver.security.config

import io.opentelemetry.api.trace.Span
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class TransactionIdMdcFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // 클라이언트가 전달한 값을 우선 쓰고, 없으면 서버에서 새 transaction id를 만든다.
        val transactionId = request.getHeader(TRANSACTION_ID_HEADER)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

        response.setHeader(TRANSACTION_ID_HEADER, transactionId)
        MDC.put(TRANSACTION_ID_MDC_KEY, transactionId)
        val currentSpan = Span.current()

        // 로그와 trace가 같은 키로 묶이도록 MDC 값과 현재 span attribute를 함께 맞춘다.
        if (currentSpan.spanContext.isValid) {
            currentSpan.setAttribute("app.transaction_id", transactionId)
            currentSpan.setAttribute("http.request.header.x_transaction_id", transactionId)
        }

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(TRANSACTION_ID_MDC_KEY)
        }
    }

    companion object {
        const val TRANSACTION_ID_HEADER = "X-Transaction-Id"
        const val TRANSACTION_ID_MDC_KEY = "transactionId"
    }
}
