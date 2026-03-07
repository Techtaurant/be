package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.post.dto.CreatePostRequest
import com.techtaurant.mainserver.post.dto.UpdatePostRequest
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
@ActiveProfiles("test")
class PostWriteServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var postWriteService: PostWriteService

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setUpTestData() {
        testUser =
            userRepository.save(
                User(
                    name = "테스트 사용자",
                    email = "test@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "test-id-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/profile.jpg",
                ),
            )
    }

    @Nested
    @DisplayName("게시물 생성 시 HTML sanitization")
    inner class CreatePostSanitization {
        @Test
        @DisplayName("title에서 모든 HTML 태그가 제거된다")
        fun createPost_titleHtmlTagsStripped() {
            // Given
            val request =
                CreatePostRequest(
                    title = "<h1>제목</h1><script>alert('xss')</script>",
                    content = "본문 내용",
                    status = PostStatusEnum.PUBLISHED,
                )

            // When
            val response = postWriteService.createPost(testUser.id!!, request)

            // Then
            assertThat(response.title).isEqualTo("제목")
            assertThat(response.title).doesNotContain("<h1>", "<script>")
        }

        @Test
        @DisplayName("content에서 script 태그가 제거된다")
        fun createPost_contentScriptTagRemoved() {
            // Given
            val request =
                CreatePostRequest(
                    title = "테스트 게시물",
                    content = "<p>안전한 내용</p><script>alert('xss')</script>",
                    status = PostStatusEnum.PUBLISHED,
                )

            // When
            val response = postWriteService.createPost(testUser.id!!, request)

            // Then
            assertThat(response.content).contains("<p>안전한 내용</p>")
            assertThat(response.content).doesNotContain("<script>")
            assertThat(response.content).doesNotContain("alert")
        }

        @Test
        @DisplayName("content에서 GitHub 허용 태그는 유지된다")
        fun createPost_contentAllowedTagsPreserved() {
            // Given
            val request =
                CreatePostRequest(
                    title = "테스트 게시물",
                    content = "<h2>소제목</h2><p><strong>굵게</strong> <em>기울임</em></p><pre><code>코드</code></pre>",
                    status = PostStatusEnum.PUBLISHED,
                )

            // When
            val response = postWriteService.createPost(testUser.id!!, request)

            // Then
            assertThat(response.content).contains("<h2>소제목</h2>")
            assertThat(response.content).contains("<strong>굵게</strong>")
            assertThat(response.content).contains("<em>기울임</em>")
            assertThat(response.content).contains("<pre><code>코드</code></pre>")
        }

        @Test
        @DisplayName("content에서 이벤트 핸들러 속성이 제거된다")
        fun createPost_contentEventHandlersRemoved() {
            // Given
            val request =
                CreatePostRequest(
                    title = "테스트 게시물",
                    content = """<div onclick="alert('xss')">내용</div>""",
                    status = PostStatusEnum.PUBLISHED,
                )

            // When
            val response = postWriteService.createPost(testUser.id!!, request)

            // Then
            assertThat(response.content).doesNotContain("onclick")
            assertThat(response.content).contains("내용")
        }

        @Test
        @DisplayName("content에서 iframe 태그가 제거된다")
        fun createPost_contentIframeRemoved() {
            // Given
            val request =
                CreatePostRequest(
                    title = "테스트 게시물",
                    content = """<p>본문</p><iframe src="https://evil.com"></iframe>""",
                    status = PostStatusEnum.PUBLISHED,
                )

            // When
            val response = postWriteService.createPost(testUser.id!!, request)

            // Then
            assertThat(response.content).contains("<p>본문</p>")
            assertThat(response.content).doesNotContain("<iframe")
        }

        @Test
        @DisplayName("content에서 a 태그의 href 속성이 유지된다")
        fun createPost_contentAnchorHrefPreserved() {
            // Given
            val request =
                CreatePostRequest(
                    title = "테스트 게시물",
                    content = """<a href="https://github.com">GitHub</a>""",
                    status = PostStatusEnum.PUBLISHED,
                )

            // When
            val response = postWriteService.createPost(testUser.id!!, request)

            // Then
            assertThat(response.content).contains("""href="https://github.com"""")
            assertThat(response.content).contains("GitHub</a>")
        }

        @Test
        @DisplayName("content에서 javascript: 프로토콜이 차단된다")
        fun createPost_contentJavascriptProtocolBlocked() {
            // Given
            val request =
                CreatePostRequest(
                    title = "테스트 게시물",
                    content = """<a href="javascript:alert('xss')">클릭</a>""",
                    status = PostStatusEnum.PUBLISHED,
                )

            // When
            val response = postWriteService.createPost(testUser.id!!, request)

            // Then
            assertThat(response.content).doesNotContain("javascript:")
        }
    }

    @Nested
    @DisplayName("게시물 수정 시 HTML sanitization")
    inner class UpdatePostSanitization {
        @Test
        @DisplayName("수정 시 title에서 HTML 태그가 제거된다")
        fun updatePost_titleHtmlTagsStripped() {
            // Given
            val createRequest =
                CreatePostRequest(
                    title = "원본 제목",
                    content = "원본 본문",
                    status = PostStatusEnum.PUBLISHED,
                )
            val created = postWriteService.createPost(testUser.id!!, createRequest)

            val updateRequest =
                UpdatePostRequest(
                    title = "<b>수정된 제목</b><script>hack()</script>",
                )

            // When
            val response = postWriteService.updatePost(created.id, updateRequest, testUser.id!!)

            // Then
            assertThat(response.title).isEqualTo("수정된 제목")
            assertThat(response.title).doesNotContain("<b>", "<script>")
        }

        @Test
        @DisplayName("수정 시 content에서 위험한 태그가 제거되고 안전한 태그는 유지된다")
        fun updatePost_contentSanitized() {
            // Given
            val createRequest =
                CreatePostRequest(
                    title = "원본 제목",
                    content = "원본 본문",
                    status = PostStatusEnum.PUBLISHED,
                )
            val created = postWriteService.createPost(testUser.id!!, createRequest)

            val updateRequest =
                UpdatePostRequest(
                    content = "<p>수정된 본문</p><script>alert('xss')</script><style>body{display:none}</style>",
                )

            // When
            val response = postWriteService.updatePost(created.id, updateRequest, testUser.id!!)

            // Then
            assertThat(response.content).contains("<p>수정된 본문</p>")
            assertThat(response.content).doesNotContain("<script>")
            assertThat(response.content).doesNotContain("<style>")
        }
    }
}
