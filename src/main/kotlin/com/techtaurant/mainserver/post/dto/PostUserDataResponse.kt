package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.common.enums.LikeStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "게시물 사용자 데이터 응답")
data class PostUserDataResponse(
    @field:Schema(description = "게시물 ID")
    val postId: UUID,
    @field:Schema(description = "현재 사용자의 좋아요 상태")
    val likeStatus: LikeStatus,
    @field:Schema(description = "현재 사용자가 읽음 표시한 게시물인지 여부")
    val isRead: Boolean,
)
