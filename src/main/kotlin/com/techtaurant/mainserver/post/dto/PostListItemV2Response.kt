package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.enums.PostStatusEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Date
import java.util.UUID

@Schema(description = "게시물 목록 아이템 v2 - 사용자별 필드 제외")
data class PostListItemV2Response(
    @field:Schema(description = "게시물 ID")
    val id: UUID,
    @field:Schema(description = "게시물 제목")
    val title: String,
    @field:Schema(description = "게시물 내용 일부 (최대 2000자)")
    val content: String,
    @field:Schema(description = "작성자 ID")
    val authorId: UUID,
    @field:Schema(description = "작성자 이름")
    val authorName: String,
    @field:Schema(description = "작성자 프로필 이미지 URL")
    val authorProfileImageUrl: String,
    @field:Schema(description = "게시물 썸네일 이미지 URL")
    val thumbnailUrl: String,
    @field:Schema(description = "게시물 카테고리")
    val category: CategoryResponse?,
    @field:Schema(description = "태그 목록")
    val tags: List<PostListTagResponse>,
    @field:Schema(description = "조회수")
    val viewCount: Long,
    @field:Schema(description = "좋아요수")
    val likeCount: Long,
    @field:Schema(description = "댓글수")
    val commentCount: Long,
    @field:Schema(description = "게시물 상태")
    val status: PostStatusEnum,
    @field:Schema(description = "작성일")
    val createdAt: Date,
    @field:Schema(description = "최종 수정일")
    val updatedAt: Date,
)
