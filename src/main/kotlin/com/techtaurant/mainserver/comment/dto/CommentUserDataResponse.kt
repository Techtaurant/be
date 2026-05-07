package com.techtaurant.mainserver.comment.dto

import com.techtaurant.mainserver.common.enums.LikeStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "댓글 사용자 데이터 응답")
data class CommentUserDataResponse(
    @field:Schema(description = "댓글 ID")
    val commentId: UUID,
    @field:Schema(description = "현재 사용자의 좋아요 상태")
    val likeStatus: LikeStatus,
    @field:Schema(description = "현재 사용자가 댓글 작성자를 차단했는지 여부")
    val isBannedAuthor: Boolean,
)
