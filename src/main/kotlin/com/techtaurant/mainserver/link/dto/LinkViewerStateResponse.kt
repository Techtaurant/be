package com.techtaurant.mainserver.link.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "링크 로그인 사용자 상태 응답")
data class LinkViewerStateResponse(
    @field:Schema(description = "링크 ID")
    val linkId: UUID,
    @field:Schema(description = "로그인한 사용자의 저장 여부")
    val isSaved: Boolean,
    @field:Schema(description = "로그인한 사용자의 읽음 여부")
    val isRead: Boolean,
)
