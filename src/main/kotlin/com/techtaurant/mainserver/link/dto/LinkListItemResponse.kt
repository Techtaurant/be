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
    @field:Schema(description = "대표 출처 회사 사용자 ID", nullable = true)
    val sourceCompanyUserId: UUID?,
    @field:ArraySchema(schema = Schema(description = "출처 회사 사용자 ID", example = "019e34c1-7b2f-70a2-bb16-e89d5b16b311"))
    val sourceCompanyUserIds: List<UUID>,
    @field:Schema(description = "발행일", nullable = true)
    val publishedAt: Instant?,
    @field:Schema(description = "저장 여부")
    val isSaved: Boolean,
    @field:Schema(description = "읽음 여부")
    val isRead: Boolean,
    @field:ArraySchema(schema = Schema(description = "링크 태그명", example = "engineering"))
    val tags: List<String>,
) {
    companion object {
        fun from(
            link: Link,
            sourceCompanyUserIds: List<UUID>,
            isSaved: Boolean,
            isRead: Boolean,
            preferredSourceCompanyUserId: UUID? = null,
        ): LinkListItemResponse {
            return LinkListItemResponse(
                id = link.id ?: throw IllegalStateException("링크 ID가 없습니다"),
                title = link.title,
                url = link.url,
                summary = link.summary,
                sourceCompanyUserId = resolvePrimarySourceCompanyUserId(sourceCompanyUserIds, preferredSourceCompanyUserId),
                sourceCompanyUserIds = sourceCompanyUserIds,
                publishedAt = link.publishedAt,
                isSaved = isSaved,
                isRead = isRead,
                tags =
                    link.tags
                        .map { it.name }
                        .sorted(),
            )
        }

        private fun resolvePrimarySourceCompanyUserId(
            sourceCompanyUserIds: List<UUID>,
            preferredSourceCompanyUserId: UUID?,
        ): UUID? =
            preferredSourceCompanyUserId?.takeIf { it in sourceCompanyUserIds }
                ?: sourceCompanyUserIds.minByOrNull { it.toString() }
    }
}
