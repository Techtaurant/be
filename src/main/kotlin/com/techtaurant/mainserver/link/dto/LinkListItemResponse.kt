package com.techtaurant.mainserver.link.dto

import com.techtaurant.mainserver.link.entity.Link
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "링크 목록 아이템 응답")
data class LinkListItemResponse(
    @field:Schema(description = "링크 ID")
    val id: UUID,
    @field:Schema(description = "제목")
    val title: String,
    @field:Schema(description = "원문 URL")
    val url: String,
    @field:Schema(description = "짧은 설명")
    val summary: String,
    @field:Schema(description = "최초 출처 사용자 ID", nullable = true)
    val sourceCompanyUserId: UUID?,
    @field:Schema(description = "링크 생성일")
    val createdAt: Instant,
    @field:Schema(description = "저장 여부")
    val isSaved: Boolean,
    @field:Schema(description = "읽음 여부")
    val isRead: Boolean,
    @field:Schema(description = "조회수")
    val viewCount: Long,
    @field:Schema(description = "좋아요수")
    val likeCount: Long,
    @field:ArraySchema(schema = Schema(description = "링크 태그명", example = "engineering"))
    val tags: List<String>,
) {
    companion object {
        fun from(
            link: Link,
            sourceCompanyUserId: UUID?,
            isSaved: Boolean,
            isRead: Boolean,
        ): LinkListItemResponse {
            return LinkListItemResponse(
                id = link.id ?: throw IllegalStateException("링크 ID가 없습니다"),
                title = link.title,
                url = link.url,
                summary = link.summary,
                sourceCompanyUserId = sourceCompanyUserId,
                createdAt = link.createdAt,
                isSaved = isSaved,
                isRead = isRead,
                viewCount = link.viewCount,
                likeCount = link.likeCount,
                tags =
                    link.tags
                        .map { it.name }
                        .sorted(),
            )
        }
    }
}
