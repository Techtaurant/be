package com.techtaurant.mainserver.infrastructure.out.crawler

import com.techtaurant.mainserver.infrastructure.out.crawler.dto.CrawlerApiResponse
import com.techtaurant.mainserver.infrastructure.out.crawler.dto.PageBaseLinkValidationRequest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.RestClient

/**
 * 크롤러 API 클라이언트
 *
 * 외부 크롤러 서비스와 통신하는 Infrastructure Out 레이어
 * RestClient를 사용하여 동기 방식으로 API 호출
 */
class CrawlerApiClient(
    private val restClient: RestClient
) {

    /**
     * 크롤링 페이지 검증
     *
     * @param request 검증 요청 정보
     * @return 검증 성공 여부 (에러 발생 시 false 반환)
     */
    fun validateCrawlingPage(request: PageBaseLinkValidationRequest): Boolean {
        return try {
            val response = restClient.post()
                .uri("/api/v1/page-base-link-crawling/validations")
                .body(
                    mapOf(
                        "blog_name" to request.blogName,
                        "base_url" to request.baseUrl,
                        "start_page" to request.startPage,
                        "article_pattern" to request.articlePattern,
                        "title_selector" to request.titleSelector,
                    )
                )
                .retrieve()
                .body(object : ParameterizedTypeReference<CrawlerApiResponse<Boolean>>() {})

            response?.data ?: false
        } catch (e: Exception) {
            // 에러 발생 시 false 반환
            false
        }
    }
}
