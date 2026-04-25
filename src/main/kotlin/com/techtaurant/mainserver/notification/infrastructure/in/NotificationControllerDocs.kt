package com.techtaurant.mainserver.notification.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.swagger.ApiCommonBadRequestUnknownAndAuthenticationRequired
import com.techtaurant.mainserver.notification.dto.MarkNotificationsReadApiResponse
import com.techtaurant.mainserver.notification.dto.MarkNotificationsReadRequest
import com.techtaurant.mainserver.notification.dto.MarkNotificationsReadResponse
import com.techtaurant.mainserver.notification.dto.NotificationListItemResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "알림", description = "알림 API")
interface NotificationControllerDocs {
    @Operation(
        summary = "내 알림 목록 조회",
        description = "현재 로그인한 사용자의 알림 목록을 최신순으로 조회합니다. 커서 기반 페이지네이션을 지원합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
        useReturnTypeSchema = true,
    )
    @ApiCommonBadRequestUnknownAndAuthenticationRequired
    fun getMyNotifications(
        userId: UUID,
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)") @Min(1) @Max(100) size: Int,
    ): ApiResponse<CursorPageResponse<NotificationListItemResponse>>

    @Operation(
        summary = "알림 다건 읽음 처리",
        description = "현재 로그인한 사용자의 알림 여러 건을 한 번에 읽음 처리합니다. 본인 알림이 아닌 ID는 무시됩니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "읽음 처리 성공",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = MarkNotificationsReadApiResponse::class))],
    )
    @ApiCommonBadRequestUnknownAndAuthenticationRequired
    fun markNotificationsRead(
        userId: UUID,
        @Valid request: MarkNotificationsReadRequest,
    ): ApiResponse<MarkNotificationsReadResponse>
}
