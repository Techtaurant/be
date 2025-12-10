package com.techtaurant.mainserver.blog.service

import com.techtaurant.mainserver.blog.BlogStatus
import com.techtaurant.mainserver.blog.dto.BlogCreateRequest
import com.techtaurant.mainserver.blog.dto.BlogResponse
import com.techtaurant.mainserver.blog.dto.BlogUpdateRequest
import com.techtaurant.mainserver.blog.entity.Blog
import com.techtaurant.mainserver.blog.infrastructure.out.BlogRepository
import com.techtaurant.mainserver.common.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BlogService(
    private val blogRepository: BlogRepository,
) {

    @Transactional
    fun createBlog(request: BlogCreateRequest): BlogResponse {
        val blog = Blog(
            name = request.name,
            displayName = request.displayName,
            iconUrl = request.iconUrl,
            baseUrl = request.baseUrl,
        )
        val savedBlog = blogRepository.save(blog)
        return savedBlog.toResponse()
    }

    @Transactional
    fun updateBlog(id: UUID, request: BlogUpdateRequest): BlogResponse {
        val blog = blogRepository.findById(id)
            .orElseThrow { ApiException(BlogStatus.BLOG_NOT_FOUND, "Blog not found with id: $id") }

        request.displayName?.let { blog.displayName = it }
        request.iconUrl?.let { blog.iconUrl = it }
        request.baseUrl?.let { blog.baseUrl = it }

        return blog.toResponse()
    }

    /**
     * 블로그를 소프트 삭제합니다.
     *
     * @param id 삭제할 블로그의 UUID
     * @throws ApiException 블로그를 찾을 수 없는 경우 (BlogStatus.BLOG_NOT_FOUND)
     *
     * 주의: Soft Delete가 적용되어 실제 DB에서 삭제되지 않고 deleted_at만 업데이트됨
     */
    @Transactional
    fun deleteBlog(id: UUID) {
        val blog = blogRepository.findById(id)
            .orElseThrow { ApiException(BlogStatus.BLOG_NOT_FOUND, "Blog not found with id: $id") }
        // @SQLDelete 어노테이션에 의해 실제로는 UPDATE blogs SET deleted_at = CURRENT_TIMESTAMP 실행
        blogRepository.delete(blog)
    }

    @Transactional(readOnly = true)
    fun getBlogById(id: UUID): BlogResponse {
        val blog = blogRepository.findById(id)
            .orElseThrow { ApiException(BlogStatus.BLOG_NOT_FOUND, "Blog not found with id: $id") }
        return blog.toResponse()
    }

    @Transactional(readOnly = true)
    fun getAllBlogs(): List<BlogResponse> {
        return blogRepository.findByDeletedAtIsNull()
            .map { it.toResponse() }
    }

    /**
     * Full Text Search를 사용한 블로그 검색
     * Trigram 유사도 기반으로 name과 displayName을 통합 검색
     *
     * @param keyword 검색 키워드
     * @return 유사도 순으로 정렬된 블로그 목록 (최대 10개)
     */
    @Transactional(readOnly = true)
    fun searchBlogByName(keyword: String): List<BlogResponse> {
        return blogRepository.searchByTrigramSimilarity(keyword, 10)
            .map { it.toResponse() }
    }

    private fun Blog.toResponse() = BlogResponse(
        id = id!!,
        name = name,
        displayName = displayName,
        iconUrl = iconUrl,
        baseUrl = baseUrl,
    )
}
