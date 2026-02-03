package com.techtaurant.mainserver.common.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 커서 기반 페이지네이션 응답
 *
 * @param T 컨텐츠 타입
 * @property content 페이지 데이터
 * @property nextCursor 다음 페이지 커서 (마지막 페이지면 null)
 * @property hasNext 다음 페이지 존재 여부
 * @property size 현재 페이지 데이터 개수
 */
@Schema(description = "커서 기반 페이지네이션 응답")
data class CursorPageResponse<T>(
    @field:Schema(description = "페이지 데이터 목록")
    val content: List<T>,

    @field:Schema(description = "다음 페이지 커서 (마지막 페이지면 null)", nullable = true)
    val nextCursor: String?,

    @field:Schema(description = "다음 페이지 존재 여부")
    val hasNext: Boolean,

    @field:Schema(description = "현재 페이지 데이터 개수")
    val size: Int,
)
