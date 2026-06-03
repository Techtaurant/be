package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.entity.Post
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

/**
 * 임시 저장 게시물 목록 아이템 응답 DTO
 *
 * DRAFT 상태의 게시물 목록 조회 시 사용됩니다.
 * 조회수, 좋아요수, 읽음 여부 등의 통계 정보는 포함하지 않습니다.
 *
 * @property id 게시물 ID
 * @property title 게시물 제목
 * @property content 게시물 본문 미리보기 (최대 100자)
 * @property createdAt 생성일시
 * @property updatedAt 최종 수정일시
 */
@Schema(description = "임시 저장 게시물 목록 아이템")
data class DraftListItemResponse(
    @field:Schema(description = "게시물 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: UUID,
    @field:Schema(description = "게시물 제목", example = "새 게시물")
    val title: String,
    @field:Schema(description = "게시물 본문 미리보기 (최대 100자)", example = "Empty")
    val contentPreview: String,
    @field:Schema(description = "생성일시")
    val createdAt: Instant,
    @field:Schema(description = "최종 수정일시")
    val updatedAt: Instant,
) {
    companion object {
        fun from(post: Post): DraftListItemResponse {
            val contentPreview =
                if (post.content.length > 100) {
                    post.content.substring(0, 100) + "..."
                } else {
                    post.content
                }

            return DraftListItemResponse(
                id = post.id!!,
                title = post.title,
                contentPreview = contentPreview,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
            )
        }
    }
}
