package com.techtaurant.mainserver.blog.infrastructure.inport

import com.techtaurant.mainserver.blog.dto.BlogCreateRequest
import com.techtaurant.mainserver.blog.dto.BlogResponse
import com.techtaurant.mainserver.blog.dto.BlogUpdateRequest
import com.techtaurant.mainserver.blog.service.BlogService
import com.techtaurant.mainserver.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody as SpringRequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "블로그 관리 (Admin)", description = "관리자 전용 블로그 관리 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/admin/blogs")
class AdminBlogController(
    private val blogService: BlogService,
) {

    @Operation(
        summary = "블로그 등록",
        description = "새로운 블로그를 등록합니다. (관리자 권한 필요)"
    )
    @SwaggerApiResponse(
        responseCode = "201",
        description = "등록 성공",
        content = [Content(schema = Schema(implementation = BlogResponse::class))]
    )
    @SwaggerApiResponse(responseCode = "400", description = "잘못된 요청")
    @SwaggerApiResponse(responseCode = "401", description = "인증 실패")
    @SwaggerApiResponse(responseCode = "403", description = "권한 없음")
    @PostMapping
    fun createBlog(
        @RequestBody(
            description = "등록할 블로그 정보",
            required = true,
            content = [Content(schema = Schema(implementation = BlogCreateRequest::class))]
        )
        @SpringRequestBody request: BlogCreateRequest
    ): ResponseEntity<ApiResponse<BlogResponse>> {
        val blog = blogService.createBlog(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(blog))
    }

    @Operation(
        summary = "블로그 수정",
        description = "기존 블로그 정보를 수정합니다. (관리자 권한 필요)"
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "수정 성공",
        content = [Content(schema = Schema(implementation = BlogResponse::class))]
    )
    @SwaggerApiResponse(responseCode = "400", description = "잘못된 요청")
    @SwaggerApiResponse(responseCode = "401", description = "인증 실패")
    @SwaggerApiResponse(responseCode = "403", description = "권한 없음")
    @SwaggerApiResponse(
        responseCode = "404",
        description = "블로그를 찾을 수 없음 (customStatusCode: 3001)"
    )
    @PutMapping("/{id}")
    fun updateBlog(
        @Parameter(description = "블로그 ID", required = true)
        @PathVariable id: UUID,
        @RequestBody(
            description = "수정할 블로그 정보",
            required = true,
            content = [Content(schema = Schema(implementation = BlogUpdateRequest::class))]
        )
        @SpringRequestBody request: BlogUpdateRequest,
    ): ResponseEntity<ApiResponse<BlogResponse>> {
        val blog = blogService.updateBlog(id, request)
        return ResponseEntity.ok(ApiResponse.ok(blog))
    }

    @Operation(
        summary = "블로그 삭제",
        description = "블로그를 삭제합니다. (관리자 권한 필요)"
    )
    @SwaggerApiResponse(responseCode = "200", description = "삭제 성공")
    @SwaggerApiResponse(responseCode = "401", description = "인증 실패")
    @SwaggerApiResponse(responseCode = "403", description = "권한 없음")
    @SwaggerApiResponse(
        responseCode = "404",
        description = "블로그를 찾을 수 없음 (customStatusCode: 3001)"
    )
    @DeleteMapping("/{id}")
    fun deleteBlog(
        @Parameter(description = "블로그 ID", required = true)
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<Unit>> {
        blogService.deleteBlog(id)
        return ResponseEntity.ok(ApiResponse.ok())
    }
}
