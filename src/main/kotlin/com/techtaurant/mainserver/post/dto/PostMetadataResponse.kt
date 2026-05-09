package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.enums.PostStatusEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "게시물 공개 동적 메타데이터 응답")
data class PostMetadataResponse(
    @field:Schema(description = "게시물 ID")
    val postId: UUID,
    @field:Schema(description = "조회수")
    val viewCount: Long,
    @field:Schema(description = "좋아요수")
    val likeCount: Long,
    @field:Schema(description = "댓글수")
    val commentCount: Long,
    @field:Schema(description = "게시물 상태")
    val status: PostStatusEnum,
    @field:Schema(description = "게시물 썸네일 이미지 URL")
    val thumbnailUrl: String,
    @field:Schema(description = "작성자 프로필 이미지 URL")
    val authorProfileImageUrl: String,
    @field:Schema(description = "게시물 본문 첨부파일 presigned URL 목록")
    val attachmentPresignedUrls: List<PostDetailAttachmentPresignedUrlResponse>,
)
