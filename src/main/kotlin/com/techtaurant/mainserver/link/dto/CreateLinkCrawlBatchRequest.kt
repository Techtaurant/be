package com.techtaurant.mainserver.link.dto

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "링크 수집 배치 등록 요청")
data class CreateLinkCrawlBatchRequest(
    @field:NotBlank(message = "배치 이름은 필수입니다")
    @field:Size(max = 100, message = "배치 이름은 최대 100자까지 가능합니다")
    @field:Schema(description = "배치 이름", example = "토스 엔지니어링 링크 수집")
    val name: String,
    @field:NotBlank(message = "baseUrl은 필수입니다")
    @field:Schema(description = "크롤링 기준 base URL", example = "https://toss.tech")
    val baseUrl: String,
    @field:NotBlank(message = "pageUriTemplate은 필수입니다")
    @field:Schema(description = "페이지 URI 템플릿. {page} 치환자를 사용합니다.", example = "/category/engineering?page={page}")
    val pageUriTemplate: String,
    @field:NotBlank(message = "itemSelector는 필수입니다")
    @field:Schema(description = "목록 페이지에서 각 게시물 카드 전체를 선택하는 selector", example = ".article-card")
    val itemSelector: String,
    @field:NotBlank(message = "articleLinkSelector는 필수입니다")
    @field:Schema(description = "itemSelector로 찾은 카드 내부에서 링크를 선택하는 selector. 카드 자체가 a 태그면 :self 사용 가능", example = "a.article-link")
    val articleLinkSelector: String,
    @field:NotBlank(message = "titleSelector는 필수입니다")
    @field:Schema(description = "카드 내부 제목 selector", example = ".title")
    val titleSelector: String,
    @field:Schema(description = "카드 내부 요약 selector", example = ".summary", nullable = true)
    val summarySelector: String? = null,
    @field:ArraySchema(schema = Schema(description = "작성자 selector", example = ".author"))
    val authorSelectors: List<String> = emptyList(),
    @field:ArraySchema(schema = Schema(description = "발행일 selector", example = "time"))
    val publishedAtSelectors: List<String> = emptyList(),
    @field:ArraySchema(schema = Schema(description = "수집된 링크에 부여할 태그명", example = "engineering"))
    val tagNames: List<String> = emptyList(),
    @field:NotBlank(message = "cronExpression은 필수입니다")
    @field:Schema(description = "cron 표현식", example = "0 0 * * * *")
    val cronExpression: String,
    @field:Min(value = 1, message = "startPage는 1 이상이어야 합니다")
    @field:Schema(description = "시작 페이지", example = "1")
    val startPage: Int = 1,
    @field:Min(value = 1, message = "endPage는 1 이상이어야 합니다")
    @field:Schema(description = "종료 페이지", example = "3")
    val endPage: Int = 1,
    @field:Schema(description = "배치 활성화 여부", example = "true")
    val active: Boolean = true,
)
