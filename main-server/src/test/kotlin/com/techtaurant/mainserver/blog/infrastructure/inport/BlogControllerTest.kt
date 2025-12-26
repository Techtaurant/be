package com.techtaurant.mainserver.blog.infrastructure.inport

import com.fasterxml.jackson.databind.ObjectMapper
import com.techtaurant.mainserver.blog.entity.Blog
import com.techtaurant.mainserver.blog.infrastructure.out.BlogRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BlogControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var blogRepository: BlogRepository

    private lateinit var testBlogId: UUID
    private lateinit var testBlog: Blog

    @BeforeEach
    fun setUp() {
        testBlog = Blog(
            name = "test-blog",
            displayName = "테스트 블로그",
            iconUrl = "https://example.com/icon.png",
            baseUrl = "https://example.com",
        )
        testBlog = blogRepository.save(testBlog)
        testBlogId = testBlog.id!!
    }

    @Test
    @DisplayName("GET /api/v1/blogs - 모든 블로그 조회 성공")
    @WithMockUser
    fun getAllBlogs_Success() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/blogs")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].id").value(testBlogId.toString()))
            .andExpect(jsonPath("$.data[0].name").value("test-blog"))
            .andExpect(jsonPath("$.data[0].displayName").value("테스트 블로그"))
    }

    @Test
    @DisplayName("GET /api/v1/blogs/{id} - ID로 블로그 조회 성공")
    @WithMockUser
    fun getBlogById_Success() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/blogs/{id}", testBlogId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data.id").value(testBlogId.toString()))
            .andExpect(jsonPath("$.data.name").value("test-blog"))
    }

    @Test
    @DisplayName("GET /api/v1/blogs/search - 이름으로 블로그 검색 성공")
    @WithMockUser
    fun searchBlogByName_Success() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/blogs/search")
                .param("name", "test-blog")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data.name").value("test-blog"))
    }

    @Test
    @DisplayName("GET /api/v1/blogs/search - 블로그 없음 (null 반환)")
    @WithMockUser
    fun searchBlogByName_NotFound() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/blogs/search")
                .param("name", "nonexistent")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data").isEmpty)
    }
}
