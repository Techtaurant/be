package com.techtaurant.mainserver.blog.infrastructure.inport

import com.techtaurant.mainserver.blog.dto.BlogResponse
import com.techtaurant.mainserver.blog.service.BlogService
import com.techtaurant.mainserver.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "블로그", description = "블로그 조회 API")
@RestController
@RequestMapping("/api/v1/blogs")
class BlogController(
    private val blogService: BlogService,
) {

    @Operation(
        summary = "전체 블로그 목록 조회",
        description = "등록된 모든 블로그 목록을 조회합니다."
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = [Content(schema = Schema(implementation = BlogResponse::class))]
    )
    @GetMapping
    fun getAllBlogs(): ResponseEntity<ApiResponse<List<BlogResponse>>> {
        val blogs = blogService.getAllBlogs()
        return ResponseEntity.ok(ApiResponse.ok(blogs))
    }

    @Operation(
        summary = "블로그 단건 조회",
        description = "ID로 특정 블로그를 조회합니다."
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = [Content(schema = Schema(implementation = BlogResponse::class))]
    )
    @SwaggerApiResponse(
        responseCode = "404",
        description = "블로그를 찾을 수 없음 (customStatusCode: 3001)"
    )
    @GetMapping("/{id}")
    fun getBlogById(
        @Parameter(description = "블로그 ID", required = true)
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<BlogResponse>> {
        val blog = blogService.getBlogById(id)
        return ResponseEntity.ok(ApiResponse.ok(blog))
    }

    @Operation(
        summary = "블로그 검색 (Full Text Search)",
        description = "Trigram 유사도 기반 Full Text Search로 블로그를 검색합니다. name과 displayName을 통합 검색하며, 부분 문자열 매칭 및 오타를 허용합니다. 유사도 순으로 정렬된 결과를 최대 10개 반환합니다."
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "검색 성공 (결과가 없을 경우 빈 배열 반환)",
        content = [Content(schema = Schema(implementation = BlogResponse::class))]
    )
    @GetMapping("/search")
    fun searchBlogByName(
        @Parameter(description = "검색 키워드 (2글자 이상 권장)", required = true)
        @RequestParam keyword: String
    ): ResponseEntity<ApiResponse<List<BlogResponse>>> {
        val blogs = blogService.searchBlogByName(keyword)
        return ResponseEntity.ok(ApiResponse.ok(blogs))
    }
}
