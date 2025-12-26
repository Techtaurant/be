package com.techtaurant.mainserver.articlelink.infrastructure.inport

import com.fasterxml.jackson.databind.ObjectMapper
import com.techtaurant.mainserver.articlelink.dto.ValidationRequest
import com.techtaurant.mainserver.articlelink.enums.ArticleLinkType
import com.techtaurant.mainserver.blog.entity.Blog
import com.techtaurant.mainserver.blog.infrastructure.out.BlogRepository
import com.techtaurant.mainserver.infrastructure.out.crawler.CrawlerApiClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleLinkControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var blogRepository: BlogRepository

    @MockBean
    private lateinit var crawlerApiClient: CrawlerApiClient

    private lateinit var testBlogId: UUID
    private lateinit var validationRequest: ValidationRequest

    @BeforeEach
    fun setUp() {
        val savedBlog = blogRepository.save(
            Blog(
                name = "test-blog",
                displayName = "테스트 블로그",
                iconUrl = "https://example.com/icon.png",
                baseUrl = "https://example.com",
            ),
        )
        testBlogId = savedBlog.id!!

        validationRequest = ValidationRequest(
            blogId = testBlogId,
            baseUrl = "https://example.com",
            startPage = 1,
            articlePattern = "/articles/",
            titleSelector = "h1.title",
            type = ArticleLinkType.PAGE_BASED,
        )
    }

    @Test
    @DisplayName("POST /api/v1/article-links/validate-and-save - 링크 검증 및 저장 성공")
    @WithMockUser
    fun validateAndSave_Success() {
        // Given
        whenever(crawlerApiClient.validateCrawlingPage(any())).thenReturn(true)

        // When & Then
        mockMvc.perform(
            post("/api/v1/article-links/validate-and-save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data").value(true))
    }

    @Test
    @DisplayName("POST /api/v1/article-links/validate-and-save - 링크 검증 실패")
    @WithMockUser
    fun validateAndSave_Failure() {
        // Given
        whenever(crawlerApiClient.validateCrawlingPage(any())).thenReturn(false)

        // When & Then
        mockMvc.perform(
            post("/api/v1/article-links/validate-and-save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data").value(false))
    }

    @Test
    @DisplayName("POST /api/v1/article-links/validate-and-save - NEXT_BUTTON_BASED 타입 테스트")
    @WithMockUser
    fun validateAndSave_NextButtonBasedType() {
        // Given
        val nextButtonRequest = validationRequest.copy(type = ArticleLinkType.NEXT_BUTTON_BASED)
        whenever(crawlerApiClient.validateCrawlingPage(any())).thenReturn(true)

        // When & Then
        mockMvc.perform(
            post("/api/v1/article-links/validate-and-save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nextButtonRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data").value(true))
    }
}
