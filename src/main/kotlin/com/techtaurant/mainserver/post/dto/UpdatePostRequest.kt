package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.enums.PostStatusEnum
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

/**
 * 게시물 수정 요청 DTO
 *
 * 모든 필드가 선택적이며, 포함된 필드만 업데이트됩니다.
 * 상태 전환 시 DRAFT를 제외한 상태는 제목과 본문이 필수입니다.
 *
 * @property title 게시물 제목 (선택, 최대 200자)
 * @property content 게시물 본문 (선택)
 * @property categoryPath 카테고리 경로 (선택)
 * @property tags 태그 목록 (선택)
 * @property status 게시물 상태 (선택, DRAFT/PUBLISHED/PRIVATE)
 */
@Schema(description = "게시물 수정 요청")
data class UpdatePostRequest(
    @field:Size(max = 200, message = "제목은 최대 200자까지 가능합니다")
    @field:Schema(description = "게시물 제목", example = "Spring Boot 시작하기", maxLength = 200)
    val title: String? = null,
    @field:Schema(description = "게시물 본문", example = "Spring Boot를 사용하면...")
    val content: String? = null,
    @field:Schema(description = "카테고리 경로 (슬래시로 구분, 최대 5단계)", example = "java/spring/deepdive")
    val categoryPath: String? = null,
    @field:Schema(description = "태그 목록", example = "[\"spring\", \"backend\", \"tutorial\"]")
    val tags: List<String>? = null,
    @field:Schema(description = "게시물 상태 (DRAFT/PUBLISHED/PRIVATE)", example = "PUBLISHED")
    val status: PostStatusEnum? = null,
    @field:Schema(description = "content에 삽입된 S3 objectKey 목록 (현재 content에 존재하는 objectKey 전체)", example = "[\"posts/uuid/photo.jpg\"]")
    val objectKeys: List<String>? = null,
)
