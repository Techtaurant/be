package com.techtaurant.mainserver.common.config

import com.techtaurant.mainserver.infrastructure.out.crawler.CrawlerApiClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * 외부 API 클라이언트 설정
 *
 * Infrastructure Out 레이어의 API 클라이언트들에 대한 RestClient 빈 생성
 */
@Configuration
class ApiClientConfig(
    @Value("\${crawler.api.base-url}")
    private val crawlerApiBaseUrl: String,
) {

    /**
     * 크롤러 API 클라이언트 빈 생성
     *
     * @return CrawlerApiClient 인스턴스
     */
    @Bean
    fun crawlerApiClient(): CrawlerApiClient {
        val restClient = RestClient.builder()
            .baseUrl(crawlerApiBaseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()

        return CrawlerApiClient(restClient)
    }
}
