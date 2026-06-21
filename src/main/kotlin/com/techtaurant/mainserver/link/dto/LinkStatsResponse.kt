package com.techtaurant.mainserver.link.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "링크 통계 합계 응답")
data class LinkStatsResponse(
    @field:Schema(description = "링크 ID")
    val linkId: UUID,
    @field:Schema(description = "조회수 합계")
    val viewCount: Long,
    @field:Schema(description = "좋아요수 합계")
    val likeCount: Long,
    @field:Schema(description = "저장수 합계")
    val saveCount: Long,
)
