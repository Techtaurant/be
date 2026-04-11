package com.techtaurant.mainserver.security.config

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
        val transactionId = request.getHeader(TRANSACTION_ID_HEADER)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

        MDC.put(TRANSACTION_ID_MDC_KEY, transactionId)

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
