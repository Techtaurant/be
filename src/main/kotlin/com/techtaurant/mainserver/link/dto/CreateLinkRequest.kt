package com.techtaurant.mainserver.link.dto

import com.techtaurant.mainserver.post.entity.TaggedContent
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

@Schema(description = "링크 등록 요청")
data class CreateLinkRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 200, message = "제목은 최대 200자까지 가능합니다")
    @field:Schema(description = "링크 제목", example = "Spring Boot 시작하기", maxLength = 200)
    val title: String,
    @field:NotBlank(message = "URL은 필수입니다")
    @field:Size(max = 2048, message = "URL은 최대 2048자까지 가능합니다")
    @field:Schema(description = "원문 URL", example = "https://tech.example.com/articles/spring-boot", maxLength = 2048)
    val url: String,
    @field:NotBlank(message = "요약은 필수입니다")
    @field:Schema(description = "짧은 설명", example = "Spring Boot를 빠르게 시작하는 방법을 소개합니다.")
    val summary: String,
    @field:Size(max = TaggedContent.MAX_TAG_COUNT, message = "태그는 최대 10개까지 설정할 수 있습니다")
    @field:ArraySchema(maxItems = TaggedContent.MAX_TAG_COUNT, schema = Schema(description = "태그명", example = "spring"))
    val tags: List<String>? = null,
    @field:Schema(description = "링크 생성일시 (ISO-8601 UTC, 입력하지 않으면 서버 시간이 사용됨)", example = "2026-06-01T00:00:00Z")
    val createdAt: Instant? = null,
)
