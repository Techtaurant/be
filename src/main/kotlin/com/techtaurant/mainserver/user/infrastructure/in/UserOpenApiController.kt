package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.user.application.UserReadService
import com.techtaurant.mainserver.user.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "User", description = "사용자 Open API")
@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/users")
@Validated
class UserOpenApiController(
    private val userReadService: UserReadService,
) {
    companion object {
        private const val SEARCH_NAME_VALIDATION_ERROR_EXAMPLE =
            "{\"status\": 400," +
                " \"data\": {\"errors\":" +
                " {\"searchByName.name\":" +
                " \"공백일 수 없습니다\"}}," +
                " \"message\": \"Wrong Request\"}"
    }

    @Operation(summary = "사용자 검색", description = "사용자 이름으로 검색합니다")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "검색 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "name이 공백인 경우",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Validation 에러",
                                value = SEARCH_NAME_VALIDATION_ERROR_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/search")
    fun searchByName(
        @Parameter(description = "검색할 사용자 이름 (1자 이상)")
        @RequestParam
        @NotBlank
        name: String,
    ): ApiResponse<List<UserResponse>> {
        return ApiResponse.ok(userReadService.searchByName(name))
    }
}
