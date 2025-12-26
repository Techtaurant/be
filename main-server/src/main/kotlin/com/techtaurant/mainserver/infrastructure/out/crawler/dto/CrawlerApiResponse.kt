package com.techtaurant.mainserver.infrastructure.out.crawler.dto

/**
 * 크롤러 API 공통 응답 형식
 *
 * @param T 응답 데이터 타입
 * @param success 성공 여부
 * @param status HTTP 상태 코드
 * @param message 응답 메시지
 * @param data 응답 데이터
 * @param timestamp 응답 시간
 */
data class CrawlerApiResponse<T>(
    val success: Boolean,
    val status: String,
    val message: String,
    val data: T,
    val timestamp: String,
)
