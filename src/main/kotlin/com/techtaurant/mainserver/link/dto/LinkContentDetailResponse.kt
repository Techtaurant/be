package com.techtaurant.mainserver.link.dto

import com.techtaurant.mainserver.link.entity.Link
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.Date
import java.util.UUID

@Schema(description = "링크 정적 콘텐츠 상세 응답")
data class LinkContentDetailResponse(
    @field:Schema(description = "링크 ID")
    val id: UUID,
    @field:Schema(description = "제목")
    val title: String,
    @field:Schema(description = "원문 URL")
    val url: String,
    @field:Schema(description = "짧은 설명")
    val summary: String,
    @field:Schema(description = "출처 회사 사용자 ID")
    val sourceCompanyUserId: UUID,
    @field:Schema(description = "발행일", nullable = true)
    val publishedAt: Instant?,
    @field:ArraySchema(schema = Schema(description = "링크 태그명", example = "engineering"))
    val tags: List<String>,
    @field:Schema(description = "생성일")
    val createdAt: Date,
    @field:Schema(description = "최종 수정일")
    val updatedAt: Date,
) {
    companion object {
        fun from(link: Link): LinkContentDetailResponse =
            LinkContentDetailResponse(
                id = link.id ?: throw IllegalStateException("링크 ID가 없습니다"),
                title = link.title,
                url = link.url,
                summary = link.summary,
                sourceCompanyUserId = link.sourceCompanyUser.id ?: throw IllegalStateException("회사 사용자 ID가 없습니다"),
                publishedAt = link.publishedAt,
                tags =
                    link.tags
                        .map { it.name }
                        .sorted(),
                createdAt = link.createdAt,
                updatedAt = link.updatedAt,
            )
    }
}
