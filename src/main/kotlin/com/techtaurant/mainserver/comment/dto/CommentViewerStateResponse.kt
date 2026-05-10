package com.techtaurant.mainserver.comment.dto

import com.techtaurant.mainserver.common.enums.LikeStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "댓글 로그인 사용자 상태 응답")
data class CommentViewerStateResponse(
    @field:Schema(description = "댓글 ID")
    val commentId: UUID,
    @field:Schema(description = "로그인한 사용자의 좋아요 상태")
    val likeStatus: LikeStatus,
    @field:Schema(description = "로그인한 사용자가 차단한 작성자의 댓글인지 여부")
    val isBanned: Boolean,
)
