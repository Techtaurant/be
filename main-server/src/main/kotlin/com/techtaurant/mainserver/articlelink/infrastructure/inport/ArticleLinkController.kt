package com.techtaurant.mainserver.articlelink.infrastructure.inport

import com.techtaurant.mainserver.articlelink.dto.ValidationRequest
import com.techtaurant.mainserver.articlelink.service.ArticleLinkService
import com.techtaurant.mainserver.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody as SpringRequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "아티클 링크", description = "아티클 링크 검증 및 저장 API")
@RestController
@RequestMapping("/api/v1/article-links")
class ArticleLinkController(
    private val articleLinkService: ArticleLinkService,
) {

    @Operation(
        summary = "아티클 링크 검증 및 저장",
        description = "아티클 링크의 유효성을 검증하고 저장합니다."
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "검증 및 저장 성공",
        content = [Content(schema = Schema(implementation = Boolean::class))]
    )
    @SwaggerApiResponse(responseCode = "400", description = "잘못된 요청")
    @PostMapping
    fun validateAndSave(
        @RequestBody(
            description = "검증할 아티클 링크 정보",
            required = true,
            content = [Content(schema = Schema(implementation = ValidationRequest::class))]
        )
        @SpringRequestBody request: ValidationRequest
    ): ResponseEntity<ApiResponse<Boolean>> {
        val result = articleLinkService.validateAndSave(request)
        return ResponseEntity.ok(ApiResponse.ok(result))
    }
}
