package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.link.dto.CreateLinkCrawlBatchRequest
import com.techtaurant.mainserver.link.dto.LinkBatchRunResponse
import com.techtaurant.mainserver.link.dto.LinkCrawlBatchListItemResponse
import com.techtaurant.mainserver.link.dto.LinkCrawlBatchResponse
import com.techtaurant.mainserver.link.dto.UpdateLinkCrawlBatchRequest
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.security.jwt.JwtStatus
import com.techtaurant.mainserver.user.enums.UserStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "관리자 링크 배치", description = "관리자 전용 링크 수집 배치 API")
interface AdminLinkCrawlBatchControllerDocs {
    @Operation(
        summary = "회사 링크 수집 배치 등록",
        description = """
        관리자가 회사별 SSR 링크 수집 배치를 등록합니다. 등록 전 설정된 시작 페이지를 1회 요청해 크롤링 가능 여부를 검증합니다.

        selector를 찾는 방법:
        1. 먼저 브라우저의 검사(Inspect)로 보이는 DOM이 아니라, View Page Source 또는 curl로 받은 원본 HTML에 요소가 실제로 있는지 확인합니다.
        2. Copy XPath, Copy selector 결과를 그대로 쓰지 말고 더 짧고 의미 있는 selector로 줄입니다.
        3. class가 `_13swo3b4`, `css-1vn47db`처럼 생성형이면 우선순위를 낮추고 `href`, `data-*`, `time`, `h1`, `article`, `main` 같은 의미 기반 selector를 먼저 찾습니다.
        4. 목록 페이지에서는 `itemSelector`로 카드 전체를 먼저 잡고, 그 내부에서 `articleLinkSelector`, `titleSelector`, `summarySelector`를 상대 selector로 찾습니다.
        5. DevTools Console에서 `document.querySelectorAll('selector').length`로 개수를 확인하고, 기대한 요소만 잡히는지 검증합니다.
        6. 카드 자체가 a 태그라면 `articleLinkSelector`에 `:self`를 넣어 카드 본인을 링크 요소로 사용할 수 있습니다.

        Toss 예시:
        - baseUrl: `https://toss.tech`
        - pageUriTemplate: `/?page={page}`
        - itemSelector: `a[data-log-name='item'][data-log-section_type='new'][data-log-item_type='article'][href^='/article/']`
        - articleLinkSelector: `:self`
        - titleSelector: `div._13swo3b7`
        - summarySelector: `div._13swo3b8`
        - publishedAtSelectors: `div.o6bzluc`

        추천 원칙:
        - 절대 XPath(`/html/body/...`)는 피합니다.
        - `body > div...`처럼 페이지 전체 경로 selector는 피합니다.
        - selector는 짧고 재사용 가능해야 합니다.
        - CSR이 아니라 SSR만 대상으로 합니다.
        - 발행일은 ISO 날짜/시간 또는 `2023년 6월 20일` 형식의 텍스트를 지원합니다.
        """,
    )
    @SwaggerApiResponse(responseCode = "201", description = "배치 등록 성공")
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED", "ACCESS_DENIED"]),
            ApiErrorCodeResponse(UserStatus::class, ["COMPANY_NOT_FOUND"]),
            ApiErrorCodeResponse(
                LinkStatus::class,
                ["INVALID_LINK_CRAWL_BATCH_CRON_EXPRESSION", "LINK_CRAWL_BATCH_PUBLISHED_AT_REQUIRED", "LINK_CRAWL_BATCH_NOT_CRAWLABLE"],
            ),
            ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST", "UNKNOWN_EXCEPTION"]),
        ],
    )
    fun createBatch(
        @Parameter(description = "회사 사용자 ID") companyUserId: UUID,
        @RequestBody(
            description = "링크 수집 배치 등록 요청",
            required = true,
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CreateLinkCrawlBatchRequest::class),
                    examples = [
                        ExampleObject(
                            name = "Toss Tech",
                            value = """
                            {
                              "name": "토스 테크 링크 수집",
                              "baseUrl": "https://toss.tech",
                              "pageUriTemplate": "/?page={page}",
                              "itemSelector": "a[data-log-name='item'][data-log-section_type='new'][data-log-item_type='article'][href^='/article/']",
                              "articleLinkSelector": ":self",
                              "titleSelector": "div._13swo3b7",
                              "summarySelector": "div._13swo3b8",
                              "publishedAtSelectors": [
                                "div.o6bzluc"
                              ],
                              "tagNames": [
                                "toss-tech"
                              ],
                              "cronExpression": "0 0 * * * *",
                              "startPage": 2,
                              "active": true
                            }
                            """,
                        ),
                    ],
                ),
            ],
        )
        @Valid request: CreateLinkCrawlBatchRequest,
    ): ApiResponse<LinkCrawlBatchResponse>

    @Operation(summary = "회사 링크 수집 배치 목록 조회", description = "관리자가 특정 회사의 링크 수집 배치 목록을 조회합니다")
    fun getBatches(
        @Parameter(description = "회사 사용자 ID") companyUserId: UUID,
    ): ApiResponse<List<LinkCrawlBatchListItemResponse>>

    @Operation(summary = "링크 수집 배치 수정", description = "관리자가 링크 수집 배치 설정을 부분 수정합니다. 수정된 설정으로 크롤링 가능 여부를 검증합니다")
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED", "ACCESS_DENIED"]),
            ApiErrorCodeResponse(
                LinkStatus::class,
                [
                    "LINK_CRAWL_BATCH_NOT_FOUND",
                    "INVALID_LINK_CRAWL_BATCH_CRON_EXPRESSION",
                    "LINK_CRAWL_BATCH_PUBLISHED_AT_REQUIRED",
                    "LINK_CRAWL_BATCH_NOT_CRAWLABLE",
                ],
            ),
            ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST", "UNKNOWN_EXCEPTION"]),
        ],
    )
    fun updateBatch(
        @Parameter(description = "배치 ID") batchId: UUID,
        @Valid request: UpdateLinkCrawlBatchRequest,
    ): ApiResponse<LinkCrawlBatchResponse>

    @Operation(
        summary = "링크 수집 배치 수동 실행",
        description = "관리자가 해당 배치를 즉시 실행하여 SSR 목록 페이지에서 링크를 수집합니다. 발행일을 수집할 수 없는 항목이 있으면 배치가 실패합니다.",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED", "ACCESS_DENIED"]),
            ApiErrorCodeResponse(LinkStatus::class, ["LINK_CRAWL_BATCH_NOT_FOUND", "LINK_CRAWL_BATCH_PUBLISHED_AT_REQUIRED"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun runBatch(
        @Parameter(description = "배치 ID") batchId: UUID,
    ): ApiResponse<LinkBatchRunResponse>
}
