package com.techtaurant.mainserver.blog.infrastructure.inport

import com.fasterxml.jackson.databind.ObjectMapper
import com.techtaurant.mainserver.blog.dto.BlogCreateRequest
import com.techtaurant.mainserver.blog.dto.BlogUpdateRequest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminBlogControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var blogRepository: BlogRepository

    private lateinit var testBlogId: UUID
    private lateinit var createRequest: BlogCreateRequest
    private lateinit var updateRequest: BlogUpdateRequest

    @BeforeEach
    fun setUp() {
        val savedBlog = blogRepository.save(
            com.techtaurant.mainserver.blog.entity.Blog(
                name = "existing-blog",
                displayName = "기존 블로그",
                iconUrl = "https://example.com/icon.png",
                baseUrl = "https://example.com",
            ),
        )
        testBlogId = savedBlog.id!!

        createRequest = BlogCreateRequest(
            name = "new-blog",
            displayName = "새로운 블로그",
            iconUrl = "https://example.com/new-icon.png",
            baseUrl = "https://newblog.com",
        )
        updateRequest = BlogUpdateRequest(
            displayName = "수정된 블로그",
            iconUrl = "https://example.com/updated-icon.png",
            baseUrl = "https://example.com/updated",
        )
    }

    @Test
    @DisplayName("POST /admin/blogs - ADMIN 권한으로 블로그 생성 성공")
    @WithMockUser(roles = ["ADMIN"])
    fun createBlog_Success_WithAdmin() {
        // When & Then
        mockMvc.perform(
            post("/admin/blogs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data.name").value("new-blog"))
            .andExpect(jsonPath("$.data.displayName").value("새로운 블로그"))
    }

    @Test
    @DisplayName("PUT /admin/blogs/{id} - ADMIN 권한으로 블로그 수정 성공")
    @WithMockUser(roles = ["ADMIN"])
    fun updateBlog_Success_WithAdmin() {
        // When & Then
        mockMvc.perform(
            put("/admin/blogs/{id}", testBlogId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data.id").value(testBlogId.toString()))
            .andExpect(jsonPath("$.data.displayName").value("수정된 블로그"))
    }

    @Test
    @DisplayName("DELETE /admin/blogs/{id} - ADMIN 권한으로 블로그 삭제 성공")
    @WithMockUser(roles = ["ADMIN"])
    fun deleteBlog_Success_WithAdmin() {
        // When & Then
        mockMvc.perform(
            delete("/admin/blogs/{id}", testBlogId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("OK"))
    }
}
