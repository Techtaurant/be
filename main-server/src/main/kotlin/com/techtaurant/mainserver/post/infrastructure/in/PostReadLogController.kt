package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.post.application.PostReadLogService
import com.techtaurant.mainserver.post.dto.RecordPostReadRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Post", description = "게시물 API")
@RestController
@RequestMapping("/api/posts")
@Validated
class PostReadLogController(
    private val postReadLogService: PostReadLogService,
) {
    @PostMapping("/{postId}/read-logs")
    @Operation(summary = "게시물 읽음 상태 변경", description = "게시물에 대한 읽음 상태를 변경합니다. isRead=true: 읽음 표시, isRead=false: 안읽음 표시. 인증된 사용자만 호출 가능합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "읽음 상태 변경 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증되지 않은 사용자",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = """게시물 또는 사용자를 찾을 수 없음
                    - 게시물 미존재: status=3001, message="게시물을 찾을 수 없습니다"
                    - 사용자 미존재: status=1002, message="사용자를 찾을 수 없습니다"""",
            ),
        ],
    )
    fun toggleReadStatus(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable postId: UUID,
        @Valid @RequestBody request: RecordPostReadRequest,
    ): ApiResponse<Unit> {
        postReadLogService.toggleReadStatus(
            postId = postId,
            userId = userId,
            isRead = request.isRead,
        )

        return ApiResponse.ok(Unit)
    }
}
