package com.techtaurant.mainserver.post.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 게시물 생성 요청 DTO
 *
 * @property title 게시물 제목 (필수, 최대 200자)
 * @property content 게시물 본문 (필수, 제한 없음)
 * @property categoryPath 카테고리 경로 (선택, 예: "java/spring/deepdive")
 * @property tags 태그 목록 (선택, 여러 개 가능)
 */
@Schema(description = "게시물 생성 요청")
data class CreatePostRequest(

    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 200, message = "제목은 최대 200자까지 가능합니다")
    @field:Schema(description = "게시물 제목", example = "Spring Boot 시작하기", required = true, maxLength = 200)
    val title: String,

    @field:NotBlank(message = "본문은 필수입니다")
    @field:Schema(description = "게시물 본문", example = "Spring Boot를 사용하면...", required = true)
    val content: String,

    @field:Schema(description = "카테고리 경로 (슬래시로 구분, 최대 5단계)", example = "java/spring/deepdive")
    val categoryPath: String? = null,

    @field:Schema(description = "태그 목록", example = "[\"spring\", \"backend\", \"tutorial\"]")
    val tags: List<String>? = null,
)
