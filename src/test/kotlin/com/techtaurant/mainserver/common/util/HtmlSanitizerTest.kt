package com.techtaurant.mainserver.common.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class HtmlSanitizerTest {
    @Nested
    @DisplayName("sanitizeContent")
    inner class SanitizeContent {
        @Test
        @DisplayName("허용된 텍스트 서식 태그가 유지된다")
        fun allowedTextFormattingTagsPreserved() {
            // Given
            val html = "<p><strong>굵게</strong> <em>기울임</em> <del>취소선</del> <code>코드</code></p>"

            // When
            val result = HtmlSanitizer.sanitizeContent(html)

            // Then
            assertThat(result).isEqualTo("<p><strong>굵게</strong> <em>기울임</em> <del>취소선</del> <code>코드</code></p>")
        }

        @Test
        @DisplayName("허용된 구조 태그가 유지된다")
        fun allowedStructuralTagsPreserved() {
            // Given
            val html = "<h1>제목</h1><blockquote>인용</blockquote><ul><li>항목</li></ul><hr>"

            // When
            val result = HtmlSanitizer.sanitizeContent(html)

            // Then
            assertThat(result).contains("<h1>제목</h1>")
            assertThat(result).contains("<blockquote>인용</blockquote>")
            assertThat(result).contains("<ul><li>항목</li></ul>")
            assertThat(result).contains("<hr>")
        }

        @Test
        @DisplayName("a 태그의 href, title 속성이 유지된다")
        fun anchorTagAttributesPreserved() {
            // Given
            val html = """<a href="https://github.com" title="GitHub">링크</a>"""

            // When
            val result = HtmlSanitizer.sanitizeContent(html)

            // Then
            assertThat(result).contains("""href="https://github.com"""")
            assertThat(result).contains("""title="GitHub"""")
            assertThat(result).contains("링크</a>")
        }

        @Test
        @DisplayName("img 태그의 src, alt, width, height 속성이 유지된다")
        fun imgTagAttributesPreserved() {
            // Given
            val html = """<img src="https://example.com/img.png" alt="이미지" width="100" height="50">"""

            // When
            val result = HtmlSanitizer.sanitizeContent(html)

            // Then
            assertThat(result).contains("""src="https://example.com/img.png"""")
            assertThat(result).contains("""alt="이미지"""")
            assertThat(result).contains("""width="100"""")
            assertThat(result).contains("""height="50"""")
        }

        @Test
        @DisplayName("table 관련 태그가 유지된다")
        fun tableTagsPreserved() {
            // Given
            val html = "<table><thead><tr><th>헤더</th></tr></thead><tbody><tr><td>데이터</td></tr></tbody></table>"

            // When
            val result = HtmlSanitizer.sanitizeContent(html)

            // Then
            assertThat(result).contains("<table>")
            assertThat(result).contains("<thead>")
            assertThat(result).contains("<th>헤더</th>")
            assertThat(result).contains("<td>데이터</td>")
        }

        @Test
        @DisplayName("details, summary 태그가 유지된다")
        fun detailsSummaryTagsPreserved() {
            // Given
            val html = """<details open><summary>펼치기</summary><p>내용</p></details>"""

            // When
            val result = HtmlSanitizer.sanitizeContent(html)

            // Then
            assertThat(result).contains("<details open>")
            assertThat(result).contains("<summary>펼치기</summary>")
        }

        @Test
        @DisplayName("script 태그가 제거된다")
        fun scriptTagRemoved() {
            // Given
            val html = "<p>안전한 텍스트</p><script>alert('xss')</script>"

            // When
            val result = HtmlSanitizer.sanitizeContent(html)

            // Then
            assertThat(result).doesNotContain("<script>")
            assertThat(result).doesNotContain("alert")
            assertThat(result).contains("<p>안전한 텍스트</p>")
        }

        @Test
        @DisplayName("이벤트 핸들러 속성이 제거된다")
        fun eventHandlerAttributesRemoved() {
            // Given
            val html = """<div onclick="alert('xss')" onmouseover="hack()">내용</div>"""

            // When
            val result = HtmlSanitizer.sanitizeContent(html)

            // Then
            assertThat(result).doesNotContain("onclick")
            assertThat(result).doesNotContain("onmouseover")
            assertThat(result).contains("내용")
        }

        @Test
        @DisplayName("style 태그가 제거된다")
        fun styleTagRemoved() {
            // Given
            val html = "<style>body{display:none}</style><p>내용</p>"

            // When
            val result = HtmlSanitizer.sanitizeContent(html)

            // Then
            assertThat(result).doesNotContain("<style>")
            assertThat(result).doesNotContain("display:none")
            assertThat(result).contains("<p>내용</p>")
        }

        @Test
        @DisplayName("iframe, object, embed 태그가 제거된다")
        fun dangerousEmbedTagsRemoved() {
            // Given
            val html = """<iframe src="https://evil.com"></iframe><object data="x"></object><embed src="y">"""

            // When
            val result = HtmlSanitizer.sanitizeContent(html)

            // Then
            assertThat(result).doesNotContain("<iframe")
            assertThat(result).doesNotContain("<object")
            assertThat(result).doesNotContain("<embed")
        }

        @Test
        @DisplayName("허용되지 않는 속성이 제거된다")
        fun disallowedAttributesRemoved() {
            // Given
            val html = """<p class="danger" id="secret" style="color:red">내용</p>"""

            // When
            val result = HtmlSanitizer.sanitizeContent(html)

            // Then
            assertThat(result).doesNotContain("class=")
            assertThat(result).doesNotContain("id=")
            assertThat(result).doesNotContain("style=")
            assertThat(result).contains("<p>내용</p>")
        }

        @Test
        @DisplayName("javascript: 프로토콜이 포함된 href가 제거된다")
        fun javascriptProtocolInHrefRemoved() {
            // Given
            val html = """<a href="javascript:alert('xss')">클릭</a>"""

            // When
            val result = HtmlSanitizer.sanitizeContent(html)

            // Then
            assertThat(result).doesNotContain("javascript:")
        }
    }

    @Nested
    @DisplayName("sanitizeTitle")
    inner class SanitizeTitle {
        @Test
        @DisplayName("모든 HTML 태그가 제거된다")
        fun allHtmlTagsRemoved() {
            // Given
            val html = "<h1>제목</h1><script>alert('xss')</script>"

            // When
            val result = HtmlSanitizer.sanitizeTitle(html)

            // Then
            assertThat(result).isEqualTo("제목")
        }

        @Test
        @DisplayName("일반 텍스트는 그대로 유지된다")
        fun plainTextPreserved() {
            // Given
            val text = "Spring Boot 시작하기"

            // When
            val result = HtmlSanitizer.sanitizeTitle(text)

            // Then
            assertThat(result).isEqualTo("Spring Boot 시작하기")
        }
    }
}
