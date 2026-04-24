package com.techtaurant.mainserver.notification.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.notification.application.NotificationReadService
import com.techtaurant.mainserver.notification.dto.MarkNotificationsReadRequest
import com.techtaurant.mainserver.notification.dto.NotificationListItemResponse
import com.techtaurant.mainserver.security.SecurityConstants
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.API_PREFIX}/notifications")
@Validated
class NotificationController(
    private val notificationReadService: NotificationReadService,
) : NotificationControllerDocs {
    @GetMapping
    override fun getMyNotifications(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ApiResponse<CursorPageResponse<NotificationListItemResponse>> {
        return ApiResponse.ok(notificationReadService.getMyNotifications(userId, cursor, size))
    }

    @PatchMapping("/read")
    override fun markNotificationsRead(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: MarkNotificationsReadRequest,
    ): ApiResponse<Unit> {
        notificationReadService.markNotificationsRead(userId, request.notificationIds)
        return ApiResponse.ok(Unit)
    }
}
