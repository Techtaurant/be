package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.user.application.UserReadService
import com.techtaurant.mainserver.user.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "User", description = "사용자 Open API")
@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/users")
class UserOpenApiController(
    private val userReadService: UserReadService,
) {
    @Operation(summary = "사용자 검색", description = "사용자 이름으로 검색합니다")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "검색 성공",
            ),
        ],
    )
    @GetMapping("/search")
    fun searchByName(
        @RequestParam name: String,
    ): ApiResponse<List<UserResponse>> {
        return ApiResponse.ok(userReadService.searchByName(name))
    }
}
